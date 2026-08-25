package dev.detpikachu.unpluggedafk.common.formatting;

import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;

@ApiStatus.Internal
public final class DurationFormatting {

    public static String format(Duration duration) {
        final var seconds = duration.toSeconds();
        if (seconds < 60) {
            return seconds + " second(s)";
        }

        final var minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " minute(s)";
        }

        return duration.toHours() + " hour(s) " + duration.toMinutesPart() + " minute(s)";
    }
}
