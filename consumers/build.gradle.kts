tasks.register("verify") {
    group = "verification"
    dependsOn(":java:run", ":kotlin:run")
}
