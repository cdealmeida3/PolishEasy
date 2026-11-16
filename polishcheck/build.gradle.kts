import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The Polish check: a build tool. The app does not depend on this module, so nothing here is packaged in it.
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
    implementation(libs.languagetool.pl)
    runtimeOnly(libs.slf4j.nop)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

val contentDir = rootProject.file("content")
val reviews = rootProject.file("polish-reviews.json")
val accepted = rootProject.file("polish-accepted.json")
val reportDir = layout.buildDirectory.dir("polish-check")

tasks.register<JavaExec>("checkPolish") {
    group = "verification"
    description = "Checks the Polish of the Authored Content and fails on any problem that is not accepted."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.polisheasy.polishcheck.CheckPolishMainKt"
    workingDir = rootProject.projectDir
    args(contentDir.path, reviews.path, accepted.path, reportDir.get().asFile.path)
    inputs.dir(contentDir)
    inputs.files(reviews, accepted)
    outputs.dir(reportDir)
}

tasks.register<JavaExec>("recordPolishReview") {
    group = "verification"
    description = "Merges a reviewed file into polish-reviews.json. Pass it with -Previewed=<file>."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.polisheasy.polishcheck.CheckPolishMainKt"
    workingDir = rootProject.projectDir
    val reviewed = providers.gradleProperty("reviewed")
    doFirst {
        require(reviewed.isPresent) { "Say which file was reviewed: -Previewed=<file>" }
        args("--record", rootProject.file(reviewed.get()).path, contentDir.path, reviews.path, accepted.path, reportDir.get().asFile.path)
    }
}
