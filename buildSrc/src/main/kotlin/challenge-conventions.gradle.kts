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

tasks.register("install") {
    dependsOn("nativeCompile")
    group = "distribution"
    description = "Build native image and symlink to /usr/local/bin"
    doLast {
        val nativeDir = layout.buildDirectory.dir("native/nativeCompile").get().asFile
        val binary = nativeDir.listFiles()?.firstOrNull { it.canExecute() && !it.name.endsWith(".txt") }
            ?: error("No native binary found. Run nativeCompile first.")
        val link = "/usr/local/bin/${binary.name}"
        val result = ProcessBuilder("sudo", "ln", "-sf", binary.absolutePath, link)
            .inheritIO()
            .start()
            .waitFor()
        if (result != 0) error("Failed to symlink ${binary.name} → $link")
        logger.lifecycle("Symlinked ${binary.name} → $link")
    }
}
