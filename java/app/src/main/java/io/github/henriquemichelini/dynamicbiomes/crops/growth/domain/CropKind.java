package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

public enum CropKind {
    WHEAT("wheat"),
    CARROTS("carrots");

    private final String policyKey;

    CropKind(String policyKey) {
        this.policyKey = policyKey;
    }

    public String policyKey() {
        return policyKey;
    }
}
