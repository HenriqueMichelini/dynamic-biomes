package io.github.henriquemichelini.dynamicbiomes.animals.breeding.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.animals.breeding.application.AnimalBreedingService;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalBreedingPolicy;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalBreedingPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PaperAnimalBreedingListenerTest {
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");
    private static final ItemStack BREED_ITEM = new ItemStack() {};

    @ParameterizedTest
    @CsvSource({"COW, COW", "SHEEP, SHEEP", "PIG, PIG", "CHICKEN, CHICKEN"})
    void cancelsEligibleBreedingWhenThePolicyDeniesIt(EntityType type, AnimalKind expectedAnimal) {
        EntityBreedEvent event = eventFor(type, type, player(), BREED_ITEM);
        PaperAnimalBreedingListener listener = listener(animal -> {
            assertEquals(expectedAnimal, animal);
            return Optional.of(new AnimalBreedingPolicy(Map.of(WINTER, 0.0)));
        });

        listener.onEntityBreed(event);

        assertTrue(event.isCancelled());
        assertEquals(3, event.getExperience());
        assertSame(BREED_ITEM, event.getBredWith());
    }

    @Test
    void leavesAllowedBreedingAndItsExperienceUnchanged() {
        EntityBreedEvent event = eventFor(EntityType.COW, EntityType.COW, player(), BREED_ITEM);

        listener(animal -> Optional.empty()).onEntityBreed(event);

        assertFalse(event.isCancelled());
        assertEquals(3, event.getExperience());
        assertSame(BREED_ITEM, event.getBredWith());
    }

    @Test
    void leavesCancelledBreedingUntouched() {
        EntityBreedEvent event = eventFor(EntityType.COW, EntityType.COW, player(), BREED_ITEM);
        event.setCancelled(true);

        ineligibleListener().onEntityBreed(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void skipsUnsupportedEntitiesAndMismatchedParents() {
        PaperAnimalBreedingListener listener = ineligibleListener();
        EntityBreedEvent unsupportedEvent = eventFor(
            EntityType.VILLAGER, EntityType.VILLAGER, player(), BREED_ITEM
        );
        EntityBreedEvent mismatchedEvent = eventFor(
            EntityType.COW, EntityType.SHEEP, player(), BREED_ITEM
        );

        listener.onEntityBreed(unsupportedEvent);
        listener.onEntityBreed(mismatchedEvent);

        assertFalse(unsupportedEvent.isCancelled());
        assertFalse(mismatchedEvent.isCancelled());
    }

    @Test
    void skipsBreedingWithoutPlayerFeedingMetadata() {
        PaperAnimalBreedingListener listener = ineligibleListener();
        EntityBreedEvent noBreeder = eventFor(EntityType.COW, EntityType.COW, null, BREED_ITEM);
        EntityBreedEvent nonPlayerBreeder = eventFor(
            EntityType.COW, EntityType.COW, entity(EntityType.ZOMBIE), BREED_ITEM
        );
        EntityBreedEvent noBreedItem = eventFor(EntityType.COW, EntityType.COW, player(), null);

        listener.onEntityBreed(noBreeder);
        listener.onEntityBreed(nonPlayerBreeder);
        listener.onEntityBreed(noBreedItem);

        assertFalse(noBreeder.isCancelled());
        assertFalse(nonPlayerBreeder.isCancelled());
        assertFalse(noBreedItem.isCancelled());
    }

    private static PaperAnimalBreedingListener listener(AnimalBreedingPolicyProvider provider) {
        return new PaperAnimalBreedingListener(new AnimalBreedingService(
            provider,
            () -> WINTER,
            () -> { throw new AssertionError("No roll is needed for these policies"); }
        ));
    }

    private static PaperAnimalBreedingListener ineligibleListener() {
        return listener(animal -> {
            throw new AssertionError("Ineligible events must not read the policy");
        });
    }

    private static EntityBreedEvent eventFor(
        EntityType childAndMother,
        EntityType father,
        LivingEntity breeder,
        ItemStack breedItem
    ) {
        return new EntityBreedEvent(
            entity(childAndMother),
            entity(childAndMother),
            entity(father),
            breeder,
            breedItem,
            3
        );
    }

    private static LivingEntity entity(EntityType type) {
        return entityProxy(LivingEntity.class, type);
    }

    private static Player player() {
        return entityProxy(Player.class, EntityType.PLAYER);
    }

    private static <T extends LivingEntity> T entityProxy(Class<T> apiType, EntityType entityType) {
        return apiType.cast(Proxy.newProxyInstance(
            apiType.getClassLoader(),
            new Class<?>[] {apiType},
            (entity, method, arguments) -> {
                if (method.getName().equals("getType")) {
                    return entityType;
                }
                throw new AssertionError("Unexpected entity call: " + method.getName());
            }
        ));
    }
}
