package nl.ochagavia.krossover

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AllFunctionalTests {
    fun initializeProject(projectName: String): Path {
        val tempDir = Path.of(System.getProperty("e2e.baseDir")).resolve(projectName)
        tempDir.toFile().deleteRecursively()

        val fixtureRoot = Path.of("src", "functionalTest", "resources", "projects", projectName)
        fixtureRoot.toFile().copyRecursively(tempDir.toFile(), overwrite = true)

        return tempDir
    }

    fun assertClassInPublicApi(
        json: JsonElement,
        className: String,
    ) {
        val classes = json.jsonObject["classes"]
        assertNotNull(classes)

        assertNotNull(classes.jsonObject[className], "$className not exported")
    }

    fun assertClassInJniConfig(
        json: JsonElement,
        className: String,
    ) {
        val classPresent =
            json.jsonArray.any {
                className == it.jsonObject["name"]?.jsonPrimitive?.contentOrNull
            }
        assertTrue(classPresent, "$className not present in JNI config")
    }

    fun assertExpectedFilesPresent(projectDir: Path, classesInPublicApi: List<String>, classesInJniConfig: List<String>) {
        // API
        val expectedApi = projectDir.resolve("build/kotlin/krossover/metadata/api.json")
        assertTrue(Files.exists(expectedApi), "Expected generated file not found: $expectedApi")
        val api = Json.decodeFromString(JsonElement.serializer(), expectedApi.readText(Charsets.UTF_8))
        classesInPublicApi.forEach {
            assertClassInPublicApi(api, it)
        }

        // JNI Config
        val expectedJniConfig = projectDir.resolve("build/kotlin/krossover/metadata/jni-config.json")
        assertTrue(Files.exists(expectedJniConfig), "Expected generated file not found: $expectedJniConfig")
        val jniConfig = Json.decodeFromString(JsonElement.serializer(), expectedJniConfig.readText(Charsets.UTF_8))
        classesInJniConfig.forEach {
            assertClassInJniConfig(jniConfig, it)
        }

        // JNI headers
        val expectedJniHeaders = projectDir.resolve("build/jni/jni_simplified.h")
        assertTrue(Files.exists(expectedJniHeaders), "Expected generated file not found: $expectedJniHeaders")

        // Bindings
        val expectedFiles = listOf(
            projectDir.resolve("build/python/__init__.py"),
            projectDir.resolve("build/rust/mod.rs"),
        )
        expectedFiles.forEach {
            assertTrue(Files.exists(it), "Expected generated file not found: $it")
        }
    }

    fun assertPythonNullCheckForNonNullableOnly(projectDir: Path) {
        val pythonFile = projectDir.resolve("build/python/__init__.py")
        val content = pythonFile.readText(Charsets.UTF_8)

        // Non-nullable param should have null check
        assertTrue(
            content.contains("`required` cannot be None"),
            "Expected null check for non-nullable param 'required'"
        )

        // Nullable param should NOT have null check
        assertTrue(
            !content.contains("`optional` cannot be None"),
            "Nullable param 'optional' should not have null check"
        )
    }

    @Test
    fun dummyBuildProducesExpectedOutput() {
        val tempDir = initializeProject("dummy")

        val projectRoot = Path.of(System.getProperty("projectRoot"))

        val gradle =
            GradleRunner
                .create()
                .withProjectDir(tempDir.toFile())
                .withArguments("clean", "generateJniConfig", "generateJniBindings", "--info", "-PprojectRoot=$projectRoot")
                .withPluginClasspath()
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, gradle.task(":generateJniConfig")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, gradle.task(":generateJniBindings")?.outcome)

        val classesInPublicApi = listOf(
            "com.example.Dummy",
            "com.example.Object",
            "com.example.NestedClass1",
        )
        val classesInJniConfig = listOf(
            $$"com.example.Dummy$NestedDummy",
            "java.util.ArrayList",
        )
        assertExpectedFilesPresent(tempDir, classesInPublicApi, classesInJniConfig)
        assertPythonNullCheckForNonNullableOnly(tempDir)
    }

    @Test
    fun dummyMultiplatformBuildProducesExpectedOutput() {
        val tempDir = initializeProject("multiplatform")

        val projectRoot = Path.of(System.getProperty("projectRoot"))

        val gradle =
            GradleRunner
                .create()
                .withProjectDir(tempDir.toFile())
                .withArguments("clean", "generateJniConfigJvm", "generateJniBindingsJvm", "--info", "-PprojectRoot=$projectRoot")
                .withPluginClasspath()
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, gradle.task(":generateJniConfigJvm")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, gradle.task(":generateJniBindingsJvm")?.outcome)

        val classesInPublicApi = listOf(
            "com.example.Dummy",
            "com.example.Object",
            "com.example.NestedClass1",
        )
        val classesInJniConfig = listOf(
            $$"com.example.DoublyNestedClass",
            "java.util.ArrayList",
        )
        assertExpectedFilesPresent(tempDir, classesInPublicApi, classesInJniConfig)
    }
}
