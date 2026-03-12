plugins {
    id("challenge-conventions")
}

application {
    mainClass.set("dev.gregross.challenges.urls.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("gig-urls")
        }
    }
}
