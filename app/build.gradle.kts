import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Credenziali della firma: keystore.properties nella radice del progetto (non versionato)
val keystoreProps = Properties()
val keystoreFile = rootProject.file("keystore.properties")
if (keystoreFile.exists()) keystoreProps.load(keystoreFile.inputStream())

// Impronta dei file di ogni tema (cartella di CompileOnDemand): se cambia, il Theme ricompila quell'app da solo (non serve alzare ASSET_REV)
val assetHashes: String = run {
    val md = MessageDigest.getInstance("MD5")
    val root = file("src/main/assets/CompileOnDemand")
    (root.listFiles() ?: emptyArray()).filter { it.isDirectory }.sortedBy { it.name }.joinToString(";") { dir ->
        md.reset()
        dir.walkTopDown().filter { it.isFile }.sortedBy { it.relativeTo(dir).invariantSeparatorsPath }.forEach { f ->
            md.update(f.relativeTo(dir).invariantSeparatorsPath.toByteArray())
            // fine riga normalizzata: lo stesso file con LF o CRLF da la stessa impronta
            md.update(f.readBytes().filter { it != 13.toByte() }.toByteArray())
        }
        dir.name + "=" + md.digest().joinToString("") { "%02x".format(it) }.take(8)
    }
}

android {
    namespace   = "it.tugaia56.oxydiantheme"
    compileSdk  = 34
    defaultConfig {
        applicationId  = "it.tugaia56.oxydian.theme"
        minSdk         = 31
        targetSdk      = 34
        versionCode    = 13
        versionName    = "1.1.10"
        buildConfigField("int", "MIN_SDK_VERSION", "$minSdk")
        buildConfigField("String", "ASSET_HASHES", "\"$assetHashes\"")
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
