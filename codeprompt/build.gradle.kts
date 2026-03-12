import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    `java-library`
    alias(libs.plugins.kotlinJvm)
    id("maven-publish")
    jacoco
}

group = "net.osgiliath.ai"
description = "Code Prompt Framework: ACP-compatible coding assistant framework powered by LangChain4j"
version = (findProperty("releaseVersion") as String?) ?: "1.0-SNAPSHOT"

ext {
    set("junit-jupiter.version", libs.versions.junit.get())
    set("commonmark.version", libs.versions.commonmark.get())
}


configure<JacocoPluginExtension> {
    toolVersion = libs.versions.jacoco.get()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}


tasks.withType<Test>().configureEach {
    // Ensure all test tasks are JUnit Platform based and produce JaCoCo reports.
    configure<JacocoTaskExtension> {
        isEnabled = true
    }
    finalizedBy(tasks.named("jacocoTestReport"))

    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    systemProperty("java.util.logging.config.file", "")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.withType<Test>())
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

springBoot {
    mainClass.set("net.osgiliath.codeprompt.CodePromptFrameworkApplication")
}

configurations.all {
    resolutionStrategy {
        val junitVersion = libs.versions.junit.get()
        val junitPlatformVersion = libs.versions.junitPlatform.get()
        force(libs.kotlinStdlib)
        force(libs.kotlinStdlibCommon)
        force(libs.kotlinxCoroutinesCore)
        force(libs.kotlinxCoroutinesCoreJvm)
        force("org.junit.platform:junit-platform-engine:$junitPlatformVersion")
        force("org.junit.platform:junit-platform-commons:$junitPlatformVersion")
        force("org.junit.platform:junit-platform-suite:$junitPlatformVersion")
        force("org.junit.platform:junit-platform-suite-api:$junitPlatformVersion")
        force("org.junit.platform:junit-platform-suite-engine:$junitPlatformVersion")
        force("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
        force("org.junit.jupiter:junit-jupiter:$junitVersion")
        force("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        force("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    }
}

dependencies {
    implementation(platform(libs.cucumberBom))
    implementation(platform(libs.langgraph4jBom))
    implementation(platform(libs.langchain4jBom))

    val sdkVersion = (findProperty("sdkVersion") as String?)
        ?: System.getenv("SDK_VERSION")
        ?: System.getenv("AGENT_SDK_VERSION")
        ?: "1.0-SNAPSHOT"
    val bridgeVersion = (findProperty("bridgeVersion") as String?) ?: System.getenv("BRIDGE_VERSION")
    ?: libs.versions.bridgeDefault.get()
    val commonmarkVersion = libs.versions.commonmark.get()

    implementation("net.osgiliath.ai:agent-sdk:$sdkVersion")
    implementation("net.osgiliath.ai:acp-langraph-langchain-bridge:$bridgeVersion")
    implementation(libs.acp)

    implementation(libs.kotlinStdlib)
    implementation(libs.kotlinStdlibCommon)
    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.kotlinxCoroutinesCoreJvm)

    implementation("dev.langchain4j:langchain4j")
    implementation("dev.langchain4j:langchain4j-spring-boot-starter")
    implementation("dev.langchain4j:langchain4j-open-ai-spring-boot-starter")
    implementation("dev.langchain4j:langchain4j-http-client-jdk")
    implementation("dev.langchain4j:langchain4j-mcp")
    implementation("dev.langchain4j:langchain4j-document-parser-markdown")

    implementation("org.bsc.langgraph4j:langgraph4j-core")
    implementation("org.bsc.langgraph4j:langgraph4j-langchain4j")

    implementation("org.commonmark:commonmark:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-task-list-items:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-yaml-front-matter:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-autolink:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-tables:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-ins:$commonmarkVersion")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.platform")
    }

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testImplementation(libs.awaitility)

    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("dev.langchain4j:langchain4j-open-ai-official")
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    standardInput = System.`in`
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    // This module is published as a library and should not resolve runtimeClasspath via bootJar in CI.
    enabled = false
    classpath = files()
}

tasks.named<Jar>("jar") {
    enabled = true
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("codeprompt")
                description.set(project.description ?: "Code Prompt Framework")
                url.set("https://github.com/OsgiliathEnterprise/coding-crew")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("charliemordant")
                        name.set("Charlie Mordant")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/OsgiliathEnterprise/coding-crew.git")
                    developerConnection.set("scm:git:ssh://git@github.com/OsgiliathEnterprise/coding-crew.git")
                    url.set("https://github.com/OsgiliathEnterprise/coding-crew")
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}
