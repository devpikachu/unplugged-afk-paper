#!/usr/bin/env bash
#
# Publishes a release to the artifacts server. Run it with no arguments for usage.

set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
project="unplugged-afk"
jar_modules=(paper velocity)
changelog="CHANGELOG.md"
changelog_script="${repo_root}/scripts/changelog.sh"
signature_suffix=".sigstore.json"

artifacts_api="${ARTIFACTS_API:-https://artifacts-admin.detpikachu.dev}"

response_file=""

# SemVer 2.0.0, minus build metadata. Keep in step with the same pattern in changelog.sh.
semver_identifier='(0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)'
semver_pattern="^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-${semver_identifier}(\.${semver_identifier})*)?\$"

die() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat >&2 <<'EOF'
usage:
    scripts/publish-artifacts.sh <version> [--dry-run]
        Stage the version's shipped JARs, a source tarball and its own section of CHANGELOG.md, sign each one with
        cosign, then publish every file and its signature to the artifacts server under /<project>/<version>/. The
        release workflow calls this once the build has produced the JARs.

        --dry-run stages locally and stops. It neither signs nor uploads: keyless signing is a network operation
        against Fulcio and Rekor, so there is no offline half of it to rehearse.

        Nothing else is uploaded. The server derives each file's media type from its name, publishes sha256, sha512,
        sha1 and md5 sidecars itself, and purges its own CDN, so a checksum file or a CDN credential here would be a
        second opinion on something it already owns.

        Re-running is safe. A file whose bytes are already stored answers 200 and changes nothing, so a run that
        failed halfway can simply be repeated; a token may never overwrite, so a file that differs from what is
        published is refused rather than silently replaced.

environment:
    ARTIFACTS_TOKEN         publishing token, scoped to /<project> and granted directory creation. Required.
    ARTIFACTS_API           base URL of the administrative host, default https://artifacts-admin.detpikachu.dev.
                            The publishing API answers on that hostname only, never on the public one.
    ARTIFACTS_SOURCE_REF    ref to archive, default the v<version> tag, HEAD when that tag does not resolve
EOF
    exit 2
}

cleanup() {
    if [[ -n "$response_file" ]]; then
        rm -f -- "$response_file"
    fi
}

# Per module rather than repo-wide: common/build/libs holds a build intermediate, and a */build/libs glob would publish
# it beside the two shipping JARs.
stage_jars() {
    local version="$1" stage="$2" module jar found

    for module in "${jar_modules[@]}"; do
        found=0
        for jar in "${repo_root}/${module}/build/libs/"*"-${version}.jar"; do
            [[ -f "$jar" ]] || continue
            cp -- "$jar" "$stage/"
            found=$((found + 1))
        done
        ((found > 0)) || die "no ${module} JAR for ${version} in ${module}/build/libs, build it first"
    done
}

source_ref() {
    local version="$1"

    if [[ -n "${ARTIFACTS_SOURCE_REF:-}" ]]; then
        printf '%s\n' "$ARTIFACTS_SOURCE_REF"
    elif git -C "$repo_root" rev-parse --quiet --verify "refs/tags/v${version}" >/dev/null 2>&1; then
        printf '%s\n' "refs/tags/v${version}"
    else
        printf '%s\n' HEAD
    fi
}

stage_source() {
    local version="$1" stage="$2" ref
    ref="$(source_ref "$version")"

    git -C "$repo_root" archive \
        --format=tar.gz \
        --prefix="${project}-${version}/" \
        --output="${stage}/${project}-${version}-src.tar.gz" \
        "$ref"
}

stage_version_changelog() {
    local version="$1" stage="$2" section

    section="$("$changelog_script" section "$version")" ||
        die "CHANGELOG.md has no released section for ${version}, stamp one with changelog.sh bump first"
    printf '%s\n' "$section" >"${stage}/${changelog}"
}

sign_stage() {
    local stage="$1" name
    local -a names

    command -v cosign >/dev/null || die "cosign is not installed, and a release is never published unsigned"

    # Read to the end BEFORE the first bundle is written, or cosign goes on to sign its own signatures.
    readarray -t names < <(staged_files "$stage")

    for name in "${names[@]}"; do
        printf 'signing %s\n' "$name"
        cosign sign-blob --yes --bundle "${stage}/${name}${signature_suffix}" "${stage}/${name}"
    done
}

staged_files() {
    local stage="$1"

    (cd -- "$stage" && find . -maxdepth 1 -type f -printf '%P\n' | sort)
}

publish_file() {
    local stage="$1" version="$2" name="$3" digest path status

    digest="$(sha256sum -- "${stage}/${name}" | cut -d ' ' -f 1)"
    path="${project}/${version}/${name}"

    # -T streams the file, and no Content-Type is sent: the server derives it from the name. mkdir is asked for
    # explicitly so a token without the grant fails as a 403 rather than a bare 404 on the version directory.
    # Both headers are piped into -H @- rather than passed as arguments, so the token never lands in the
    # world-readable /proc/<pid>/cmdline. -T still takes the body from the file, leaving stdin free.
    if ! status="$(printf 'Authorization: Bearer %s\nChecksum-SHA256: %s\n' "$ARTIFACTS_TOKEN" "$digest" \
        | curl --fail-with-body -sS -T "${stage}/${name}" \
        -H @- \
        -o "$response_file" -w '%{http_code}' \
        "${artifacts_api}/_/api/v1/artifacts/${path}?mkdir=1")"; then
        printf 'error: publishing %s failed\n' "$path" >&2
        if [[ -s "$response_file" ]]; then
            cat -- "$response_file" >&2
            printf '\n' >&2
        fi
        return 1
    fi

    case "$status" in
        201) printf 'published %s\n' "$path" ;;
        200) printf 'unchanged %s\n' "$path" ;;
        *) die "unexpected ${status} publishing ${path}" ;;
    esac
}

publish_stage() {
    local stage="$1" version="$2" name
    local -a names

    readarray -t names < <(staged_files "$stage")

    for name in "${names[@]}"; do
        publish_file "$stage" "$version" "$name"
    done
}

main() {
    local version="" dry_run=0 stage name
    local -a names

    while (($# > 0)); do
        case "$1" in
            --dry-run) dry_run=1 ;;
            -*) usage ;;
            *)
                [[ -z "$version" ]] || usage
                version="$1"
                ;;
        esac
        shift
    done

    [[ -n "$version" ]] || usage
    [[ "$version" =~ $semver_pattern ]] ||
        die "'$version' is not a version, expected x.y.z or a pre-release such as 1.0.0-rc.1"

    if ((dry_run == 0)); then
        [[ -n "${ARTIFACTS_TOKEN:-}" ]] ||
            die "ARTIFACTS_TOKEN is empty, there is nothing to authenticate the upload with"
    fi

    stage="${repo_root}/build/publish/${version}"
    rm -rf -- "$stage"
    mkdir -p -- "$stage"

    stage_jars "$version" "$stage"
    stage_source "$version" "$stage"
    stage_version_changelog "$version" "$stage"

    if ((dry_run == 1)); then
        printf 'dry run, staged in %s:\n' "$stage"
        readarray -t names < <(staged_files "$stage")
        for name in "${names[@]}"; do
            printf '  %s\n' "$name"
        done
        printf 'signed nothing and uploaded nothing\n'
        return 0
    fi

    response_file="$(mktemp)"

    sign_stage "$stage"
    publish_stage "$stage" "$version"
}

trap cleanup EXIT

main "$@"
