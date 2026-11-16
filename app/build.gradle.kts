import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Optional release signing: see README ("Signierte Release-APK").
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Optional branding at build time, see README ("Betreiber-Build"). Without these
// properties the build is the neutral "Modulento" app that asks for the address.
fun brandProperty(name: String): String =
    (project.findProperty(name) as String?)?.trim().orEmpty()

val appServerUrl = brandProperty("appServerUrl")
val appLabel = brandProperty("appLabel").ifEmpty { "Modulento" }
val appIdSuffix = brandProperty("appIdSuffix").trimStart('.')
val appIconPath = brandProperty("appIconPath")

if (appIdSuffix.isNotEmpty()) {
    require(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*").matches(appIdSuffix)) {
        "appIdSuffix must consist of lowercase package segments (a-z, 0-9, _), got: $appIdSuffix"
    }
}

/** Escapes a value for a Java string literal (BuildConfig). */
fun javaLiteral(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

/** Escapes a value for an Android string resource (XML escaping is done by AGP). */
fun stringResource(value: String): String {
    val escaped = value.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"")
    return if (escaped.startsWith("@") || escaped.startsWith("?")) "\\" + escaped else escaped
}

// A custom launcher icon replaces drawable/ic_launcher_foreground. It is copied
// into a generated resource folder that the build types layer over "main".
val brandIconRes = layout.buildDirectory.dir("generated/brandIcon/res").get().asFile
val copyBrandIcon = if (appIconPath.isNotEmpty()) {
    val source = file(appIconPath)
    require(source.isFile && source.extension.lowercase() == "png") {
        "appIconPath must point to an existing PNG file, got: $appIconPath"
    }
    tasks.register<Sync>("copyBrandIcon") {
        from(source) { rename { "ic_launcher_foreground.png" } }
        into(brandIconRes.resolve("drawable"))
    }
} else {
    null
}

android {
    namespace = "at.modulento.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "at.modulento.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        if (appIdSuffix.isNotEmpty()) applicationIdSuffix = ".$appIdSuffix"
        buildConfigField("String", "DEFAULT_SERVER_URL", javaLiteral(appServerUrl))
        // The only definition of app_name; it is deliberately not in strings.xml.
        resValue("string", "app_name", stringResource(appLabel))
    }

    if (copyBrandIcon != null) {
        sourceSets.getByName("debug").res.srcDir(brandIconRes)
        sourceSets.getByName("release").res.srcDir(brandIconRes)
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

if (copyBrandIcon != null) {
    tasks.named("preBuild") { dependsOn(copyBrandIcon) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
