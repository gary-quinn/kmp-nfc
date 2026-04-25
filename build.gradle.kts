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

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-opt-in=kotlinx.cinterop.BetaInteropApi")
                    freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
                }
            }
        }
    }

    android {
        namespace = "com.atruedev.kmpnfc"
        compileSdk =
            libs.versions.androidCompileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()

        withHostTestBuilder {}.configure {}
    }

    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "KmpNfc"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(project(":kmp-nfc-testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.androidx.startup)
        }
    }
}

// KGP does not wire consumerProguardFiles for KMP android targets.
// https://youtrack.jetbrains.com/issue/KT-module-proguard (track upstream fix)
run {
    val taskName = "bundleAndroidMainAar"
    val rules = file("src/androidMain/consumer-rules.pro")
    tasks.withType<Zip>().matching { it.name == taskName }.configureEach {
        from(rules) { rename { "proguard.txt" } }
    }
    afterEvaluate {
        check(tasks.findByName(taskName) != null) {
            "Expected task '$taskName' not found - KGP may have renamed it. ProGuard rules will not be bundled."
        }
    }
}

tasks.register("assembleXCFramework") {
    dependsOn(
        "linkReleaseFrameworkIosArm64",
        "linkReleaseFrameworkIosSimulatorArm64",
        "linkReleaseFrameworkIosX64",
    )
    group = "build"
    description = "Assembles KmpNfc.xcframework from iOS release frameworks"

    val outputDir = layout.buildDirectory.dir("XCFrameworks/release")
    val arm64 = layout.buildDirectory.dir("bin/iosArm64/releaseFramework/KmpNfc.framework")
    val simArm64 = layout.buildDirectory.dir("bin/iosSimulatorArm64/releaseFramework/KmpNfc.framework")
    val simX64 = layout.buildDirectory.dir("bin/iosX64/releaseFramework/KmpNfc.framework")
    val fatSim = layout.buildDirectory.dir("bin/iosSimulatorFat/releaseFramework/KmpNfc.framework")

    doLast {
        outputDir.get().asFile.let { dir ->
            dir.deleteRecursively()
            dir.mkdirs()
        }

        val fatDir = fatSim.get().asFile
        fatDir.deleteRecursively()
        simArm64.get().asFile.copyRecursively(fatDir, overwrite = true)

        fun run(vararg args: String) {
            val result = ProcessBuilder(*args).inheritIO().start().waitFor()
            require(result == 0) { "${args.first()} failed with exit code $result" }
        }

        run(
            "lipo",
            "-create",
            File(simArm64.get().asFile, "KmpNfc").absolutePath,
            File(simX64.get().asFile, "KmpNfc").absolutePath,
            "-output",
            File(fatDir, "KmpNfc").absolutePath,
        )

        run(
            "xcodebuild",
            "-create-xcframework",
            "-framework",
            arm64.get().asFile.absolutePath,
            "-framework",
            fatDir.absolutePath,
            "-output",
            File(outputDir.get().asFile, "KmpNfc.xcframework").absolutePath,
        )
    }
}

dokka {
    dokkaPublications.html {
        moduleName.set("kmp-nfc")
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("com.atruedev", "kmp-nfc", version.toString())

    pom {
        name.set("kmp-nfc")
        description.set("Kotlin Multiplatform NFC library for Android, iOS, and JVM")
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
