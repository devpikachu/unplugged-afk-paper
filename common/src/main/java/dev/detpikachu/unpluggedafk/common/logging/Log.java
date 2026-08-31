package dev.detpikachu.unpluggedafk.common.logging;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BooleanSupplier;

@ApiStatus.Internal
public final class Log {

    private static final String FALLBACK_NAME = "unplugged-afk";

    private static Logger logger = LoggerFactory.getLogger(FALLBACK_NAME);
    private static BooleanSupplier debugEnabled = () -> false;

    public static void configure(Logger logger, BooleanSupplier debugEnabled) {
        Log.logger = logger;
        Log.debugEnabled = debugEnabled;
    }

    public static Logger logger() {
        return logger;
    }

    public static void debug(String message, @Nullable Object... arguments) {
        if (debugEnabled.getAsBoolean()) {
            logger.info(message, arguments);
        }
    }

    public static void info(String message, @Nullable Object... arguments) {
        logger.info(message, arguments);
    }

    public static void warn(String message, @Nullable Object... arguments) {
        logger.warn(message, arguments);
    }

    public static void error(String message, @Nullable Object... arguments) {
        logger.error(message, arguments);
    }
}
