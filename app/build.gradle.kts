import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.detekt)
    jacoco
}

android {
    namespace = "net.meshcore.mineralog"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.meshcore.mineralog"
        minSdk = 27
        targetSdk = 35
        versionCode = 30
        versionName = "3.0.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.generateKotlin", "true")
        }

        // Read Google Maps API key from local.properties
        val localProperties = rootProject.file("local.properties")
        val properties = Properties()
        if (localProperties.exists()) {
            properties.load(FileInputStream(localProperties))
        }
        val mapsApiKey = properties.getProperty("MAPS_API_KEY", "")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    signingConfigs {
        // Debug signing (default Android debug keystore)
        getByName("debug") {
            // Uses default debug keystore
        }

        // Release signing with proper keystore
        // Configured via environment variables for security
        // See: https://developer.android.com/studio/publish/app-signing
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            val keystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            val keyPassword = System.getenv("RELEASE_KEY_PASSWORD")

            if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                // Production signing with secure keystore
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword

                // Enable v1, v2, v3, and v4 signing for maximum compatibility and features
                enableV1Signing = true  // Android 4.4-6.0 (Nougat) compatibility
                enableV2Signing = true  // Android 7.0+ (full-APK signature)
                enableV3Signing = true  // Android 9.0+ (key rotation support)
                enableV4Signing = true  // Android 11+ (faster installation)
            } else {
                // Fallback to debug signing for local development
                // WARNING: This is NOT suitable for production releases!
                println("WARNING: Release keystore not configured. Using debug keystore.")
                println("Set RELEASE_KEYSTORE_PATH, RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS, and RELEASE_KEY_PASSWORD")
                storeFile = signingConfigs.getByName("debug").storeFile
                storePassword = signingConfigs.getByName("debug").storePassword
                this.keyAlias = signingConfigs.getByName("debug").keyAlias
                this.keyPassword = signingConfigs.getByName("debug").keyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use release signing configuration
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        // Enable strict lint checks for better code quality
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false // Set to true for even stricter checks

        // Disable specific checks that may be too strict for this project
        disable += setOf(
            "ObsoleteLintCustomCheck",
            "GradleDependency" // Allow older dependency versions
        )
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // ML Kit
    implementation(libs.mlkit.barcode.scanning)

    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)

    // Crypto
    implementation(libs.tink.android)
    implementation(libs.argon2kt)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.security.crypto)

    // QR generation
    implementation(libs.zxing.core)

    // File I/O
    implementation(libs.okio)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Testing
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)

    // Instrumented testing
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}

tasks.withType<Test> {
    // useJUnitPlatform()  // Disabled to support Robolectric tests (JUnit 4)
    ignoreFailures = false
    // Configure test logging
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// JaCoCo configuration for code coverage
jacoco {
    toolVersion = "0.8.12"
}

val jacocoExclusionFilter = listOf(
    // Exclude generated files
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    // Exclude Android framework
    "androidx/**/*.*",
    // Exclude data binding
    "**/*DataBinding*.*",
    "**/*Binding*.*",
    // Exclude DI
    "**/*_Factory.*",
    "**/*_MembersInjector.*",
    "**/Dagger*.*",
    "**/*Module.*",
    "**/*Component.*",
    // Exclude Room generated
    "**/*_Impl.*",
    // Exclude Compose generated
    "**/*\$\$*.*"
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(jacocoExclusionFilter)
    }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.buildDir) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    })
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("testDebugUnitTest")

    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(jacocoExclusionFilter)
    }

    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.buildDir) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    })

    violationRules {
        rule {
            // Global minimum coverage: 60%
            limit {
                minimum = "0.60".toBigDecimal()
            }
        }

        rule {
            // ViewModels should have higher coverage: 70%
            element = "CLASS"
            includes = listOf(
                "net.meshcore.mineralog.ui.screens.*.*.SettingsViewModel",
                "net.meshcore.mineralog.ui.screens.*.*.EditMineralViewModel"
            )
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

// Make build task depend on coverage verification
tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}
