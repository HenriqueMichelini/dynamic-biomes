package io.github.henriquemichelini.dynamicbiomes.ore.identity.domain;

import java.util.regex.Pattern;

public record OreKind(String value) {
    private static final Pattern VALID_ID = Pattern.compile(
        "[a-z0-9._-]+:[a-z0-9/._-]+"
    );

    public OreKind {
        if (value == null || !VALID_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Ore kind must be a valid lowercase namespaced identifier"
            );
        }
    }
}
