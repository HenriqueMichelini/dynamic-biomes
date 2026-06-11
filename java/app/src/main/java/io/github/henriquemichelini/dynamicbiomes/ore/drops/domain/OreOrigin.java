package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import org.bukkit.Location;
import org.bukkit.block.Biome;

import net.kyori.adventure.text.BlockNBTComponent.WorldPos.Coordinate;

public record OreOrigin(Biome biome, Coordinate coordinate, Location) {}
