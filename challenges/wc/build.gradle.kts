plugins {
    id("challenge-conventions")
}

application {
    mainClass.set("dev.gregross.challenges.wc.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
