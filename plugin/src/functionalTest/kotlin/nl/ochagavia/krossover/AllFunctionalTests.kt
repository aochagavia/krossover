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

        // API
        val expectedApi = tempDir.resolve("build/generated/ksp/main/resources/public-api/api.json")
        assertTrue(Files.exists(expectedApi), "Expected generated file not found: $expectedApi")
        val api = Json.decodeFromString(JsonElement.serializer(), expectedApi.readText(Charsets.UTF_8))
        assertClassInPublicApi(api, "com.example.Dummy")
        assertClassInPublicApi(api, "com.example.Object")
        assertClassInPublicApi(api, "com.example.NestedClass1")

        // JNI Config
        val expectedJniConfig = tempDir.resolve("build/generated/ksp/main/resources/public-api/jni-config.json")
        assertTrue(Files.exists(expectedJniConfig), "Expected generated file not found: $expectedJniConfig")
        val jniConfig = Json.decodeFromString(JsonElement.serializer(), expectedJniConfig.readText(Charsets.UTF_8))
        assertClassInJniConfig(jniConfig, $$"com.example.Dummy$NestedDummy")

        // Python bindings
        val expectedPythonFile = tempDir.resolve("build/generated/ksp/main/resources/public-api/main.py")
        assertTrue(Files.exists(expectedPythonFile), "Expected generated file not found: $expectedJniConfig")
    }

    @Test
    fun dummyMultiplatformBuildProducesExpectedOutput() {
        val tempDir = initializeProject("multiplatform")

        val projectRoot = Path.of(System.getProperty("projectRoot"))

        val gradle =
            GradleRunner
                .create()
                .withProjectDir(tempDir.toFile())
                .withArguments("clean", "generateJniConfigJvm", "--info", "-PprojectRoot=$projectRoot")
                .withPluginClasspath()
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, gradle.task(":generateJniConfigJvm")?.outcome)

        val expected = tempDir.resolve("build/generated/ksp/jvm/jvmMain/resources/public-api/api.json")
        assertTrue(Files.exists(expected), "Expected generated file not found: $expected")

        val api = Json.decodeFromString(JsonElement.serializer(), expected.readText(Charsets.UTF_8))
        assertClassInPublicApi(api, "com.example.Dummy")
        assertClassInPublicApi(api, "com.example.Object")
        assertClassInPublicApi(api, "com.example.NestedClass1")
    }
}
