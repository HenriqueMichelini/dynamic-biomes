package io.github.henriquemichelini.dynamicbiomes.crops.identity.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CropKindTest {
    @Test
    void parsesSupportedPolicyKeys() {
        Map<String, CropKind> supportedCropKinds = Map.of(
            "wheat",
            CropKind.WHEAT,
            "carrots",
            CropKind.CARROTS,
            "potatoes",
            CropKind.POTATOES,
            "beetroot",
            CropKind.BEETROOT
        );

        supportedCropKinds.forEach((policyKey, cropKind) ->
            assertEquals(cropKind, CropKind.fromPolicyKey(policyKey).orElseThrow())
        );
    }

    @Test
    void rejectsUnsupportedPolicyKeys() {
        assertAll(
            () -> assertTrue(CropKind.fromPolicyKey(null).isEmpty()),
            () -> assertTrue(CropKind.fromPolicyKey("").isEmpty()),
            () -> assertTrue(CropKind.fromPolicyKey("   ").isEmpty()),
            () -> assertTrue(CropKind.fromPolicyKey(" wheat ").isEmpty()),
            () -> assertTrue(CropKind.fromPolicyKey("WHEAT").isEmpty()),
            () -> assertTrue(CropKind.fromPolicyKey("nether_wart").isEmpty())
        );
    }

    @Test
    void exposesStablePolicyKeys() {
        assertAll(
            () -> assertEquals("wheat", CropKind.WHEAT.policyKey()),
            () -> assertEquals("carrots", CropKind.CARROTS.policyKey()),
            () -> assertEquals("potatoes", CropKind.POTATOES.policyKey()),
            () -> assertEquals("beetroot", CropKind.BEETROOT.policyKey())
        );
    }
}
