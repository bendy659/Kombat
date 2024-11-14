import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val modId: String by project
val minecraftVersion: String by project
val forgeVersion: String by project
val modVersion: String by project
val shadowLibrary: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

plugins {
    java
    idea
    id("dev.architectury.loom") version "1.7-SNAPSHOT"
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("me.fallenbreath.yamlang") version "1.4.0"
}

java.withSourcesJar()

sourceSets {
    main.get().resources.srcDir(file("src/generated"))
}

group = "modGroupId".fromProperties
version = modVersion

base {
    archivesName = "archivesName".fromProperties
}

java.withSourcesJar()

loom {
    silentMojangMappingsLicense()

    val awFile = project.file("src/main/resources/$modId.accesswidener")
    if (awFile.exists()) accessWidenerPath = awFile

    forge {
        convertAccessWideners = true
        if (loom.accessWidenerPath.isPresent) extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
        mixinConfigs("$modId.mixins.json")
    }
}

repositories {
    mavenCentral()
    maven("https://maven.0mods.team/releases")
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.parchmentmc.org")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    flatDir {
        dir("libs")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${minecraftVersion}:${"parchmentVersion".fromProperties}@zip")
    })
    forge("net.minecraftforge:forge:${minecraftVersion}-${forgeVersion}")

    compileOnly("org.spongepowered:mixin:0.8")
    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")

    // Include libs
    shadowLibrary("team.0mods:KotlinExtras:1.4-noreflect")

    // kotlin runtime & compile
    implementation(kotlin("stdlib", "2.0.10"))
    implementation(minecraftRuntimeLibraries("org.jetbrains.kotlinx:kotlinx-coroutines-core:+")) {}
    implementation(minecraftRuntimeLibraries("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:+")) {}
    implementation(minecraftRuntimeLibraries("org.jetbrains.kotlinx:kotlinx-serialization-core:+")) {}
    implementation(minecraftRuntimeLibraries("org.jetbrains.kotlinx:kotlinx-serialization-json:+")) {}

    // HollowCore
    implementation("ru.hollowhorizon:HollowCore-forge-1.20.1:2.1.2:dev")

    if (file("libs").isDirectory) {
        file("libs").listFiles()?.forEach {
            val splitPos = it.name.lastIndexOf("-")

            if (it.name.endsWith(".jar")) {
                println(it.name)

                val modArtifact = it.name.substring(0, splitPos)
                val modVersion = it.name.substring(splitPos + 1, it.name.length - 4)
                val modReference = "lib:$modArtifact:$modVersion"
                dependencies {
                    modImplementation(project.dependencies.create(modReference) {
                        isTransitive = false
                    })
                }
            }
        }
    } else file("libs").mkdir()
}

tasks {
    jar {
        manifest {
            attributes(mapOf(
                "Specification-Title" to modId,
                "Specification-Vendor" to "modAuthors".fromProperties,
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to version,
                "Implementation-Vendor" to "modAuthors".fromProperties,
                "Implementation-Timestamp" to ZonedDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")),
            ))
        }

        finalizedBy("reobfJar")
    }

    shadowJar {
        configurations = listOf(shadowLibrary)
        archiveClassifier = ""

        val relocateLibs = listOf(
            "org.jetbrains", "com.typesafe", "kotlinx",
            "kotlin", "okio", "org.intellij", "_COROUTINE"
        )

        relocateLibs.forEach {
            relocate(it, "${"modGroupId".fromProperties}.$modId.shadowlibs.$it")
        }
    }

    compileKotlin {
        useDaemonFallbackStrategy = false
        compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
    }

    processResources {
        from(project.sourceSets.main.get().resources)

        val replacement = mapOf(
            "modId".propertiesTo, "modVersion".propertiesTo, "modName".propertiesTo,
            "modCredits".propertiesTo, "modAuthors".propertiesTo,
            "modDesc".propertiesTo, "forgeVersionRange".propertiesTo,
            "minecraftVersionRange".propertiesTo, "loaderVersionRange".propertiesTo,
            "modLicense".propertiesTo, "modGroupId".propertiesTo
        )

        filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta", "*.mixins.json")) {
            expand(replacement)
        }

        inputs.properties(replacement)
    }

    build.get().finalizedBy("shadowJar")

    remapJar {
        entryCompression = ZipEntryCompression.STORED
        inputFile = shadowJar.get().archiveFile
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 17
    }
}

kotlin {
    jvmToolchain(17)
}

yamlang {
    targetSourceSets = listOf(sourceSets.main.get())
    inputDir = "assets/$modId/lang"
}

val String.propertiesTo
    get() = this to this.fromProperties

val String.fromProperties
    get() = project.properties[this].toString()