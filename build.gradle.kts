plugins {
    id("java")
    id("java-base")
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


group = "de.timesnake"
version = "2.0.0"
var projectId = 19

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://git.timesnake.de/api/v4/groups/7/-/packages/maven")
        name = "timesnake"
        credentials(PasswordCredentials::class)
    }
}

dependencies {
    compileOnly("de.timesnake:database-proxy:4.+")
    compileOnly("de.timesnake:database-api:4.+")

    compileOnly("de.timesnake:channel-proxy:5.+")
    compileOnly("de.timesnake:channel-api:5.+")

    implementation("de.timesnake:library-network:2.+")
    implementation("de.timesnake:library-commands:2.+")
    implementation("de.timesnake:library-permissions:2.+")
    implementation("de.timesnake:library-basic:2.+")
    implementation("de.timesnake:library-chat:2.+")

    compileOnly("org.apache.logging.log4j:log4j-api:2.22.1")
    compileOnly("org.apache.logging.log4j:log4j-core:2.22.1")

    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    compileOnly("commons-io:commons-io:2.11.0")
    compileOnly("org.freemarker:freemarker:2.3.31")
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        if (project.parent != null) {
            substitute(module("de.timesnake:database-proxy")).using(project(":database:database-proxy"))
            substitute(module("de.timesnake:database-api")).using(project(":database:database-api"))

            substitute(module("de.timesnake:channel-proxy")).using(project(":channel:channel-proxy"))
            substitute(module("de.timesnake:channel-api")).using(project(":channel:channel-api"))

            substitute(module("de.timesnake:library-network")).using(project(":libraries:library-network"))
            substitute(module("de.timesnake:library-commands")).using(project(":libraries:library-commands"))
            substitute(module("de.timesnake:library-permissions")).using(project(":libraries:library-permissions"))
            substitute(module("de.timesnake:library-basic")).using(project(":libraries:library-basic"))
            substitute(module("de.timesnake:library-chat")).using(project(":libraries:library-chat"))
        }
    }
}


tasks.register<Copy>("exportAsPlugin") {
    from(layout.buildDirectory.file("libs/${project.name}-${project.version}-all.jar"))
    into(findProperty("timesnakePluginDir") ?: "")

    dependsOn("shadowJar")
}

tasks.withType<PublishToMavenRepository> {
    dependsOn("shadowJar")
}

publishing {
    repositories {
        maven {
            url = uri("https://git.timesnake.de/api/v4/projects/$projectId/packages/maven")
            name = "timesnake"
            credentials(PasswordCredentials::class)
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }

    processResources {
        inputs.property("version", project.version)

        filesMatching("plugin.yml") {
            expand(mapOf(Pair("version", project.version)))
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}