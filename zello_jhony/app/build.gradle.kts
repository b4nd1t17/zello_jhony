plugins {
    id("com.android.application")
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.zello_jhony"
        minSdk = 24
        targetSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Añade esta línea para especificar el namespace
    namespace = "com.example.zello_jhony"
}

dependencies {

    implementation ("javax.json:javax.json-api:1.1.4")
    implementation("com.arthenica:ffmpeg-kit-full:4.4") // Ajusta la versión según las disponibles en la página de GitHub
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.java-websocket:Java-WebSocket:1.5.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
