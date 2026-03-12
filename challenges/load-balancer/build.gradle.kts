plugins {
    id("challenge-conventions")
}

application {
    mainClass.set("dev.gregross.challenges.lb.MainKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("gig-lb")
        }
    }
}
