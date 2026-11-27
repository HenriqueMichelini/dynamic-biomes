package io.github.henriquemichelini.dynamicbiomes.animals.breeding.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.animals.breeding.application.AnimalBreedingService;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalKind;
import java.util.Objects;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

public final class PaperAnimalBreedingListener implements Listener {
    private final AnimalBreedingService breedingService;

    public PaperAnimalBreedingListener(AnimalBreedingService breedingService) {
        this.breedingService = Objects.requireNonNull(breedingService);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.isCancelled() || !(event.getBreeder() instanceof Player) || event.getBredWith() == null) {
            return;
        }

        EntityType childType = event.getEntityType();
        if (event.getMother().getType() != childType || event.getFather().getType() != childType) {
            return;
        }

        AnimalKind animal = switch (childType) {
            case COW -> AnimalKind.COW;
            case SHEEP -> AnimalKind.SHEEP;
            case PIG -> AnimalKind.PIG;
            case CHICKEN -> AnimalKind.CHICKEN;
            default -> null;
        };
        if (animal != null && !breedingService.allowsBreeding(animal)) {
            event.setCancelled(true);
        }
    }
}
