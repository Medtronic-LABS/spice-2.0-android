# SPICE-Android

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Medtronic-LABS_spice-android&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Medtronic-LABS_spice-android)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Medtronic-LABS_spice-android&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=Medtronic-LABS_spice-android)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Medtronic-LABS_spice-android&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Medtronic-LABS_spice-android)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Medtronic-LABS_spice-android&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=Medtronic-LABS_spice-android)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Medtronic-LABS_spice-android&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=Medtronic-LABS_spice-android)

## Prerequisite
- To bring up the Spice backend server, there are a few prerequisites that need to be completed. Please follow the instructions provided in this [link](https://github.com/Medtronic-LABS/spice-2.0-server.git). Once you have completed the steps, you will get a ***SERVER URL*** to use in our Application.

## Tools used

```sh
Android Studio IDE
```
## Tech stack used

```sh
Kotlin-1.9.0
Java-18
Android MVVM Design Pattern
Gradle-8.2
```

## Installation Steps

#### Download and Install Android Studio
- You can download Android Studio from [here](https://developer.android.com/studio/install)

- Next, check your Java version by running the command ***java -version*** in your terminal.
  ```
  $ java –version
  ```
  If Java 18 is not installed, you can follow these steps:
  * Visit the JDK downloads page using this [link](https://www.oracle.com/java/technologies/javase/jdk18-archive-downloads.html).
  * Install Java 18 according to the provided instructions.
    </br>

- To change the Java version in Android Studio,
  * Open your Android Studio project.
  * Click on "File" in the top menu.
  * Select "Project Structure" from the dropdown menu.
  * In the left panel, select "SDK Location".
  * Under the "JDK Location" section, click on the dropdown menu next to "JDK location" and select the path to the desired Java version.
  * Click "Apply" and then "OK" to save the changes.
  * You may need to re-build your project by selecting "Build" -> "Rebuild Project".</br>

#### Download and Install Git.
To check the Git version, you can run the following command in your terminal:
```sh
git --version
```

If Git is not installed, you can follow the instructions below based on your operating system:
##### Ubuntu:
To install Git on Ubuntu, run the following command in your terminal or click on [Git Official site](https://git-scm.com/download/linux).

```sh
$ sudo apt install git
```
##### Windows:
For Windows, you can visit the [Git Official site](https://git-scm.com/download/win) and download the Git installer.

## Download Source code
Once you have Git installed, you can clone the Spice open source repository by running the following command in your terminal:

```sh
git clone https://github.com/Medtronic-LABS/spice-2.0-android.git
```
</br>
After cloning the repository, you can open the folder in Android Studio.

## Configuration
To execute the application, you must access the ***build.gradle*** file located in the app-level directory of your project.
```
build.gradle[App-level]
```

This file allows you to define your desired values for specific properties.

### Configure Server URL
Use the server URL obtained from the prerequisites mentioned above. Or, you may also follow the steps as mentioned in the [link](https://github.com/Medtronic-LABS/spice-server.git).
```server
    buildConfigField("String", "API_BASE_URL", "\"http://localhost:8762/\"")
````
Substitute ***http://localhost:8762/*** with the server URL obtained.

### Configure Salt key
To enhance security, you may need to provide the salt key used in the backend for user authentication. The salt key is a randomly generated string that adds an extra layer of security when hashing passwords.
```Salt key
    buildConfigField("String", "SALT", "\"spice_opensource\"")
````

By default, the Salt key is set as spice_opensource, but you can modify it if necessary. Please note that the Salt key must match the key used in the backend.
### Complete build.gradle look like this:
```properties
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
                            buildConfigField("String", "API_BASE_URL", "\"http://localhost:8762/\"")
                            buildConfigField("String", "ADMIN_BASE_URL", "\"{ADMIN_URL}\"")
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
                            buildConfigField("String", "API_BASE_URL", "\"http://localhost:8762/\"")
                            buildConfigField("String", "ADMIN_BASE_URL", "\"{ADMIN_URL}\"")
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
```
<font color = "#BA1016">`Synchronize the Gradle files by selecting File > Sync project. After that, you can click on the "Run" button.`</font>

## Optional
#### [Emulator Set up](https://developer.android.com/studio/run/managing-avds)
To install an app on an Android emulator in Android Studio you can follow these steps
- Open the Android Studio and launch the emulator that you want to use
- Click on the "Run" button or press the "Shift" + "F10" keys to open the Run Configuration dialog
- Select the app module that you want to run from the "Module" drop-down list
- Select the emulator that you want to use from the "Device" drop-down list
- Click on the "OK" button to run the app on the selected emulator
- Wait for the app to be installed and launched on the emulator. </br>

**Note:** Before running the app on the emulator, make sure that the emulator is properly configured and running. if not, follow the below instructions:

You can install an Android emulator in Android Studio by following these steps:
- Open Android Studio and click on the "AVD Manager" button in the toolbar or navigate to "Tools" -> "AVD Manager".
- Click on the "Create Virtual Device" button.
- Select a device definition from the list, or click on "New Hardware Profile" to create a custom device definition.
- Choose a system image to run on the emulator, and click "Download" if the image has not been previously downloaded.
- Click "Next", then customize any additional settings you wish to modify, such as the device name or the amount of RAM allocated to the emulator.
- Click "Finish" to create the virtual device.
- Once the virtual device is created, you can start it by selecting it from the AVD Manager and clicking the "Play" button.

After starting the emulator, you can install your app on it by running your project in Android Studio and selecting the emulator as the deployment target.
If you want to run apps on a hardware device,[follow the instructions.](https://developer.android.com/studio/run/device)

[SPICE DOCUMENTATION](https://app.gitbook.com/o/RnePNEThd1XTpW5Hf3HB/s/7inBQ0zjo0nwpqK5625P/~/changes/16/deploy/deployment-guide/android)