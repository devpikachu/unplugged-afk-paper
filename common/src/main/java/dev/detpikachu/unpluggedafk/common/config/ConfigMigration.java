package dev.detpikachu.unpluggedafk.common.config;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

@ApiStatus.Internal
public record ConfigMigration(int from, Consumer<Document> apply) {}
