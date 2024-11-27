plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.attendance"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.example.attendance"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    buildFeatures {
        viewBinding =true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.android.material:material:1.9.0")
    implementation("org.apache.poi:poi:5.2.3")

    // For handling Excel files with XSSF (XLSX format)
    implementation("org.apache.poi:poi-ooxml:5.2.3")
//
    // Optional: For additional POI features like OOXML schemas
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
//
//    // Optional: For working with compressed files
//    implementation("org.apache.commons:commons-compress:1.24.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}