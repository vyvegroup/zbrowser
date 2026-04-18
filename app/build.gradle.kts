import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.zbrowser.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zbrowser.app"
        minSdk = 26 // Android 8.0 Oreo
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        create("release") {
            // Check for noSigning flag
            val noSigning = project.hasProperty("noSigning")
            if (!noSigning) {
                // CI: read from environment variables
                val storeFilePath = System.getenv("KEYSTORE_PATH") ?: ""
                val storePasswordEnv = System.getenv("KEYSTORE_PASSWORD") ?: ""
                val keyAliasEnv = System.getenv("KEY_ALIAS") ?: ""
                val keyPasswordEnv = System.getenv("KEY_PASSWORD") ?: ""

                // Local: read from keystore.properties
                val keystorePropertiesFile = rootProject.file("keystore.properties")
                val localProps = Properties()
                if (keystorePropertiesFile.exists()) {
                    localProps.load(FileInputStream(keystorePropertiesFile))
                }

                storeFile = if (storeFilePath.isNotEmpty()) rootProject.file(storeFilePath)
                            else if (localProps.containsKey("storeFile")) rootProject.file(localProps.getProperty("storeFile"))
                            else rootProject.file("app/release.keystore")
                storePassword = storePasswordEnv.ifEmpty { localProps.getProperty("storePassword") ?: "" }
                keyAlias = keyAliasEnv.ifEmpty { localProps.getProperty("keyAlias") ?: "" }
                keyPassword = keyPasswordEnv.ifEmpty { localProps.getProperty("keyPassword") ?: "" }
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Only use signingConfig when not in noSigning mode
            if (!project.hasProperty("noSigning")) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Material 3
    implementation("com.google.android.material:material:1.11.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // WebView Chromium helper
    implementation("androidx.webkit:webkit:1.9.0")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil for image loading
    implementation("io.coil-kt:coil:2.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
