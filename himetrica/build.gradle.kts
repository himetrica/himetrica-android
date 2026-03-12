plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    `maven-publish`
    signing
}

android {
    namespace = "com.himetrica.android"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

val libVersion = findProperty("VERSION_NAME")?.toString() ?: "0.1.0"

android.publishing {
    singleVariant("release") {
        withSourcesJar()
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.himetrica.tracker"
                artifactId = "himetrica-android"
                version = libVersion

                pom {
                    name.set("Himetrica Android SDK")
                    description.set("Analytics SDK for Android apps")
                    url.set("https://github.com/himetrica/himetrica-android")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("himetrica")
                            name.set("Himetrica")
                            email.set("hello@himetrica.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/himetrica/himetrica-android.git")
                        developerConnection.set("scm:git:ssh://github.com:himetrica/himetrica-android.git")
                        url.set("https://github.com/himetrica/himetrica-android")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "staging"
                url = uri(layout.buildDirectory.dir("staging-deploy"))
            }
        }
    }

    signing {
        val signingKey = findProperty("SIGNING_KEY")?.toString() ?: System.getenv("SIGNING_KEY")
        val signingPassword = findProperty("SIGNING_PASSWORD")?.toString() ?: System.getenv("SIGNING_PASSWORD")
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["release"])
        }
    }
}

dependencies {
    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Lifecycle (foreground/background detection)
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")

    // Compose (optional - compileOnly so apps without Compose still work)
    compileOnly("androidx.compose.runtime:runtime:1.5.4")
    compileOnly("androidx.compose.ui:ui:1.5.4")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
