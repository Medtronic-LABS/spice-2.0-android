plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
}

android {
    namespace = "com.medtroniclabs.opensource"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.medtroniclabs.opensource"
        minSdk = 23
        targetSdk = 34
        versionCode = 6
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "version"
    productFlavors {
        create("africa") {
            dimension = "version"
            versionName = "2.0.0"
            versionCode = 1
        }
    }

    buildTypes {

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationVariants.all {
                if (buildType.name == "release") {
                    when (productFlavors[0].name) {
                        "africa" -> {
                            buildConfigField("String", "API_BASE_URL", "\"http://localhost/\"")
                            buildConfigField("String", "ADMIN_BASE_URL", "\"http://localhost/\"")
                            buildConfigField("String", "SALT", "\"spice_opensource\"")
                            buildConfigField("String", "DB_PASSWORD", "\"OpenSource\"")
                            resValue("string", "spice_app_name", "SPICE")
                        }
                    }
                }
            }
        }

        debug {
            isDebuggable = true
            applicationIdSuffix = ".dev"
            applicationVariants.all {
                if (buildType.name == "debug") {
                    when (productFlavors[0].name) {
                        "africa" -> {
                            buildConfigField("String", "API_BASE_URL", "\"http://localhost/\"")
                            buildConfigField("String", "ADMIN_BASE_URL", "\"http://localhost/\"")
                            buildConfigField("String", "SALT", "\"spice_opensource\"")
                            buildConfigField("String", "DB_PASSWORD", "\"OpenSource\"")
                            resValue("string", "spice_app_name", "SPICE DEV")
                        }
                    }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"
    }
}

dependencies {

    implementation(project(":analytics"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.hilt:hilt-common:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    implementation("androidx.activity:activity:1.9.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    //Dagger-Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    //Room database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    // To use Kotlin annotation processing tool (kapt)
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    //Kotlin coroutine dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")

    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    //Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    //Flexbox Layout
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    //Lottie
    implementation("com.airbnb.android:lottie:6.5.0")

    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("joda-time:joda-time:2.12.5")

    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    //Swipe Refresh Layout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("androidx.core:core-splashscreen:1.0.1")

    // Security hashing
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    //Workmanager
    implementation("androidx.work:work-runtime:2.9.1")

    //Local date
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Pagination
    implementation("androidx.paging:paging-runtime-ktx:3.3.2")

    implementation("net.zetetic:sqlcipher-android:4.6.0@aar")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    //in-app update
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // NCD
    implementation("com.github.jeffreyliu8:FlexBoxRadioGroup:0.0.8") {
        exclude(group = "com.google.android", module = "flexbox")
    }

    // Graph
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // loading progress
    implementation("com.github.ybq:Android-SpinKit:1.4.0")

    //compose
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.foundation:foundation")

    // Core Compose libraries
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("com.google.accompanist:accompanist-themeadapter-material3:0.28.0")

    // Debug dependencies
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Testing dependencies
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test")

}