package io.github.henriquemichelini.dynamicbiomes.biome.identity.domain;

import java.util.regex.Pattern;

public record BiomeId(String value) {
    private static final Pattern VALID_ID = Pattern.compile(
        "[a-z0-9._-]+:[a-z0-9/._-]+"
    );

    public BiomeId {
        if (value == null || !VALID_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Biome identity must be a valid lowercase namespaced identifier"
            );
        }
    }
}
