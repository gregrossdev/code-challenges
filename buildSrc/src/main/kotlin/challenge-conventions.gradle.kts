plugins {
    id("org.jetbrains.kotlin.jvm")
    application
    id("org.graalvm.buildtools.native")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            buildArgs.add("-O2")
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(24))
                vendor.set(JvmVendorSpec.ORACLE)
            })
        }
    }
}
