plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
    implementation("org.graalvm.buildtools:native-gradle-plugin:0.10.6")
}
