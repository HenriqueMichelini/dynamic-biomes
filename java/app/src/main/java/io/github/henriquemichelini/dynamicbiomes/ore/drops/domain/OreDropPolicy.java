package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import java.util.Map;

public final class OreDropPolicy {
    private final String key;
    private final Map<String, OreDropMultiplierRange> multiplierRangesByOreKey;

    public OreDropPolicy(
        String key,
        Map<String, OreDropMultiplierRange> multiplierRangesByOreKey
    ) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Ore drop policy key must not be blank");
        }
        if (multiplierRangesByOreKey == null || multiplierRangesByOreKey.isEmpty()) {
            throw new IllegalArgumentException("Ore drop policy must define at least one ore multiplier range");
        }

        this.key = key;
        this.multiplierRangesByOreKey = Map.copyOf(multiplierRangesByOreKey);
    }

    public String key() {
        return key;
    }

    public OreDropMultiplierRange multiplierRangeFor(String oreKey) {
        OreDropMultiplierRange range = multiplierRangesByOreKey.get(oreKey);
        if (range == null) {
            throw new IllegalArgumentException(
                "Missing ore drop multiplier range for ore key: " + oreKey
            );
        }
        return range;
    }
}
