import org.jetbrains.compose.compose
import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
}

group = "me.llarence"
version = "1.0-SNAPSHOT"

kotlin {
    android()
    jvm("desktop") {
        jvmToolchain(11)
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)

                implementation("androidx.compose.ui:ui-graphics:1.4.3")

                implementation("org.slf4j:slf4j-simple:1.7.30")

                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")

                implementation("org.json:json:20230227")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.5.1")
                api("androidx.core:core-ktx:1.9.0")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
        val desktopTest by getting
    }
}

android {
    compileSdkVersion(33)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(33)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
