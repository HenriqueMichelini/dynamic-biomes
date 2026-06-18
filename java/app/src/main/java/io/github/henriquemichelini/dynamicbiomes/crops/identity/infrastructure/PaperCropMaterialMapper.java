package io.github.henriquemichelini.dynamicbiomes.crops.identity.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

public final class PaperCropMaterialMapper {
    private static final Map<Material, CropKind> CROP_KINDS = Map.of(
        Material.WHEAT,
        CropKind.WHEAT,
        Material.CARROTS,
        CropKind.CARROTS,
        Material.POTATOES,
        CropKind.POTATOES,
        Material.BEETROOTS,
        CropKind.BEETROOT
    );

    private PaperCropMaterialMapper() {}

    public static Optional<CropKind> cropKindFor(Material material) {
        return Optional.ofNullable(CROP_KINDS.get(material));
    }

    public static boolean isSupportedCrop(Material material) {
        return CROP_KINDS.containsKey(material);
    }
}
