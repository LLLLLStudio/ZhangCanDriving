plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zhangcan.driving"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zhangcan.driving"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        resourceConfigurations += listOf("zh")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // 娱乐项目，直接用 debug 签名，保证 APK 可安装
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module")
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        // targetSdk 与依赖版本按 PRD 锁定，不跟随提示升级
        disable += setOf("OldTargetApi", "GradleDependency", "UnusedResources")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
