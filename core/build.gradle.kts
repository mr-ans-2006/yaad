// "core" is plain Kotlin: no Android here. That means we can test the brain of
// Yaad (tokenizer, embeddings, search ranking, tags, key line) on a normal computer.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

dependencies {
    // ONNX Runtime's Java API. On the phone, the app provides the Android build of it.
    compileOnly("com.microsoft.onnxruntime:onnxruntime:1.19.2")

    testImplementation("com.microsoft.onnxruntime:onnxruntime:1.19.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

tasks.test {
    // The tests load the real model from the app's assets folder.
    systemProperty("yaad.assets", rootProject.file("app/src/main/assets").absolutePath)
    testLogging { events("passed", "failed"); showStandardStreams = true }
    // The speed benchmark is slow and machine-dependent, so it only runs with :core:benchmark.
    filter { excludeTestsMatching("*SearchBenchmark") }
}

// ./gradlew :core:benchmark  ->  search speed at 500 / 1,000 / 2,000 screenshots (see SearchBenchmark.kt)
tasks.register<Test>("benchmark") {
    description = "Times hybrid search at 500, 1,000 and 2,000 fake screenshots."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("*SearchBenchmark") }
    systemProperty("yaad.assets", rootProject.file("app/src/main/assets").absolutePath)
    systemProperty("yaad.bench", "true")
    testLogging { showStandardStreams = true }
    outputs.upToDateWhen { false }
}
