plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.komodgn.rollingnumber"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        // Fixture models are shared by JVM tests and device tests.
        getByName("test").kotlin.srcDir("src/testShared/kotlin")
        getByName("androidTest").kotlin.srcDir("src/testShared/kotlin")
        // Device tests read the shared fixtures through AssetManager.
        getByName("androidTest").assets.srcDir(rootProject.file("fixtures"))
    }

    testOptions {
        targetSdk = 36
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                // JVM / Robolectric tests read the shared fixtures straight from the repo.
                test.systemProperty("rollingnumber.fixtures", rootProject.file("fixtures").absolutePath)
            }
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
