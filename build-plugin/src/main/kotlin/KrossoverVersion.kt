import java.io.File

/**
 * Centralized version management for Krossover.
 * Computes version strings that include git commit SHA for snapshot builds.
 *
 * Usage in build.gradle.kts:
 *   val isRelease = project.findProperty("release") == "true"
 *   version = KrossoverVersion.getVersion(rootProject.projectDir, isRelease)
 *
 * Build commands:
 *   Snapshot: ./gradlew build                -> 1.0.5-abc1234-SNAPSHOT
 *   Release:  ./gradlew build -Prelease=true -> 1.0.5
 */
object KrossoverVersion {
    /**
     * Base version number without snapshot suffix.
     * Update this when preparing a new release.
     */
    const val BASE_VERSION = "1.0.5"

    /**
     * Returns the short git commit SHA (7 characters).
     * Falls back to "unknown" if git is not available.
     */
    fun getGitSha(projectDir: File): String {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Returns the version string based on release mode.
     *
     * @param projectDir The project directory for git SHA lookup
     * @param isRelease If true, returns release version (e.g., "1.0.5").
     *                  If false, returns snapshot version (e.g., "1.0.5-abc1234-SNAPSHOT").
     */
    fun getVersion(projectDir: File, isRelease: Boolean): String {
        return if (isRelease) {
            BASE_VERSION
        } else {
            "$BASE_VERSION-${getGitSha(projectDir)}-SNAPSHOT"
        }
    }

    /**
     * Returns the full snapshot version including git SHA.
     * Format: {BASE_VERSION}-{gitSha}-SNAPSHOT
     * Example: 1.0.5-abc1234-SNAPSHOT
     */
    fun getSnapshotVersion(projectDir: File): String = getVersion(projectDir, isRelease = false)

    /**
     * Returns the release version (without SNAPSHOT suffix).
     * Format: {BASE_VERSION}
     */
    fun getReleaseVersion(): String = BASE_VERSION
}