plugins {
    alias(libs.plugins.android.application)

    // Attiviamo i plugin dicendo a Gradle: "Usa la versione che hai già caricato!"
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.ksp)

    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pol.memento"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pol.memento"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.compose.reorderable)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Librerie per Room Database
    val room_version = "2.7.2"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Libreria per JGit (Sincronizzazione)
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
    // Libreria per Sicurezza (Token Git in EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}