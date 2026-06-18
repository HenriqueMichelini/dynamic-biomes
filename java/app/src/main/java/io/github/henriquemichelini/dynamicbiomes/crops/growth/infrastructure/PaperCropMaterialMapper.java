package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropKind;
import java.util.Optional;
import org.bukkit.Material;

public final class PaperCropMaterialMapper {
    public Optional<CropKind> cropKindFor(Material material) {
        return switch (material) {
            case WHEAT -> Optional.of(CropKind.WHEAT);
            case CARROTS -> Optional.of(CropKind.CARROTS);
            default -> Optional.empty();
        };
    }
}
