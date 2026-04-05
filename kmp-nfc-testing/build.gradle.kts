plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
}

group = "com.atruedev"
version = providers.environmentVariable("VERSION").getOrElse("0.0.0-local")

kotlin {
    explicitApi()

    android {
        namespace = "com.atruedev.kmpnfc.testing"
        compileSdk =
            libs.versions.androidCompileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            // api: fakes implement interfaces from root module — consumers need those types on classpath
            api(project(":"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

mavenPublishing {
    coordinates("com.atruedev", "kmp-nfc-testing", version.toString())

    pom {
        name.set("kmp-nfc-testing")
        description.set("Test doubles for kmp-nfc")
        url.set("https://github.com/gary-quinn/kmp-nfc")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("gary-quinn")
                name.set("Gary Quinn")
                email.set("gary@atruedev.com")
            }
        }
        scm {
            url.set("https://github.com/gary-quinn/kmp-nfc")
            connection.set("scm:git:git://github.com/gary-quinn/kmp-nfc.git")
            developerConnection.set("scm:git:ssh://github.com/gary-quinn/kmp-nfc.git")
        }
    }
}
