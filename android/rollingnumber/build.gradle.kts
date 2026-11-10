plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    signing
}

group = "io.github.komodgn"
version = providers.gradleProperty("VERSION_NAME").getOrElse("0.1.0-SNAPSHOT")

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

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
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
    // Public signatures use Modifier, TextStyle, Dp and @Composable, so these are part of the API.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.text)
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

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "rollingnumber"
            afterEvaluate { from(components["release"]) }
            pom {
                name.set("RollingNumber")
                description.set("Jetpack Compose number display that rolls changed digits, for prices in checkout screens.")
                url.set("https://github.com/komodgn/rolling-number")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("komodgn")
                        name.set("komodgn")
                    }
                }
                scm {
                    url.set("https://github.com/komodgn/rolling-number")
                    connection.set("scm:git:https://github.com/komodgn/rolling-number.git")
                    developerConnection.set("scm:git:ssh://git@github.com/komodgn/rolling-number.git")
                }
            }
        }
    }
    repositories {
        // Bundle built here, zipped and uploaded to the Central Portal by .github/workflows/release.yml.
        maven {
            name = "staging"
            url = uri(rootProject.layout.buildDirectory.dir("staging-repo"))
        }
    }
}

signing {
    val key = providers.environmentVariable("SIGNING_KEY")
    val password = providers.environmentVariable("SIGNING_PASSWORD")
    isRequired = key.isPresent
    if (key.isPresent) {
        useInMemoryPgpKeys(key.get(), password.orNull)
        sign(publishing.publications)
    }
}
