plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}


android {
    namespace = "com.martinsterentjevs.cacheit"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.martinsterentjevs.cacheit"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug{
            buildConfigField("String", "SERVER_BASE_URL", "\"http://192.168.8.133:9001/\"")
        }
        release {
            buildConfigField("String", "SERVER_BASE_URL", "\"http://10.0.2.2:8080/\"")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    buildToolsVersion = "36.1.0"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx) // Flow/coroutine support for DAO methods
    ksp(libs.room.compiler)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.bouncycastle)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.mockito.core)

    implementation(libs.retrofit)

    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    // TEMPORARY
    implementation(libs.androidx.compose.icons.extended)
}