plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.imagenesvainilla"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.imagenesvainilla"
        minSdk = 24
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
    buildFeatures{
        viewBinding = true
        mlModelBinding = true
    }
}



dependencies {


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // If you want to additionally use the CameraX View class
    implementation("androidx.camera:camera-core:1.1.0")  // Núcleo de CameraX
    implementation("androidx.camera:camera-camera2:1.1.0")  // Implementación de Camera2
    implementation("androidx.camera:camera-lifecycle:1.1.0")  // Ciclo de vida
    implementation("androidx.camera:camera-view:1.0.0")  // Vista de la cámara (si es necesario para la vista previa)
    implementation("com.google.android.material:material:1.4.0")  // Asegúrate de tener esta dependencia
    implementation ("com.google.code.gson:gson:2.10.1")

    implementation(libs.androidx.cardview)

    implementation("androidx.camera:camera-view:1.3.0-alpha06")
    implementation("com.github.jose-jhr:Library-CameraX:1.0.8")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
}