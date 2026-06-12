package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

public interface OreDropPolicyProvider {
    OreDropPolicy policyFor(String policyKey);
}
