import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            suppressWarnings.set(true)
        }
    }
}
