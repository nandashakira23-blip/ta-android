plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.fleur.attendance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fleur.attendance"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Production VPS (public IP, nginx :80)
            buildConfigField("String", "API_BASE_URL", "\"http://202.155.13.195/\"")
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            // Production VPS (public IP, nginx :80)
            buildConfigField("String", "API_BASE_URL", "\"http://202.155.13.195/\"")
            isMinifyEnabled = true
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
        viewBinding = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Security (for EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Networking — okhttp BOM menyamakan versi okhttp + okio + logging-interceptor
    // (fix crash "java.lang.IllegalStateException: closed" di HttpLoggingInterceptor pada Android 15)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    
    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.14.2")
    
    // Image Cropping
    implementation("com.github.yalantis:ucrop:2.2.8")
    
    // Camera
    implementation("androidx.camera:camera-camera2:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")
    
    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
