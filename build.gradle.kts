import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar

plugins {
    id("st.evening.mc.prelude.build") version "1.0.1"
    id("st.evening.kt.invokecontrol") version "2.4.20-0.1.0"
    id("io.kotest") version "6.1.11"
    id("maven-publish")
}

val projectModId: String = "prelude"
group = "st.evening.mc.prelude"
version = "0.0.1a"

preludeBuild {
    modId = projectModId
    mod {
        accessTransformerFile = "${projectModId}_at.cfg"
        loadingPluginClass = "coremod.PreludeLoadingPlugin"
    }
    minecraft {
        mcVersion = "1.12.2"
        deobf {
            mappingsChannel = "stable"
            mappingsVersion = "39"
        }
    }
    presets {
        mixin {
            enable = true
            mixinConfigs.add("$projectModId.mixin.json")
        }
        prelude {
            enable = true
            includeDeps = false
        }
    }
}

repositories {
    maven {
        name = "Cleanroom"
        url = uri("https://maven.cleanroommc.com/")
    }
    maven {
        name = "GTCEu"
        url = uri("https://maven.gtceu.com/")
    }
}

dependencies {
    api("st.evening.kt.invokecontrol:kt-invoke-control-lib:0.1.0")
    implementation(rfg.deobf("com.cleanroommc:groovyscript:1.4.3:") as String) {
        exclude(group = "mezz.jei")
        exclude(group = "curse.maven")
        isTransitive = false
    }

    testImplementation("io.kotest:kotest-assertions-core:6.1.10")
    testImplementation("io.kotest:kotest-property:6.1.10")
    testImplementation("io.kotest:kotest-framework-engine:6.1.10")
    testImplementation("io.kotest:kotest-runner-junit5:6.1.10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
}

java {
    withSourcesJar()
}

tasks.test {
    javaLauncher = minecraft.getToolchainLauncher(project, 25)
    useJUnitPlatform()
}

/*
 * Example mod
 */

val example: SourceSet = sourceSets.create("example") {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}

dependencies {
    val exampleImplementation = configurations.getByName(example.implementationConfigurationName)
    exampleImplementation(sourceSets.main.get().output)
}

val exampleJar: TaskProvider<Jar> = tasks.register<Jar>("exampleJar") {
    description = "Builds the example mod jar."
    archiveBaseName = "prelude-example"
    archiveClassifier = "dev"
    from(example.output)
}

tasks.runClient {
    classpath(exampleJar)
}

tasks.runServer {
    classpath(exampleJar)
}

val reobfExampleJar: TaskProvider<ReobfuscatedJar> = tasks.named<ReobfuscatedJar>("reobfExampleJar") {
    referenceClasspath.setFrom(example.runtimeClasspath)
}

val copyExampleObfJar: TaskProvider<Copy> = tasks.register<Copy>("copyExampleObfJar") {
    description = "Copies the obfuscated example mod jar to the obfuscated runtime mod directory."
    dependsOn(tasks.prepareObfModsFolder, reobfExampleJar)
    from(reobfExampleJar)
    into(projectDir.resolve("run").resolve("obfuscated").resolve("mods"))
}

tasks.build {
    dependsOn(reobfExampleJar)
}

tasks.runObfClient {
    dependsOn(copyExampleObfJar)
}

tasks.runObfServer {
    dependsOn(copyExampleObfJar)
}
