package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropKind;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class PaperCropMaterialMapperTest {
    @Test
    void mapsWheatMaterialToWheatCropKind() {
        assertEquals(
            CropKind.WHEAT,
            new PaperCropMaterialMapper().cropKindFor(Material.WHEAT).orElseThrow()
        );
    }

    @Test
    void mapsCarrotMaterialToCarrotCropKind() {
        assertEquals(
            CropKind.CARROTS,
            new PaperCropMaterialMapper().cropKindFor(Material.CARROTS).orElseThrow()
        );
    }

    @Test
    void rejectsUnsupportedCropMaterials() {
        PaperCropMaterialMapper mapper = new PaperCropMaterialMapper();

        assertTrue(mapper.cropKindFor(Material.POTATOES).isEmpty());
        assertTrue(mapper.cropKindFor(Material.BEETROOTS).isEmpty());
        assertTrue(mapper.cropKindFor(Material.STONE).isEmpty());
    }
}
