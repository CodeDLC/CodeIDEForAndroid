package com.example.templates

import android.content.Context
import java.io.File

object FabricModTemplateGenerator {

    fun generateFabricModProject(
        parentDir: File,
        modId: String = "example_mod",
        modName: String = "Example Fabric Mod",
        packageName: String = "com.example.mod",
        minecraftVersion: String = "1.21.4"
    ): File {
        val projectDir = File(parentDir, modName.replace(" ", ""))
        projectDir.mkdirs()

        // 1. gradle.properties
        val gradleProperties = """
            # Fabric Properties
            minecraft_version=$minecraftVersion
            yarn_mappings=${minecraftVersion}+build.1
            loader_version=0.16.10

            # Mod Properties
            mod_version=1.0.0
            maven_group=$packageName
            archives_base_name=$modId
        """.trimIndent()
        File(projectDir, "gradle.properties").writeText(gradleProperties)

        // 2. build.gradle.kts
        val buildGradleKts = """
            plugins {
                id("fabric-loom") version "1.9-SNAPSHOT"
                id("maven-publish")
                kotlin("jvm") version "2.1.0"
            }

            version = property("mod_version").toString()
            group = property("maven_group").toString()

            repositories {
                mavenCentral()
                maven { url = uri("https://maven.fabricmc.net/") }
            }

            dependencies {
                minecraft("com.mojang:minecraft:${'$'}{property("minecraft_version")}")
                mappings("net.fabricmc:yarn:${'$'}{property("yarn_mappings")}:v2")
                modImplementation("net.fabricmc:fabric-loader:${'$'}{property("loader_version")}")
                modImplementation("net.fabricmc.fabric-api:fabric-api:0.115.0+$minecraftVersion")
                modImplementation("net.fabricmc:fabric-language-kotlin:1.13.0+kotlin.2.1.0")
            }

            tasks.withType<JavaCompile> {
                options.encoding = "UTF-8"
                options.release.set(21)
            }
        """.trimIndent()
        File(projectDir, "build.gradle.kts").writeText(buildGradleKts)

        // 3. settings.gradle.kts
        val settingsGradleKts = """
            pluginManagement {
                repositories {
                    maven { url = uri("https://maven.fabricmc.net/") }
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            rootProject.name = "$modName"
        """.trimIndent()
        File(projectDir, "settings.gradle.kts").writeText(settingsGradleKts)

        // 4. src/main/resources/fabric.mod.json
        val resourceDir = File(projectDir, "src/main/resources")
        resourceDir.mkdirs()
        val fabricModJson = """
            {
              "schemaVersion": 1,
              "id": "$modId",
              "version": "${'$'}{version}",
              "name": "$modName",
              "description": "Fabric Mod created using Idea Mobile on Android!",
              "authors": ["Developer"],
              "contact": {
                "homepage": "https://fabricmc.net/"
              },
              "license": "CC0-1.0",
              "icon": "assets/$modId/icon.png",
              "environment": "*",
              "entrypoints": {
                "main": [
                  "$packageName.ExampleMod"
                ],
                "client": [
                  "$packageName.ExampleModClient"
                ]
              },
              "mixins": [
                "$modId.mixins.json"
              ],
              "depends": {
                "fabricloader": ">=0.16.0",
                "minecraft": "~$minecraftVersion",
                "java": ">=21",
                "fabric": "*"
              }
            }
        """.trimIndent()
        File(resourceDir, "fabric.mod.json").writeText(fabricModJson)

        // 5. mixins.json
        val mixinJson = """
            {
              "required": true,
              "package": "$packageName.mixin",
              "compatibilityLevel": "JAVA_21",
              "mixins": [
                "ExampleMixin"
              ],
              "injectors": {
                "defaultRequire": 1
              }
            }
        """.trimIndent()
        File(resourceDir, "$modId.mixins.json").writeText(mixinJson)

        // 6. Java source files
        val packagePath = packageName.replace('.', '/')
        val javaSourceDir = File(projectDir, "src/main/java/$packagePath")
        javaSourceDir.mkdirs()

        val mainModClass = """
            package $packageName;

            import net.fabricmc.api.ModInitializer;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            public class ExampleMod implements ModInitializer {
                public static final String MOD_ID = "$modId";
                public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

                @Override
                public void onInitialize() {
                    LOGGER.info("Hello Fabric world from Idea Mobile!");
                    // Register items, blocks, and events here
                }
            }
        """.trimIndent()
        File(javaSourceDir, "ExampleMod.java").writeText(mainModClass)

        val clientModClass = """
            package $packageName;

            import net.fabricmc.api.ClientModInitializer;

            public class ExampleModClient implements ClientModInitializer {
                @Override
                public void onInitializeClient() {
                    ExampleMod.LOGGER.info("Initializing Fabric Client side...");
                }
            }
        """.trimIndent()
        File(javaSourceDir, "ExampleModClient.java").writeText(clientModClass)

        // Mixin example class
        val mixinDir = File(javaSourceDir, "mixin")
        mixinDir.mkdirs()
        val mixinClass = """
            package $packageName.mixin;

            import net.minecraft.client.gui.screen.TitleScreen;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

            @Mixin(TitleScreen.class)
            public class ExampleMixin {
                @Inject(at = @At("HEAD"), method = "init()V")
                private fun init(info: CallbackInfo) {
                    System.out.println("This line is printed by an example mod mixin!");
                }
            }
        """.trimIndent()
        File(mixinDir, "ExampleMixin.java").writeText(mixinClass)

        // 7. Kotlin source file example
        val ktSourceDir = File(projectDir, "src/main/kotlin/$packagePath")
        ktSourceDir.mkdirs()
        val ktClass = """
            package $packageName

            import org.slf4j.LoggerFactory

            object ModItems {
                private val LOGGER = LoggerFactory.getLogger("$modId")

                fun registerModItems() {
                    LOGGER.info("Registering Mod Items for $modName")
                }
            }
        """.trimIndent()
        File(ktSourceDir, "ModItems.kt").writeText(ktClass)

        // 8. Assets folder
        val assetDir = File(resourceDir, "assets/$modId")
        assetDir.mkdirs()
        File(assetDir, "lang/en_us.json").apply {
            parentFile?.mkdirs()
            writeText("""{"item.$modId.example_item": "Example Item"}""")
        }

        return projectDir
    }

    fun ensureSampleProject(context: Context): File {
        val projectsDir = File(context.filesDir, "projects")
        projectsDir.mkdirs()
        val sampleDir = File(projectsDir, "ExampleFabricMod")
        if (!sampleDir.exists()) {
            generateFabricModProject(
                parentDir = projectsDir,
                modId = "example_fabric_mod",
                modName = "ExampleFabricMod",
                packageName = "com.example.fabricmod",
                minecraftVersion = "1.21.4"
            )
        }
        return sampleDir
    }
}
