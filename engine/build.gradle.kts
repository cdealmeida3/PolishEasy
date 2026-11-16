import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    implementation(libs.serialization.json)
    testImplementation(kotlin("test"))
}

val contentDir = rootProject.file("content")
val releasedIds = rootProject.file("released-ids.json")

tasks.test {
    useJUnitPlatform()
    // The real Authored Content is loaded and checked by tests.
    systemProperty("contentDir", contentDir.absolutePath)
    systemProperty("releasedIds", releasedIds.absolutePath)
    inputs.dir(contentDir)
    inputs.file(releasedIds)
}

// The app's build depends on this task, so no APK or bundle is built from broken Authored Content.
val checkContent by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks the Authored Content and fails the build on any problem."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.polisheasy.engine.CheckContentMainKt"
    args(contentDir.absolutePath, releasedIds.absolutePath)
    inputs.dir(contentDir)
    inputs.file(releasedIds)
    val passed = layout.buildDirectory.file("content-check/passed")
    outputs.file(passed)
    doLast { passed.get().asFile.writeText("passed\n") }
}
tasks.check { dependsOn(checkContent) }

tasks.register<JavaExec>("recordContentIds") {
    group = "verification"
    description = "Adds the content's new identifiers to released-ids.json."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.polisheasy.engine.CheckContentMainKt"
    args("--record", contentDir.absolutePath, releasedIds.absolutePath)
}
