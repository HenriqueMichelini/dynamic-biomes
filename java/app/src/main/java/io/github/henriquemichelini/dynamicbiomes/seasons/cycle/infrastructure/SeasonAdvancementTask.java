package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application.SeasonAdvancementService;

public final class SeasonAdvancementTask implements Runnable {
    private final SeasonAdvancementService advancementService;

    public SeasonAdvancementTask(SeasonAdvancementService advancementService) {
        this.advancementService = advancementService;
    }

    @Override
    public void run() {
        advancementService.advance();
    }
}
