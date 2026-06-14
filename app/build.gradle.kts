plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

dependencies {

implementation("androidx.core:core-ktx:1.13.1")

}

android {
   namespace = "com.colorcottage.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.colorcottage.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


kotlin {
    jvmToolchain(17)
}
