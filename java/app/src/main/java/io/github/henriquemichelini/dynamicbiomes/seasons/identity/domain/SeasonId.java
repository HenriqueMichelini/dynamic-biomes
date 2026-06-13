package io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain;

import java.util.regex.Pattern;

public record SeasonId(String value) {
    private static final Pattern VALID_ID = Pattern.compile(
        "[a-z0-9._-]+:[a-z0-9/._-]+"
    );

    public SeasonId {
        if (value == null || !VALID_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Season identity must be a valid lowercase namespaced identifier"
            );
        }
    }
}
