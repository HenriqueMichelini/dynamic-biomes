package io.github.henriquemichelini.dynamicbiomes.crops.identity.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class PaperCropMaterialMapperTest {
    @Test
    void mapsSupportedCropMaterialsToCropKinds() {
        Map<Material, CropKind> supportedCropKinds = Map.of(
            Material.WHEAT,
            CropKind.WHEAT,
            Material.CARROTS,
            CropKind.CARROTS,
            Material.POTATOES,
            CropKind.POTATOES,
            Material.BEETROOTS,
            CropKind.BEETROOT
        );

        supportedCropKinds.forEach((material, cropKind) -> {
            assertEquals(
                cropKind,
                PaperCropMaterialMapper.cropKindFor(material).orElseThrow()
            );
            assertTrue(PaperCropMaterialMapper.isSupportedCrop(material));
        });
    }

    @Test
    void rejectsUnsupportedCropMaterials() {
        assertTrue(PaperCropMaterialMapper.cropKindFor(Material.STONE).isEmpty());
        assertTrue(PaperCropMaterialMapper.cropKindFor(Material.NETHER_WART).isEmpty());
        assertFalse(PaperCropMaterialMapper.isSupportedCrop(Material.STONE));
        assertFalse(PaperCropMaterialMapper.isSupportedCrop(Material.NETHER_WART));
    }
}
