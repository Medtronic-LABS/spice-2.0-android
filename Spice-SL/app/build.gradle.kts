import java.util.Properties

val envProperties = Properties()
val envFile = rootProject.file("environment.properties")
if (envFile.exists()) {
    envFile.inputStream().use { envProperties.load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
    id("org.jlleitschuh.gradle.ktlint")
}

android {

    namespace = "org.medtroniclabs.uhis"
    compileSdk = 36
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "org.medtroniclabs.uhis"
        minSdk = 23
        targetSdk = 36
        versionCode = 21
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        missingDimensionStrategy("version", "production")
        resValue("color", "toolbar_color", "#2514BE")

        // MicroCoaching SDK config (flavor-agnostic)
        buildConfigField("String", "HF_TOKEN", "\"${envProperties["HF_TOKEN"] ?: ""}\"")
        buildConfigField(
            "boolean",
            "ENABLE_COACHING_TELEMETRY",
            "${envProperties["ENABLE_COACHING_TELEMETRY"] ?: "false"}",
        )
    }

    // Produce one APK per ABI on every build:
    //   • app-arm64-v8a-<flavor>-<type>.apk → real mobile devices (production ships this)
    //   • app-x86_64-<flavor>-<type>.apk    → Intel/Windows dev emulators
    // Each APK is single-ABI, so neither carries the other's ~70 MB of native libs —
    // the arm64 build stays as lean as the old arm64-only filter. x86 and armeabi-v7a
    // are intentionally excluded. isUniversalApk = false → no extra all-ABI APK.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    // Make exported Room schemas available to tests
    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    lint {
        disable += setOf("NullSafeMutableLiveData")
        // abortOnError = false // <- enable if you need CI to pass while stabilizing
    }

    signingConfigs {
        val prodKeyAlias = envProperties["PROD_JKS_ALIAS"].toString()
        val prodKeyPassword = envProperties["PROD_JKS_KEY_PASSWORD"].toString()
        val prodStorePassword = envProperties["PROD_JKS_STORE_PASSWORD"].toString()

        val stagingKeyAlias = envProperties["STAGE_JKS_ALIAS"].toString()
        val stagingKeyPassword = envProperties["STAGE_JKS_KEY_PASSWORD"].toString()
        val stagingStorePassword = envProperties["STAGE_JKS_STORE_PASSWORD"].toString()

        val nonProdKeyAlias = envProperties["NON_PROD_JKS_ALIAS"].toString()
        val nonProdKeyPassword = envProperties["NON_PROD_JKS_PASSWORD"].toString()
        val nonProdStorePassword = envProperties["NON_PROD_JKS_STORE_PASSWORD"].toString()

        // Signing Key for Prod Release
        create("production") {
            keyAlias = prodKeyAlias
            keyPassword = prodKeyPassword
            storePassword = prodStorePassword
            storeFile = file("uhis_production.jks")
        }
        // Signing Key for Staging Release
        create("staging") {
            keyAlias = stagingKeyAlias
            keyPassword = stagingKeyPassword
            storePassword = stagingStorePassword
            storeFile = file("uhis_staging.jks")
        }
        // Signing key for Dev/QA
        create("nonProd") {
            keyAlias = nonProdKeyAlias
            keyPassword = nonProdKeyPassword
            storePassword = nonProdStorePassword
            storeFile = file("uhis_non_prod.jks")
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("dev") {
            dimension = "version"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "UHIS Dev")
            signingConfig = signingConfigs.getByName("nonProd")
        }
        create("qa") {
            dimension = "version"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "UHIS QA")
            signingConfig = signingConfigs.getByName("nonProd")
        }
        create("staging") {
            dimension = "version"
            applicationIdSuffix = ".staging"
            signingConfig = signingConfigs.getByName("staging")
            resValue("string", "app_name", "UHIS Training")
            resValue("color", "toolbar_color", "#f1b192")
        }
        create("production") {
            dimension = "version"
            signingConfig = signingConfigs.getByName("production")
        }
    }

    androidComponents {
        beforeVariants(selector().withBuildType("debug")) { variantBuilder ->
            // If the flavor is 'production/staging', disable the debug variant
            if (variantBuilder.productFlavors.any { it.second == "production" }) {
                variantBuilder.enable = false
            }
            if (variantBuilder.productFlavors.any { it.second == "staging" }) {
                variantBuilder.enable = false
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationVariants.all {
                when (productFlavors[0].name) {
                    "dev" -> {
                        buildConfigField("String", "API_BASE_URL", "\"${envProperties["UHIS_DEV_API_BASE_URL"]}\"")
                        buildConfigField("String", "ADMIN_BASE_URL", "\"${envProperties["UHIS_DEV_ADMIN_BASE_URL"]}\"")
                        buildConfigField("String", "SALT", "\"${envProperties["UHIS_DEV_SALT_KEY"]}\"")
                        buildConfigField("String", "ROOM_DB_ENCRYPTION_KEY", "\"${envProperties["UHIS_DEV_DB_ENCRYPTION_KEY"]}\"")
                        buildConfigField("String", "COACHING_BACKEND_URL", "\"${envProperties["UHIS_DEV_COACHING_BACKEND_URL"] ?: "http://10.0.2.2:8000/"}\"")
                    }
                    "qa" -> {
                        buildConfigField("String", "API_BASE_URL", "\"${envProperties["UHIS_QA_API_BASE_URL"]}\"")
                        buildConfigField("String", "ADMIN_BASE_URL", "\"${envProperties["UHIS_QA_ADMIN_BASE_URL"]}\"")
                        buildConfigField("String", "SALT", "\"${envProperties["UHIS_QA_SALT_KEY"]}\"")
                        buildConfigField("String", "ROOM_DB_ENCRYPTION_KEY", "\"${envProperties["UHIS_QA_DB_ENCRYPTION_KEY"]}\"")
                        buildConfigField("String", "COACHING_BACKEND_URL", "\"${envProperties["UHIS_QA_COACHING_BACKEND_URL"] ?: "http://10.0.2.2:8000/"}\"")
                    }
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            applicationVariants.all {
                when (productFlavors[0].name) {
                    "dev" -> {
                        buildConfigField("String", "API_BASE_URL", "\"${envProperties["UHIS_DEV_API_BASE_URL"]}\"")
                        buildConfigField("String", "ADMIN_BASE_URL", "\"${envProperties["UHIS_DEV_ADMIN_BASE_URL"]}\"")
                        buildConfigField("String", "SALT", "\"${envProperties["UHIS_DEV_SALT_KEY"]}\"")
                        buildConfigField("String", "ROOM_DB_ENCRYPTION_KEY", "\"${envProperties["UHIS_DEV_DB_ENCRYPTION_KEY"]}\"")
                        buildConfigField("String", "COACHING_BACKEND_URL", "\"${envProperties["UHIS_DEV_COACHING_BACKEND_URL"] ?: "http://10.0.2.2:8000/"}\"")
                    }
                    "qa" -> {
                        buildConfigField("String", "API_BASE_URL", "\"${envProperties["UHIS_QA_API_BASE_URL"]}\"")
                        buildConfigField("String", "ADMIN_BASE_URL", "\"${envProperties["UHIS_QA_ADMIN_BASE_URL"]}\"")
                        buildConfigField("String", "SALT", "\"${envProperties["UHIS_QA_SALT_KEY"]}\"")
                        buildConfigField("String", "ROOM_DB_ENCRYPTION_KEY", "\"${envProperties["UHIS_QA_DB_ENCRYPTION_KEY"]}\"")
                        buildConfigField("String", "COACHING_BACKEND_URL", "\"${envProperties["UHIS_QA_COACHING_BACKEND_URL"] ?: "http://10.0.2.2:8000/"}\"")
                    }
                    "staging" -> {
                        buildConfigField("String", "API_BASE_URL", "\"${envProperties["UHIS_STAGE_API_BASE_URL"]}\"")
                        buildConfigField("String", "ADMIN_BASE_URL", "\"${envProperties["UHIS_STAGE_ADMIN_BASE_URL"]}\"")
                        buildConfigField("String", "SALT", "\"${envProperties["UHIS_STAGE_SALT_KEY"]}\"")
                        buildConfigField("String", "ROOM_DB_ENCRYPTION_KEY", "\"${envProperties["UHIS_STAGE_DB_ENCRYPTION_KEY"]}\"")
                        buildConfigField("String", "COACHING_BACKEND_URL", "\"${envProperties["UHIS_STAGE_COACHING_BACKEND_URL"] ?: "http://10.0.2.2:8000/"}\"")
                    }
                    "production" -> {
                        buildConfigField("String", "API_BASE_URL", "\"${envProperties["UHIS_PROD_API_BASE_URL"]}\"")
                        buildConfigField("String", "ADMIN_BASE_URL", "\"${envProperties["UHIS_PROD_ADMIN_BASE_URL"]}\"")
                        buildConfigField("String", "SALT", "\"${envProperties["UHIS_PROD_SALT_KEY"]}\"")
                        buildConfigField("String", "ROOM_DB_ENCRYPTION_KEY", "\"${envProperties["UHIS_PROD_DB_ENCRYPTION_KEY"]}\"")
                        buildConfigField("String", "COACHING_BACKEND_URL", "\"${envProperties["UHIS_PROD_COACHING_BACKEND_URL"] ?: "http://10.0.2.2:8000/"}\"")
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
        compose = true
    }
    // Kotlin 2.x: the Compose compiler is provided by the kotlin.plugin.compose plugin
    // applied above; no manual kotlinCompilerExtensionVersion required.

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    bundle {
        // Disabling splitting the APK based on language
        // as we have dynamic switching of language on runtime
        language {
            enableSplit = false
        }
    }
}

/** Export Room schemas via KAPT (no Room plugin needed) */
kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        // arg("room.incremental", "true")
        // arg("room.expandProjection", "true")
    }
}

dependencies {

    implementation(project(":analytics"))

    // MicroCoaching SDK (sourced from mavenLocal — see ../micro-coaching-android-sdk)
    implementation("com.medtroniclabs.microcoaching:sdk-android:0.5.2-SNAPSHOT")
    // Optional offline-Bengali STT engine. Bundles sherpa-onnx (~30 MB)
    // and provides SherpaOnnxStt.factory which the Builder consumes below.
    implementation("com.medtroniclabs.microcoaching:sdk-android-sherpa:0.4.0-SNAPSHOT")

    // Provide Lifecycle lint to avoid detector crashes
    // lintChecks("androidx.lifecycle:lifecycle-runtime-lint:2.8.6")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.hilt:hilt-common:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // Use only the KTX artifact and align to latest you used elsewhere
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Dagger-Hilt
    implementation("com.google.dagger:hilt-android:2.56")
    kapt("com.google.dagger:hilt-compiler:2.56")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")

    // Coroutines & Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Flexbox Layout
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Lottie
    implementation("com.airbnb.android:lottie:6.5.0")

    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("joda-time:joda-time:2.12.5")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Security
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    // WorkManager
    implementation("androidx.work:work-runtime:2.9.1")

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Pagination
    implementation("androidx.paging:paging-runtime-ktx:3.3.2")

    // SQLCipher & SQLite KTX
    implementation("net.zetetic:sqlcipher-android:4.10.0@aar")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // In-app update
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // NCD
    implementation("io.github.mrherintsoahasina:flextools:1.0.3") {
        exclude(group = "com.google.android", module = "flexbox")
    }

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Loading progress
    implementation("com.github.ybq:Android-SpinKit:1.4.0")

    // Compose BOM + libs
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.compose.runtime:runtime-livedata")

    implementation("com.google.accompanist:accompanist-themeadapter-material3:0.28.0")

    // Debug / Test for Compose
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test")
}
