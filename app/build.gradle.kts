plugins {
    alias(libs.plugins.android.application)
}

android {
    //namespace = "com.example.radiocommande"
    namespace = "com.radiocommande"
    compileSdk {
           version = release(36) {
            minorApiLevel = 1
        }
    }
    
    

    defaultConfig {
        //applicationId = "com.example.radiocommande"
        applicationId = "com.radiocommande"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.3"

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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.github.mwiede:jsch:0.2.17") // Version moderne et maintenue de JSch
    implementation("com.google.android.material:material:1.11.0")
}