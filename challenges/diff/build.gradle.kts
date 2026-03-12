plugins {
    id("challenge-conventions")
}

application {
    mainClass.set("dev.gregross.challenges.diff.MainKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("gig-diff")
        }
    }
}
