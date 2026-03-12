plugins {
    id("challenge-conventions")
}

application {
    mainClass.set("dev.gregross.challenges.calc.MainKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("gig-calc")
        }
    }
}
