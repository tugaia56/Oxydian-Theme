import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Credenziali della firma: keystore.properties nella radice del progetto (non versionato)
val keystoreProps = Properties()
val keystoreFile = rootProject.file("keystore.properties")
if (keystoreFile.exists()) keystoreProps.load(keystoreFile.inputStream())

android {
    namespace   = "it.tugaia56.oxydiantheme"
    compileSdk  = 34
    defaultConfig {
        applicationId  = "it.tugaia56.oxydian.theme"
        minSdk         = 31
        targetSdk      = 34
        versionCode    = 6
        versionName    = "1.1.3"
        buildConfigField("int", "MIN_SDK_VERSION", "$minSdk")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { buildConfig = true }

    signingConfigs {
        create("release") {
            if (keystoreFile.exists()) {
                storeFile     = file(keystoreProps["storeFile"]     as String)
                storePassword = keystoreProps["storePassword"]      as String
                keyAlias      = keystoreProps["keyAlias"]           as String
                keyPassword   = keystoreProps["keyPassword"]        as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig   = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

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
