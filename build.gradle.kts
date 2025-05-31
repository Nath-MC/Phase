// hey! DO NOT MODIFY THIS FILE I GAVE MY SOUL TO FIX THE ISSUE

plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
    id("com.gradleup.shadow") version "9.0.0-beta11"
}

version = project.property("version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.

    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("phase") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    accessWidenerPath = file("src/main/resources/phase.accesswidener")
}

dependencies {
    minecraft("net.minecraft:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabricApi_version")}")
    modImplementation("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    // Api modules bundled with the mod
    val apiModules = setOf(
        "fabric-api-base",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1",
        "fabric-key-binding-api-v1",
        "fabric-screen-api-v1"
    )

    apiModules.forEach { module ->
        include(
            fabricApi.module(
                module,
                project.property("fabricApi_version") as String
            )
        ) //include is sufficient for fabric dependencies
    }

    val reflections = "org.reflections:reflections:${project.property("reflections_version")}"
    implementation(reflections)
    shadow(reflections)
}


tasks {
    processResources {
        val properties = mapOf(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version")
        )

        inputs.properties(properties)
        filesMatching("fabric.mod.json") {
            expand(properties)
        }
    }

    jar {
        val name = project.base.archivesName.get()
        from("LICENSE") {
            rename { "${it}_${name}" }
        }
    }

    shadowJar {
        from(sourceSets["main"].output)
        from(sourceSets["client"].output)
        configurations = listOf(project.configurations.shadow.get())

        archiveClassifier.set("dev")

        val name = project.base.archivesName.get()
        from("LICENSE") {
            rename { "${it}_${name}" }
        }

        dependencies {
            exclude {
                it.moduleGroup == "org.slf4j"
            }
        }
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveClassifier.set("") // This becomes the main artifact
    }

    jar {
        enabled = false
    }

    withType<JavaCompile>().configureEach {
        options.release.set(21)
    }
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Make sure the remapped jar is published instead of the regular jar
artifacts {
    archives(tasks.remapJar)
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String

            // Publish the remapped jar (which contains everything)
            artifact(tasks.remapJar)
            artifact(tasks.remapSourcesJar)
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}