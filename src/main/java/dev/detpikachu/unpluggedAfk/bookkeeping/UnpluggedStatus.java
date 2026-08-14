package dev.detpikachu.unpluggedAfk.bookkeeping;

public enum UnpluggedStatus {

    ACTIVE,
    INACTIVE,
    EXPIRED,
    INTERRUPTED,
    TERMINATED;

    public static String formatStatus(UnpluggedStatus status)
    {
        return switch (status)
        {
            case ACTIVE -> "§6Active§r";
            case INACTIVE -> "§aInactive§r";
            case EXPIRED -> "§bExpired§r";
            case INTERRUPTED -> "§cInterrupted§r";
            case TERMINATED -> "§cTerminated§r";
        };
    }
}
