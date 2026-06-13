package io.github.henriquemichelini.dynamicbiomes.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final String ROOT = "io.github.henriquemichelini.dynamicbiomes";
    private static final String PLUGIN_RUNTIME = ROOT + ".pluginruntime..";
    private static final String BIOME = ROOT + ".biome..";
    private static final String SEASONS = ROOT + ".seasons..";
    // New top-level production packages are treated as feature contexts until classified here.
    private static final Set<String> NON_FEATURE_CONTEXTS = Set.of(
        "biome",
        "configuration",
        "persistence",
        "pluginruntime",
        "seasons",
        "spatial"
    );
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
        ROOT + ".seasons.profile.domain.SeasonClimateAdjustment",
        ROOT + ".seasons.profile.domain.SeasonalAdjustment",
        ROOT + ".seasons.profile.domain.SeasonProfileProvider"
    );
    private static final Set<String> FILE_IO_TYPES = Set.of(
        "java.io.File",
        "java.io.FileDescriptor",
        "java.io.FileFilter",
        "java.io.FileInputStream",
        "java.io.FileOutputStream",
        "java.io.FileReader",
        "java.io.FileWriter",
        "java.io.FilenameFilter",
        "java.io.InputStream",
        "java.io.OutputStream",
        "java.io.RandomAccessFile",
        "java.io.Reader",
        "java.io.Writer",
        "java.nio.channels.AsynchronousFileChannel",
        "java.nio.channels.FileChannel"
    );
    private static final Set<String> FRAMEWORK_ANNOTATION_TYPES = Set.of(
        "jakarta.annotation.ManagedBean",
        "jakarta.annotation.PostConstruct",
        "jakarta.annotation.PreDestroy",
        "jakarta.annotation.Priority",
        "jakarta.annotation.Resource",
        "jakarta.annotation.Resources",
        "javax.annotation.ManagedBean",
        "javax.annotation.PostConstruct",
        "javax.annotation.PreDestroy",
        "javax.annotation.Priority",
        "javax.annotation.Resource",
        "javax.annotation.Resources"
    );
    private static final String[] FRAMEWORK_ANNOTATION_PACKAGES = {
        "jakarta.inject.",
        "jakarta.persistence.",
        "jakarta.transaction.",
        "jakarta.validation.",
        "javax.inject.",
        "javax.persistence.",
        "javax.transaction.",
        "javax.validation.",
        "com.fasterxml.jackson.annotation.",
        "org.hibernate.annotations.",
        "org.springframework."
    };
    private static final DescribedPredicate<JavaClass> FEATURE_CONTEXT =
        new DescribedPredicate<>("a downstream feature context") {
            @Override
            public boolean test(JavaClass javaClass) {
                if (!javaClass.getPackageName().startsWith(ROOT + ".")) {
                    return false;
                }
                String relativePackage = relativePackage(javaClass);
                int separator = relativePackage.indexOf('.');
                String topLevelContext = separator < 0
                    ? relativePackage
                    : relativePackage.substring(0, separator);
                return !topLevelContext.isBlank() && !NON_FEATURE_CONTEXTS.contains(topLevelContext);
            }
        };
    private static final DescribedPredicate<JavaClass> FILE_IO_DEPENDENCY =
        new DescribedPredicate<>("a file-I/O boundary type") {
            @Override
            public boolean test(JavaClass javaClass) {
                return FILE_IO_TYPES.contains(javaClass.getName())
                    || javaClass.getPackageName().startsWith("java.nio.file");
            }
        };
    private static final DescribedPredicate<JavaClass> YAML_OR_CONFIG_API =
        resideInAnyPackage(
            "org.yaml..",
            "org.snakeyaml..",
            "com.fasterxml.jackson.dataformat.yaml..",
            "com.typesafe.config..",
            "org.apache.commons.configuration2.."
        ).as("a YAML or configuration API");
    private static final DescribedPredicate<JavaClass> DATABASE_IMPLEMENTATION_DETAIL =
        resideInAnyPackage(
            "java.sql..",
            "javax.sql..",
            "org.hibernate.."
        ).as("a database implementation detail");
    private static final DescribedPredicate<JavaClass> FRAMEWORK_ANNOTATION =
        new DescribedPredicate<>("a framework annotation") {
            @Override
            public boolean test(JavaClass javaClass) {
                if (!javaClass.isAnnotation()) {
                    return false;
                }
                if (FRAMEWORK_ANNOTATION_TYPES.contains(javaClass.getName())) {
                    return true;
                }
                for (String packagePrefix : FRAMEWORK_ANNOTATION_PACKAGES) {
                    if (javaClass.getPackageName().startsWith(packagePrefix)) {
                        return true;
                    }
                }
                return false;
            }
        };
    private static final DescribedPredicate<JavaClass> DOMAIN_OR_SPATIAL_FORBIDDEN_DEPENDENCY =
        resideInAnyPackage("org.bukkit..", "io.papermc..")
            .or(YAML_OR_CONFIG_API)
            .or(FILE_IO_DEPENDENCY)
            .or(DATABASE_IMPLEMENTATION_DETAIL)
            .or(FRAMEWORK_ANNOTATION)
            .as("a framework or infrastructure boundary type");

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
            .that().resideInAnyPackage(BIOME, SEASONS)
            .should().dependOnClassesThat(FEATURE_CONTEXT)
            .allowEmptyShould(true)
            .check(productionClasses);
    }

    @Test
    void downstreamFeatureContextsMustNotDependOnUpstreamInfrastructure() {
        noClasses()
            .that(FEATURE_CONTEXT)
            .should().dependOnClassesThat().resideInAnyPackage(
                ROOT + ".biome..infrastructure..",
                ROOT + ".seasons..infrastructure.."
            )
            .allowEmptyShould(true)
            .check(productionClasses);
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
            .that(FEATURE_CONTEXT)
            .should().dependOnClassesThat(unpublishedUpstreamType)
            .allowEmptyShould(true)
            .check(productionClasses);
    }

    @Test
    void seasonsMustNotDependOnBiome() {
        noClasses()
            .that().resideInAnyPackage(SEASONS)
            .should().dependOnClassesThat().resideInAnyPackage(BIOME)
            .allowEmptyShould(true)
            .check(productionClasses);
    }

    @Test
    void applicationPackagesMustNotDependOnBukkitEventsOrListeners() {
        noClasses()
            .that().resideInAnyPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.bukkit.event..",
                "io.papermc.paper.event.."
            )
            .allowEmptyShould(true)
            .check(productionClasses);
    }

    @Test
    void domainPackagesMustRemainFrameworkAndInfrastructureIndependent() {
        noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat(DOMAIN_OR_SPATIAL_FORBIDDEN_DEPENDENCY)
            .allowEmptyShould(true)
            .check(productionClasses);
    }

    @Test
    void spatialMustRemainFrameworkAndInfrastructureIndependent() {
        noClasses()
            .that().resideInAnyPackage(ROOT + ".spatial..")
            .should().dependOnClassesThat(DOMAIN_OR_SPATIAL_FORBIDDEN_DEPENDENCY)
            .allowEmptyShould(true)
            .check(productionClasses);
    }

    private static String relativePackage(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (packageName.equals(ROOT)) {
            return "";
        }
        return packageName.startsWith(ROOT + ".")
            ? packageName.substring(ROOT.length() + 1)
            : packageName;
    }

    private static DescribedPredicate<JavaClass> resideInAnyPackage(String... packages) {
        return JavaClass.Predicates.resideInAnyPackage(packages);
    }
}
