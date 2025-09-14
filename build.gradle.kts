// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.io.ByteArrayOutputStream

val eeaRepo = "https://github.com/Krantig1999/EMA.git"
val mmaRepo = "https://github.com/Krantig1999/MMA.git"

fun runCmd(cmd: List<String>, ignoreError: Boolean = false): String {
    val output = ByteArrayOutputStream()
    val result = exec {
        commandLine = cmd
        standardOutput = output
        errorOutput = output
        isIgnoreExitValue = ignoreError
    }
    if (result.exitValue != 0 && !ignoreError) {
        throw GradleException("Command failed: ${cmd.joinToString(" ")}\n${output.toString()}")
    }
    return output.toString().trim()
}

tasks.register("syncFromEEA") {
    group = "versioning"
    description = "Fetch latest tag from EEA repo and push to MMA repo"

    doLast {
        println("🔍 Fetching latest tag from EEA repo...")

        // Get all tags
        val tagsOutput = runCmd(listOf("git", "ls-remote", "--tags", eeaRepo))
        if (tagsOutput.isBlank()) {
            throw GradleException("❌ No tags found in EEA repo!")
        }

        // Extract tags
        val tags = tagsOutput
            .lines()
            .filter { it.contains("refs/tags/") }
            .map { it.substringAfter("refs/tags/") }


        if (tags.isEmpty()) {
            throw GradleException("❌ No valid tags found!")
        }

        val latestTag = tags.last()
        println("✅ Latest tag: $latestTag")

        // Get commit hash for latest tag
        val commitHash = runCmd(listOf("git", "ls-remote", eeaRepo, "refs/tags/$latestTag"))
            .split("\t")[0]
        println("🔗 Commit hash: $commitHash")

        // Get current branch name
        val currentBranch = runCmd(listOf("git", "rev-parse", "--abbrev-ref", "HEAD"))
        println("🌿 Current branch: $currentBranch")

        // Add MMA remote if not exists
        //runCmd(listOf("git", "remote", "add", "mma", mmaRepo), ignoreError = true)

        // Fetch the commit from EEA
        runCmd(listOf("git", "fetch", eeaRepo, commitHash))

        // Merge into current branch
        runCmd(listOf("git", "merge", "--no-ff", commitHash))

        // Push current branch to MMA
        runCmd(listOf("git", "push", "mma", currentBranch))

        println("🎉 Synced $latestTag ($commitHash) from EEA → MMA branch $currentBranch")
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
}