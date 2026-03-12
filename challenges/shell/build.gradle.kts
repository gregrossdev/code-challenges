plugins {
    id("challenge-conventions")
}

application {
    mainClass.set("dev.gregross.challenges.shell.MainKt")
}

dependencies {
    implementation("org.jline:jline:3.28.0")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("gig-sh")
        }
    }
}
