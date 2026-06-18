package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropKind;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class PaperCropMaterialMapperTest {
    @Test
    void mapsWheatMaterialToWheatCropKind() {
        assertEquals(
            CropKind.WHEAT,
            PaperCropMaterialMapper.cropKindFor(Material.WHEAT).orElseThrow()
        );
        assertTrue(PaperCropMaterialMapper.isSupportedCrop(Material.WHEAT));
    }

    @Test
    void mapsCarrotMaterialToCarrotCropKind() {
        assertEquals(
            CropKind.CARROTS,
            PaperCropMaterialMapper.cropKindFor(Material.CARROTS).orElseThrow()
        );
        assertTrue(PaperCropMaterialMapper.isSupportedCrop(Material.CARROTS));
    }

    @Test
    void rejectsUnsupportedCropMaterials() {
        assertTrue(PaperCropMaterialMapper.cropKindFor(Material.POTATOES).isEmpty());
        assertTrue(PaperCropMaterialMapper.cropKindFor(Material.BEETROOTS).isEmpty());
        assertTrue(PaperCropMaterialMapper.cropKindFor(Material.STONE).isEmpty());
        assertFalse(PaperCropMaterialMapper.isSupportedCrop(Material.POTATOES));
        assertFalse(PaperCropMaterialMapper.isSupportedCrop(Material.BEETROOTS));
        assertFalse(PaperCropMaterialMapper.isSupportedCrop(Material.STONE));
    }
}
