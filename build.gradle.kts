// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

// Global JDK image transformation disable
allprojects {
    tasks.matching { it.name.contains("JdkImageTransform") }.configureEach { enabled = false }
    tasks.matching { it.name.contains("androidJdkImage") }.configureEach { enabled = false }
    tasks.matching { it.name.contains("JdkImage") }.configureEach { enabled = false }
}

tasks.register("clean", Delete::class) {
    delete(layout.projectDirectory.dir("build"))
}
