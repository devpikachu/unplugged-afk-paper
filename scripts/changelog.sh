#!/usr/bin/env bash
#
# Changelog-driven release helper. Run it with no arguments for usage.

set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
changelog="${repo_root}/CHANGELOG.md"
gradle_properties="${repo_root}/gradle.properties"
release_branch="main"

die() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat >&2 <<'EOF'
usage:
    scripts/changelog.sh notes <version>
        Print the body of a released CHANGELOG.md section, without its heading. CI feeds this to the GitHub release.

    scripts/changelog.sh bump <version|major|minor|patch> [--no-git]
        Stamp the Unreleased section as a new release, move gradle.properties to the next dev version, then commit and
        tag. Never pushes; that stays a manual step.
EOF
    exit 2
}

# Drop leading and trailing blank lines, keep the interior ones.
trim_blank_lines() {
    awk '
        /[^[:space:]]/ {
            for (i = 0; i < pending; i++) print ""
            pending = 0
            started = 1
            print
            next
        }
        started { pending++ }
    '
}

# Print every line below the "## [<version>]" heading, stopping at the next heading or at the link reference block.
section_body() {
    awk -v heading="## [$1]" '
        !found && substr($0, 1, length(heading)) == heading { found = 1; next }
        found && substr($0, 1, 3) == "## " { exit }
        found && /^\[[^]]+\]: / { exit }
        found { print }
    ' "$changelog" | trim_blank_lines
}

# The newest version that has a released section, ignoring Unreleased. Empty if there is none.
latest_released_version() {
    awk '
        substr($0, 1, 4) != "## [" { next }
        {
            rest = substr($0, 5)
            end = index(rest, "]")
            if (end == 0) next
            name = substr(rest, 1, end - 1)
            if (name == "Unreleased") next
            print name
            exit
        }
    ' "$changelog"
}

next_version() {
    local major minor patch
    IFS=. read -r major minor patch <<<"$1"
    case "$2" in
        major) printf '%d.0.0\n' "$((major + 1))" ;;
        minor) printf '%d.%d.0\n' "$major" "$((minor + 1))" ;;
        patch) printf '%d.%d.%d\n' "$major" "$minor" "$((patch + 1))" ;;
        *) die "unknown bump kind '$2'" ;;
    esac
}

# The repository URL, taken from the changelog's own link references rather than from the git remote.
changelog_base_url() {
    local prefix='[unreleased]: ' line base
    line="$(grep -m1 '^\[unreleased\]: ' "$changelog" || true)"
    [[ -n "$line" ]] || die "CHANGELOG.md has no '[unreleased]:' link reference"
    line="${line#"$prefix"}"
    base="${line%%/compare/*}"
    [[ "$base" != "$line" ]] || die "the '[unreleased]:' link reference is not a /compare/ URL"
    printf '%s\n' "$base"
}

# Move the Unreleased entries under a dated heading and refresh the link references.
stamp_changelog() {
    local version="$1" date="$2" previous="$3" base="$4" stamped

    stamped="$(awk -v version="$version" -v date="$date" -v previous="$previous" -v base="$base" '
        need_blank {
            need_blank = 0
            if ($0 != "") print ""
        }
        !stamped && $0 == "## [Unreleased]" {
            print $0
            print ""
            print "## [" version "] - " date
            stamped = 1
            need_blank = 1
            next
        }
        /^\[unreleased\]: / {
            print "[unreleased]: " base "/compare/v" version "...HEAD"
            if (previous == "") print "[" version "]: " base "/releases/tag/v" version
            else print "[" version "]: " base "/compare/v" previous "...v" version
            next
        }
        { print }
        END { if (!stamped) exit 1 }
    ' "$changelog")" || die "CHANGELOG.md has no '## [Unreleased]' heading"

    printf '%s\n' "$stamped" >"$changelog"
}

set_dev_version() {
    local dev_version="$1" properties

    properties="$(awk -v dev_version="$dev_version" '
        substr($0, 1, 8) == "version=" { print "version=" dev_version; next }
        { print }
    ' "$gradle_properties")"

    printf '%s\n' "$properties" >"$gradle_properties"
}

cmd_notes() {
    local version="${1:-}" body
    [[ -n "$version" ]] || usage

    body="$(section_body "$version")"
    [[ -n "$body" ]] || die "CHANGELOG.md has no entries under '## [$version]'"

    printf '%s\n' "$body"
}

cmd_bump() {
    local requested="${1:-}" do_git=1
    [[ -n "$requested" ]] || usage

    case "${2:-}" in
        '') ;;
        --no-git) do_git=0 ;;
        *) usage ;;
    esac

    local previous version
    previous="$(latest_released_version)"

    case "$requested" in
        major | minor | patch) version="$(next_version "${previous:-0.0.0}" "$requested")" ;;
        *) version="$requested" ;;
    esac

    [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
        die "'$requested' is neither an x.y.z version nor one of major, minor, patch"
    [[ -z "$(section_body "$version")" ]] || die "CHANGELOG.md already has a '## [$version]' section"
    [[ -n "$(section_body Unreleased)" ]] || die "the Unreleased section is empty, there is nothing to release"

    local branch
    branch="$(git -C "$repo_root" symbolic-ref --quiet --short HEAD || true)"
    [[ "$branch" == "$release_branch" ]] ||
        die "on '${branch:-detached HEAD}', releases are cut from '$release_branch'"
    [[ -z "$(git -C "$repo_root" status --porcelain)" ]] || die "the working tree is dirty, commit or stash first"
    if git -C "$repo_root" rev-parse --quiet --verify "refs/tags/v$version" >/dev/null 2>&1; then
        die "tag 'v$version' already exists"
    fi

    local base today dev_version
    base="$(changelog_base_url)"
    today="$(date +%F)"
    dev_version="$(next_version "$version" patch)-SNAPSHOT"

    stamp_changelog "$version" "$today" "$previous" "$base"
    set_dev_version "$dev_version"
    printf 'stamped CHANGELOG.md as %s, moved the dev version to %s\n' "$version" "$dev_version"

    if ((do_git == 0)); then
        printf 'left uncommitted, --no-git was passed\n'
        return
    fi

    git -C "$repo_root" add CHANGELOG.md gradle.properties
    git -C "$repo_root" commit --quiet -m "release $version"
    git -C "$repo_root" tag -a "v$version" -m "v$version"
    printf 'committed and tagged v%s\n\npush it to build the draft release:\n\n    git push --follow-tags\n' "$version"
}

case "${1:-}" in
    notes)
        shift
        cmd_notes "$@"
        ;;
    bump)
        shift
        cmd_bump "$@"
        ;;
    *) usage ;;
esac
