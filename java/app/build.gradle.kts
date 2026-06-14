plugins {
    java
}

group = "io.github.henriquemichelini"
version = "1.0.0"

val pluginTest by sourceSets.creating

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    implementation("org.yaml:snakeyaml:2.4")

    testImplementation(libs.archunit.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    add(pluginTest.implementationConfigurationName, libs.junit.jupiter)
    add(pluginTest.implementationConfigurationName, libs.mockbukkit)
    add(pluginTest.implementationConfigurationName, "io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    add(pluginTest.runtimeOnlyConfigurationName, "org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.test {
    systemProperty("dynamicBiomes.pluginVersion", project.version.toString())
    useJUnitPlatform()
}

val pluginTestTask = tasks.register<Test>("pluginTest") {
    description = "Verifies the built plugin JAR through MockBukkit's public API."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    dependsOn(tasks.jar)
    testClassesDirs = pluginTest.output.classesDirs
    classpath = pluginTest.runtimeClasspath
    systemProperty("dynamicBiomes.pluginJar", tasks.jar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("dynamicBiomes.pluginVersion", project.version.toString())
    useJUnitPlatform()
}

tasks.check {
    dependsOn(pluginTestTask)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString()
    )

    inputs.properties(props)

    filesMatching("plugin.yml") {
        expand(props)
    }
}

val pluginArchiveName = "dynamic-biomes"

val repositoryRoot = if (rootDir.name == "java") {
    rootDir.parentFile
} else {
    rootDir
}

val dockerDirectory = repositoryRoot.resolve("docker")
val dockerComposeFile = dockerDirectory.resolve("compose.dev.yml")
val paperPluginsDirectory = dockerDirectory.resolve("server-data/plugins")

base {
    archivesName.set(pluginArchiveName)
}

tasks.register<Delete>("cleanDockerPlugin") {
    group = "docker"
    description = "Removes old Dynamic Biomes plugin jars from the Docker Paper server plugins directory."

    delete(
        fileTree(paperPluginsDirectory) {
            include("$pluginArchiveName-*.jar")
            include("$pluginArchiveName.jar")
        }
    )
}

tasks.register<Copy>("copyPluginToDocker") {
    group = "docker"
    description = "Builds the plugin and copies the jar into the Docker Paper server plugins directory."

    dependsOn("cleanDockerPlugin")
    dependsOn(tasks.named("jar"))

    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(paperPluginsDirectory)
}

tasks.register<Exec>("startPaperDocker") {
    group = "docker"
    description = "Starts the Docker Paper server."

    workingDir = dockerDirectory
    commandLine("docker", "compose", "-f", dockerComposeFile.absolutePath, "up", "-d")
}

tasks.register<Exec>("stopPaperDocker") {
    group = "docker"
    description = "Stops the Docker Paper server."

    workingDir = dockerDirectory
    commandLine("docker", "compose", "-f", dockerComposeFile.absolutePath, "down")
}

tasks.register<Exec>("restartPaperDocker") {
    group = "docker"
    description = "Builds the plugin, copies it into the Paper server, and restarts Docker."

    dependsOn("copyPluginToDocker")

    workingDir = dockerDirectory
    commandLine("docker", "compose", "-f", dockerComposeFile.absolutePath, "restart", "paper")
}
