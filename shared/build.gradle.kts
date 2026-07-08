import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    jvm()

    val legadoSharedFramework = XCFramework("LegadoShared")
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LegadoShared"
            isStatic = true
            legadoSharedFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "io.legado.shared"
    compileSdk = rootProject.extra["compile_sdk_version"] as Int

    defaultConfig {
        minSdk = 23
    }
}

tasks.withType<Test>().configureEach {
    if (name == "jvmTest") {
        val kotlinTestClasses = files(layout.buildDirectory.dir("classes/kotlin/jvm/test"))
        testClassesDirs = testClassesDirs + kotlinTestClasses
        classpath += kotlinTestClasses
    }
}
