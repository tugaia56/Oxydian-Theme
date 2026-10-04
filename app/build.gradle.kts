plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace   = "it.tugaia56.oxydiantheme"
    compileSdk  = 34
    defaultConfig {
        applicationId  = "it.tugaia56.oxydian.theme"
        minSdk         = 31
        targetSdk      = 34
        versionCode    = 1
        versionName    = "0.1.0"
        buildConfigField("int", "MIN_SDK_VERSION", "$minSdk")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { buildConfig = true }

    // aapt2 e zipalign sono .so estratti su disco: servono come binari eseguibili
    packaging {
        jniLibs.excludes += setOf("/META-INF/*", "/META-INF/versions/**", "/org/bouncycastle/**")
        resources.excludes += setOf("/META-INF/*", "/META-INF/versions/**", "/org/bouncycastle/**")
        jniLibs.useLegacyPackaging = true
    }

    applicationVariants.all {
        val variant = this
        val suffix = if (variant.name == "release") "-${variant.versionName}" else ""
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "OxydianTheme-${variant.name}$suffix.apk"
        }
    }
}

dependencies {
    implementation(libs.android.appcompat)
    implementation(libs.android.material)
    implementation(libs.android.recyclerview)
    implementation(libs.libsu.core)
    implementation(libs.bcpkix)
}
