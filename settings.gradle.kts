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
        mavenLocal()
        mavenCentral()
        maven("https://repo.spring.io/release")

    }
}

rootProject.name = "parent"

val bridgeDir = file("acp-langraph-langchain-bridge")
if (bridgeDir.exists()) {
    includeBuild(bridgeDir) {
        dependencySubstitution {
            substitute(module("net.osgiliath.ai:acp-langraph-langchain-bridge")).using(project(":"))
        }
    }
}

val agentSdkDir = file("agent-sdk")
if (agentSdkDir.exists()) {
    includeBuild(agentSdkDir) {
        dependencySubstitution {
            substitute(module("net.osgiliath.ai:agent-sdk")).using(project(":"))
        }
    }
}

val agentsCommonDir = file("agents-common")
if (agentsCommonDir.exists()) {
    includeBuild(agentsCommonDir) {
        dependencySubstitution {
            substitute(module("net.osgiliath.ai:agents-common")).using(project(":"))
        }
    }
}
include("codeprompt")

