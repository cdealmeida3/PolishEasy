plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Everything a change must pass before it is merged (docs/adr/0015-the-checks-run-before-the-merge.md).
// GitHub runs it only when started by hand; after a merge it builds and publishes the app alone.
tasks.register("checkBeforeMerge") {
    group = "verification"
    description = "Runs every test and check, then builds the debug and release app."
    dependsOn(
        ":engine:test",
        ":polishcheck:test",
        ":audio:test",
        ":app:testDebugUnitTest",
        ":polishcheck:checkPolish",
        ":app:assembleDebug",
        ":app:assembleRelease",
        ":app:bundleRelease",
    )
}
