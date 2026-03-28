plugins {
    // Define plugin versions for subprojects (not applied at root)
    alias(libs.plugins.springBoot) apply false
    alias(libs.plugins.springDependencyManagement) apply false
    alias(libs.plugins.ideaExt)
    id("idea")
    id("base")
    alias(libs.plugins.dependencycheck)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.jreleaser)
    wrapper
}

fun Project.secret(name: String): String? =
    (findProperty(name) as String?) ?: System.getenv(name)

val sonarMainBinaries = subprojects.flatMap { subproject ->
    listOf(
        "${subproject.layout.buildDirectory.get()}/classes/java/main",
        "${subproject.layout.buildDirectory.get()}/classes/main"
    )
}.joinToString(",")

val sonarTestBinaries = subprojects.flatMap { subproject ->
    listOf(
        "${subproject.layout.buildDirectory.get()}/classes/java/test"
    )
}.joinToString(",")

group = "net.osgiliath.ai"
description = "Coding Crew ACP framework and release automation parent project"
version = (findProperty("releaseVersion") as String?) ?: "1.0-SNAPSHOT"

tasks.wrapper {
    gradleVersion = "9.4.1"
    distributionType = Wrapper.DistributionType.BIN
}

sonar {
    properties {
        secret("SONAR_HOST_URL")?.let { property("sonar.host.url", it) }
        secret("SONAR_TOKEN")?.let { property("sonar.token", it) }
        secret("SONAR_ORGANIZATION")?.let { property("sonar.organization", it) }
        secret("SONAR_PROJECT_KEY")?.let { property("sonar.projectKey", it) }
        secret("SONAR_PROJECT_NAME")?.let { property("sonar.projectName", it) }
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            subprojects.joinToString(",") { sub ->
                "${sub.layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml"
            }
        )
        property("sonar.java.binaries", sonarMainBinaries)
        property("sonar.java.test.binaries", sonarTestBinaries)
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    pluginManager.apply("java")
    pluginManager.apply("jacoco")

    // Configure Java toolchain explicitly via extension to avoid ordering issues
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    configure<JacocoPluginExtension> {
        toolVersion = rootProject.libs.versions.jacoco.get()
    }

    dependencies {
        "testImplementation"(platform(rootProject.libs.junitBom))
        "testImplementation"(rootProject.libs.junitJupiter)
        "testRuntimeOnly"(rootProject.libs.junitPlatformLauncher)
        "implementation"(platform(rootProject.libs.cucumberBom))
        "implementation"(platform(rootProject.libs.langgraph4jBom))
        "implementation"(platform(rootProject.libs.langchain4jBom))
    }

    tasks.withType<Test>().configureEach {
        // Attach JaCoCo agent to every test task
        configure<JacocoTaskExtension> {
            isEnabled = true
        }
        finalizedBy(tasks.named("jacocoTestReport"))

        useJUnitPlatform()

        // Add detailed test logging for debugging
        testLogging {
            events("passed", "skipped", "failed", "standardOut", "standardError")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }

        // Enable debug output
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
}

jreleaser {
    configFile.set(file("jreleaser.yml"))
}
