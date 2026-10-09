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
        versionName = (project.findProperty("releaseVersionName") as String?) ?: "1.0-MVP"

        testInstrumentationRunner = "com.martinsterentjevs.cacheit.test.CacheItHiltTestRunner"
    }

    signingConfigs {
        create("demo") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                val debugConfig = signingConfigs.getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
        }
    }

    buildTypes {
        debug{
            buildConfigField("String", "SERVER_BASE_URL", "\"https://cacheit-staging.smokywastaken.id.lv\"")
        }
        release {
            buildConfigField("String", "SERVER_BASE_URL", "\"https://cacheit.smokywastaken.id.lv\"")
            optimization {
                enable = false
            }
        }
        create("demo") {
            applicationIdSuffix=".demo"
            versionNameSuffix = ".demo"
            isDebuggable = false
            signingConfig = signingConfigs.getByName("demo")
            buildConfigField("String","SERVER_BASE_URL", "\"https://cacheit-demo.smokywastaken.id.lv\"")
        }
        create("staging") {
            applicationIdSuffix = ".staging"
            versionNameSuffix = ".staging"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "SERVER_BASE_URL", "\"https://cacheit-staging.smokywastaken.id.lv\"")
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
}

dependencies {
    implementation(libs.compose.markdown)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
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
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.uiautomator)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.bouncycastle)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockk)
    implementation(libs.retrofit)

    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    // TEMPORARY
    implementation(libs.androidx.compose.icons.extended)
}