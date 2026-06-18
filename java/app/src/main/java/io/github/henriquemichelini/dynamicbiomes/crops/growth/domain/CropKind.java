package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import java.util.Optional;

public enum CropKind {
    WHEAT("wheat"),
    CARROTS("carrots"),
    POTATOES("potatoes"),
    BEETROOT("beetroot");

    private final String policyKey;

    CropKind(String policyKey) {
        this.policyKey = policyKey;
    }

    public String policyKey() {
        return policyKey;
    }

    public static Optional<CropKind> fromPolicyKey(String policyKey) {
        for (CropKind cropKind : values()) {
            if (cropKind.policyKey.equals(policyKey)) {
                return Optional.of(cropKind);
            }
        }
        return Optional.empty();
    }
}
