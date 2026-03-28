pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.spring.io/release")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.spring.io/release")
    }
}

rootProject.name = "parent"

// Gradle 9.x can fail when writing verification metadata across composite included builds.
val writingVerificationMetadata = runCatching {
    val method = gradle.startParameter.javaClass.methods.firstOrNull {
        it.name == "getWriteDependencyVerifications" && it.parameterCount == 0
    }
    val value = method?.invoke(gradle.startParameter)
    (value as? Collection<*>)?.isNotEmpty() == true
}.getOrDefault(false)

val bridgeDir = file("acp-langraph-langchain-bridge")
if (!writingVerificationMetadata && bridgeDir.exists()) {
    includeBuild(bridgeDir) {
        dependencySubstitution {
            substitute(module("net.osgiliath.ai:acp-langraph-langchain-bridge")).using(project(":"))
        }
    }
}

val agentSdkDir = file("agent-sdk")
if (!writingVerificationMetadata && agentSdkDir.exists()) {
    includeBuild(agentSdkDir) {
        dependencySubstitution {
            substitute(module("net.osgiliath.ai:agent-sdk")).using(project(":"))
        }
    }
}

val agentsCommonDir = file("agents-common")
if (!writingVerificationMetadata && agentsCommonDir.exists()) {
    includeBuild(agentsCommonDir) {
        dependencySubstitution {
            substitute(module("net.osgiliath.ai:agents-common")).using(project(":"))
        }
    }
}
include("codeprompt")

