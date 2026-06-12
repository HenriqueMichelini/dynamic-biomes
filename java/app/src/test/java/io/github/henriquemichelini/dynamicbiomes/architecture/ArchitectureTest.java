package io.github.henriquemichelini.dynamicbiomes.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final String ROOT = "io.github.henriquemichelini.dynamicbiomes";
    private static final String PLUGIN_RUNTIME = ROOT + ".pluginruntime..";
    private static final String[] UPSTREAM_CONTEXTS = {
        ROOT + ".biome..",
        ROOT + ".seasons.."
    };
    private static final String[] DOWNSTREAM_CONTEXTS = {
        ROOT + ".ore..",
        ROOT + ".crops..",
        ROOT + ".trees..",
        ROOT + ".animals.."
    };
    private static final Set<String> PUBLISHED_UPSTREAM_CONTRACTS = Set.of(
        ROOT + ".biome.identity.domain.BiomeId",
        ROOT + ".biome.resolution.domain.BiomeContext",
        ROOT + ".biome.resolution.domain.BiomeResolver",
        ROOT + ".biome.profile.domain.BiomeProfile",
        ROOT + ".biome.profile.domain.BiomeProfileProvider",
        ROOT + ".biome.profile.domain.BiomeTag",
        ROOT + ".biome.profile.domain.ClimateProfile",
        ROOT + ".biome.profile.domain.Humidity",
        ROOT + ".biome.profile.domain.Temperature",
        ROOT + ".biome.profile.domain.Fertility",
        ROOT + ".biome.profile.domain.MineralRichness",
        ROOT + ".biome.profile.domain.EcologicalPressure",
        ROOT + ".biome.dynamics.domain.EcologicalRegionState",
        ROOT + ".seasons.identity.domain.SeasonId",
        ROOT + ".seasons.cycle.domain.CurrentSeasonQuery",
        ROOT + ".seasons.profile.domain.SeasonProfile",
        ROOT + ".seasons.profile.domain.SeasonProfileProvider"
    );

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(ROOT);
    }

    @Test
    void packagesOutsidePluginRuntimeMustNotDependOnPluginRuntime() {
        noClasses()
            .that().resideOutsideOfPackage(PLUGIN_RUNTIME)
            .should().dependOnClassesThat().resideInAnyPackage(PLUGIN_RUNTIME)
            .allowEmptyShould(true)
            .check(productionClasses);
    }

    @Test
    void upstreamEnvironmentalContextsMustNotDependOnDownstreamFeatureContexts() {
        noClasses()
            .that().resideInAnyPackage(UPSTREAM_CONTEXTS)
            .should().dependOnClassesThat().resideInAnyPackage(DOWNSTREAM_CONTEXTS)
            .allowEmptyShould(true)
            .check(productionClasses);
    }

    @Test
    void downstreamFeatureContextsMustNotDependOnUpstreamInfrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage(DOWNSTREAM_CONTEXTS)
            .should().dependOnClassesThat().resideInAnyPackage(
                ROOT + ".biome..infrastructure..",
                ROOT + ".seasons..infrastructure.."
            )
            .allowEmptyShould(true);

        rule.check(productionClasses);
    }

    @Test
    void downstreamFeatureContextsMayOnlyDependOnPublishedUpstreamContracts() {
        DescribedPredicate<JavaClass> unpublishedUpstreamType =
            new DescribedPredicate<>("an unpublished biome or seasons type") {
                @Override
                public boolean test(JavaClass javaClass) {
                    String packageName = javaClass.getPackageName();
                    boolean belongsToUpstream = packageName.startsWith(ROOT + ".biome.")
                        || packageName.startsWith(ROOT + ".seasons.");
                    return belongsToUpstream && !PUBLISHED_UPSTREAM_CONTRACTS.contains(javaClass.getName());
                }
            };

        noClasses()
            .that().resideInAnyPackage(DOWNSTREAM_CONTEXTS)
            .should().dependOnClassesThat(unpublishedUpstreamType)
            .allowEmptyShould(true)
            .check(productionClasses);
    }

    @Test
    void domainPackagesMustRemainFrameworkAndInfrastructureIndependent() {
        noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.bukkit..",
                "io.papermc..",
                "org.yaml..",
                "org.snakeyaml..",
                "com.fasterxml.jackson.dataformat.yaml..",
                "java.io..",
                "java.nio.file..",
                "java.sql..",
                "javax.persistence..",
                "jakarta.persistence..",
                "org.springframework.."
            )
            .allowEmptyShould(true)
            .check(productionClasses);
    }
}
