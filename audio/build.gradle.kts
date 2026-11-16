import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The command that makes the Clips: a build tool. The app does not depend on this module, so
// nothing here is packaged in it, and no build or check runs the command: audio is made by hand.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(libs.serialization.json)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

// Switches go in --args: ./gradlew :audio:makeClips --args="--dry-run"
tasks.register<JavaExec>("makeClips") {
    group = "content"
    description = "Makes a Clip for every Polish text the course speaks and removes the Clips nothing speaks."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.polisheasy.audio.MakeClipsMainKt"
    // The command finds content/, audio-voice.json and audio-recordings.json from here.
    workingDir = rootProject.projectDir
}
