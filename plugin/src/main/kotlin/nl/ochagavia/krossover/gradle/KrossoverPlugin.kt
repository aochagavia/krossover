package nl.ochagavia.krossover.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.pathString

class KrossoverPlugin : Plugin<Project> {
    fun readPluginVersion(): String {
        val props = Properties()
        KrossoverPlugin::class.java
            .getResourceAsStream("/version.properties")
            ?.use { props.load(it) }
        return props.getProperty("version")
            ?: throw GradleException("plugin version is unknown")
    }

    fun applyInner(
        ext: KrossoverExtension,
        project: Project,
        kspConfigurationName: String?,
        targetName: String?,
    ) {
        project.pluginManager.apply("com.google.devtools.ksp")
        project.pluginManager.withPlugin("com.google.devtools.ksp") {
            val kspConfig = kspConfigurationName ?: "ksp"
            val kspProcessor = "nl.ochagavia.krossover:ksp-processor:${readPluginVersion()}"
            project.dependencies.add(kspConfig, kspProcessor)
            project.logger.info("Added ksp dependency on `$kspProcessor`")

            // Set KSP args
            project.extensions.configure(KspExtension::class.java) { ksp ->
                ksp.arg(
                    "rootClasses",
                    ext.rootClasses.map { it.joinToString(",") },
                )
                ksp.arg(
                    "packages",
                    ext.packages.map { it.joinToString(",") },
                )
                ksp.arg("outputPackageName", ext.outputPackageName)
                ksp.arg("fileName", ext.fileName)
            }

            // Register `GenerateGraalNativeImageConfigTask`
            val jniTask =
                project.tasks.register(
                    "generateJniConfig${targetName ?: ""}",
                    GenerateGraalNativeImageConfigTask::class.java,
                ) {
                    val outPkg = ext.outputPackageName
                    val apiName = ext.fileName

                    val mainSourceSet =
                        if (targetName == null) {
                            "main"
                        } else {
                            val targetNameUncap = targetName.replaceFirstChar { c -> c.lowercase() }
                            "$targetNameUncap/${targetNameUncap}Main"
                        }

                    it.additionalJniClasses.set(ext.additionalJniClasses)
                    it.publicApiMetadataFile.set(
                        project.layout.buildDirectory.file(
                            outPkg.zip(apiName) { pkg, name ->
                                "generated/ksp/$mainSourceSet/resources/$pkg/$name"
                            },
                        ),
                    )
                    it.jniConfigOutputFile.set(
                        project.layout.buildDirectory.file(
                            outPkg.map { pkg -> "generated/ksp/$mainSourceSet/resources/$pkg/jni-config.json" },
                        ),
                    )
                }

            jniTask.configure { task ->
                // There's probably a more robust way to do this, but I've lost way too many hours
                // to gradle
                val kspTasks = project.tasks.matching { p -> p.name.startsWith("ksp") }
                kspTasks.forEach { kspTask ->
                    task.dependsOn(kspTask)
                }
            }

            val bindgenTask =
                project.tasks.register(
                    "generateJniBindings${targetName ?: ""}",
                    GenerateBindingsTask::class.java,
                ) {
                    project.logger.error("krossover: hello from generate config")

                    it.dependsOn(jniTask)

                    val outPkg = ext.outputPackageName
                    val apiName = ext.fileName

                    val mainSourceSet =
                        if (targetName == null) {
                            "main"
                        } else {
                            val targetNameUncap = targetName.replaceFirstChar { c -> c.lowercase() }
                            "$targetNameUncap/${targetNameUncap}Main"
                        }

                    it.publicApiMetadataFile.set(
                        project.layout.buildDirectory.file(
                            outPkg.zip(apiName) { pkg, name ->
                                "generated/ksp/$mainSourceSet/resources/$pkg/$name"
                            },
                        ),
                    )
                    it.pythonFile.set(
                        project.layout.buildDirectory.file(
                            outPkg.map { pkg ->
                                ext.pythonOutputFile
                                    .orElse(Path("generated/ksp/$mainSourceSet/resources/$pkg/main.py"))
                                    .get()
                                    .pathString
                            },
                        ),
                    )
                }
        }
    }

    override fun apply(project: Project) {
        project.logger.info("krossover: applying plugin")
        val ext = project.extensions.create("krossover", KrossoverExtension::class.java)

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.logger.info("krossover: Kotlin/JVM build detected")

            project.pluginManager.apply("com.google.devtools.ksp")
            project.pluginManager.withPlugin("com.google.devtools.ksp") {
                applyInner(ext, project, null, null)
            }
        }

        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            project.logger.info("krossover: Kotlin/Multiplatform build detected")

            val kmp = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            if (kmp == null) {
                project.logger.error("krossover: error, could not find `KotlinMultiplatformExtension.`")
                return@withId
            }

            project.pluginManager.apply("com.google.devtools.ksp")
            project.pluginManager.withPlugin("com.google.devtools.ksp") {
                // There can be multiple JVM targets, so let's configure all of them
                kmp.targets.configureEach { target ->
                    if (target.platformType != KotlinPlatformType.jvm) {
                        project.logger.info("krossover: skipping non-jvm target (`${target.name}`)")
                        return@configureEach
                    }

                    val targetName = target.name.replaceFirstChar { it.uppercase() }
                    val candidates = listOf("ksp$targetName", "kspKotlin$targetName")

                    val configurationName =
                        candidates.firstOrNull { candidateName ->
                            project.configurations.findByName(candidateName) !=
                                null
                        }
                    if (configurationName == null) {
                        project.logger.debug("krossover: skipping non-ksp target (`${target.name}`)")
                    } else {
                        project.logger.info("krossover: apply to `${target.name}`")
                        applyInner(ext, project, configurationName, targetName)
                    }
                }
            }
        }
    }
}
