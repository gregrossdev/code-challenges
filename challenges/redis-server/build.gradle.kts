plugins {
    id("challenge-conventions")
}

application {
    mainClass.set("dev.gregross.challenges.redis.MainKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("gig-redis")
        }
    }
}
