import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

android {
    namespace = "com.jsb.chatapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jsb.chatapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {

            buildConfigField("String", "BASE_URL", "\"https://newsapi.org/v2/\"")
            buildConfigField(
                "String",
                "NEWS_API_KEY",
                "\"${localProperties["NEWS_API_KEY"]}\""
            )
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "BASE_URL", "\"https://newsapi.org/v2/\"")
            buildConfigField(
                "String",
                "NEWS_API_KEY",
                "\"${localProperties["NEWS_API_KEY"]}\""
            )
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
        buildConfig = true
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    //  Dagger Hilt
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.android)
    // Hilt Navigation Compose
    implementation(libs.hilt.navigation.compose)
    //  Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.analytics)
    implementation (libs.play.services.auth)
    implementation (libs.androidx.datastore.preferences)
    implementation(libs.firebase.messaging.ktx)
    implementation (libs.firebase.storage.ktx)
    // Navigation Compose
    implementation(libs.navigation.compose)
    // Coroutines for asynchronous operations
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    // Lifecycle ViewModel Compose
    implementation(libs.lifecycle.viewmodel.compose)
    // Coil for Image
    implementation(libs.coil.compose)
    // Pagination
    implementation (libs.androidx.paging.runtime)
    implementation (libs.androidx.paging.compose)
    // REST API Ktor
    implementation(libs.bundles.ktor)

}