package io.github.henriquemichelini.dynamicbiomes.ore.drops.application;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.UnsupportedOreDropConfigurationException;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.Objects;
import java.util.Optional;

public final class OreInspectionService {
    private final BiomeResolver biomeResolver;
    private final OreDropPolicyProvider policyProvider;
    private final OreOriginTrackingService originTracking;

    public OreInspectionService(
        BiomeResolver biomeResolver,
        OreDropPolicyProvider policyProvider,
        OreOriginTrackingService originTracking
    ) {
        this.biomeResolver = Objects.requireNonNull(biomeResolver);
        this.policyProvider = Objects.requireNonNull(policyProvider);
        this.originTracking = Objects.requireNonNull(originTracking);
    }

    public Inspection inspect(BlockPosition position, OreKind oreKind) {
        BiomeId biomeId;
        try {
            biomeId = biomeResolver.resolve(position).biomeId();
        } catch (UnsupportedBiomeException exception) {
            return new UnsupportedBiome(exception.biomeId());
        }

        boolean policySupported = hasPolicyRule(biomeId, oreKind);
        return new SupportedBiome(biomeId, policySupported, originTracking.originTypeAt(position));
    }

    private boolean hasPolicyRule(BiomeId biomeId, OreKind oreKind) {
        try {
            policyProvider.policyFor(biomeId).multiplierRangeFor(oreKind);
            return true;
        } catch (UnsupportedOreDropConfigurationException exception) {
            return false;
        }
    }

    public sealed interface Inspection permits SupportedBiome, UnsupportedBiome {}

    public record SupportedBiome(
        BiomeId biomeId,
        boolean policySupported,
        OreOriginType originType
    ) implements Inspection {
        public boolean eligibleForMultiplier() {
            return policySupported && originType.isEligibleForBiomeBasedMultiplier();
        }
    }

    public record UnsupportedBiome(Optional<BiomeId> biomeId) implements Inspection {}
}
