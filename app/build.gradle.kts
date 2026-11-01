import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("io.github.takahirom.roborazzi")
}

// Signing details are read from keystore.properties, which is NOT in the repo
// (it holds passwords). See docs/MAINTAINING.md for how to make your own.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "io.github.mrans2006.yaad"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.mrans2006.yaad"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
        // The Oppo Reno 6 (and almost every phone from the last 6 years) is arm64.
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                // Opt-in frame recorder (MotionRecordingTest): ./gradlew ... -Pyaad.motion=true
                it.systemProperty("yaad.motion", (project.findProperty("yaad.motion") ?: "false").toString())
            }
        }
    }

    lint {
        // lifecycle 2.9's lint check crashes inside AGP 8.7's lint ("KaCallableMemberCall ... interface was expected").
        // Yaad uses no LiveData, so this check has nothing to look at anyway.
        disable += "NullSafeMutableLiveData"
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        // Compress the native libraries inside the APK: a much smaller download/install file.
        jniLibs { useLegacyPackaging = true }
    }
}

dependencies {
    implementation(project(":core"))

    // Jetpack Compose: the toolkit we draw the whole UI with
    val composeBom = platform("androidx.compose:compose-bom:2026.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.core:core-ktx:1.13.1")

    // Room: the on-phone database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager: runs the "look for new screenshots" job in the background
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ML Kit text recognition, BUNDLED model (works with no internet)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // ONNX Runtime: runs the small search model on the phone
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.2")

    // Coil: loads screenshot thumbnails
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Tests that render the real screens to PNG images on a computer
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.work:work-testing:2.9.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.26.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.26.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
