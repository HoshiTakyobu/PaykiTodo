import org.gradle.api.GradleException
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val paykiVersionName = "1.16.6"
val paykiVersionCode = 371

val paykiCloudProperties = Properties().apply {
    val file = rootProject.file("user/cloud.properties")
    if (file.isFile) file.inputStream().use(::load)
}
val paykiCloudBaseUrl = paykiCloudProperties.getProperty("baseUrl", "https://paykitodo.invalid")
    .trim()
    .trimEnd('/')
    .takeIf { it.isNotBlank() }
    ?: "https://paykitodo.invalid"

project.setProperty("archivesBaseName", "PaykiTodo-$paykiVersionName")

val releaseKeystorePropertiesFile = listOf(
    rootProject.file("user/keystore.properties"),
    rootProject.file("keystore.properties")
).firstOrNull { it.isFile } ?: rootProject.file("user/keystore.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.isFile) {
        releaseKeystorePropertiesFile.inputStream().use(::load)
    }
}

fun releaseSigningProperty(name: String): String {
    return releaseKeystoreProperties.getProperty(name)?.trim().orEmpty()
}

val releaseStoreFile = releaseSigningProperty("storeFile")
val releaseStorePath = listOf(
    rootProject.file(releaseStoreFile),
    releaseKeystorePropertiesFile.parentFile.resolve(releaseStoreFile)
).firstOrNull { it.isFile }
val releaseSigningConfigured = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword"
).all { key ->
    val value = releaseSigningProperty(key)
    value.isNotBlank() && !value.startsWith("REPLACE_WITH_")
} && releaseStoreFile.isNotBlank() && releaseStorePath != null

val releaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    val simpleTaskName = taskName.substringAfterLast(":")
    simpleTaskName.equals("assemble", ignoreCase = true) ||
        simpleTaskName.equals("bundle", ignoreCase = true) ||
        simpleTaskName.contains("Release", ignoreCase = true)
}

if (releaseTaskRequested && !releaseSigningConfigured) {
    throw GradleException(
        "Release signing is not configured. Fill user/keystore.properties and generate the keystore first. " +
            "Use keystore.properties.example as the safe template; do not commit user/ or release signing material."
    )
}

android {
    namespace = "com.example.todoalarm"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.paykitodo.app"
        minSdk = 26
        targetSdk = 34
        versionCode = paykiVersionCode
        versionName = paykiVersionName
        buildConfigField("String", "PAYKITODO_CLOUD_BASE_URL", "\"${paykiCloudBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = releaseStorePath
                storePassword = releaseSigningProperty("storePassword")
                keyAlias = releaseSigningProperty("keyAlias")
                keyPassword = releaseSigningProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    debugImplementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")
}
