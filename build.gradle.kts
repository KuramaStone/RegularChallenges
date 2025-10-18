import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("fabric-loom") version "1.9-SNAPSHOT"
    id("maven-publish")
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String
val modid = project.property("modid") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

loom {
    accessWidenerPath = file("src/main/resources/regularchallenges.accesswidener")

    splitEnvironmentSourceSets()

    mods {
        register(modid) {
            sourceSet("main")
            sourceSet("client")
        }
    }
}

val shade by configurations.creating {
    isTransitive = false // Prevents unnecessary transitive dependencies
}

// Extend implementation to include shaded dependencies
configurations.implementation.get().extendsFrom(shade)

val modShade by configurations.creating {
    isTransitive = false // Prevents unnecessary transitive dependencies
}

// Extend implementation to include shaded dependencies
configurations.modImplementation.get().extendsFrom(modShade)

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven("https://maven.impactdev.net/repository/development/")
    maven(url = "https://maven.fabricmc.net/")
    maven(url = "https://maven.enginehub.org/repo/")
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
    maven ("https://oss.sonatype.org/content/repositories/snapshots/")
    maven ("https://repo.kyori.net/repository/maven-public/")

    maven("https://maven.blazing-coop.net/releases")
    maven("https://repo.kyori.net/snapshots")
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("net.minecraft:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    val fabricModules = listOf("fabric-lifecycle-events-v1", "fabric-command-api-v2", "fabric-networking-api-v1", "fabric-events-interaction-v0")
    fabricModules.forEach {
        modImplementation(fabricApi.module(it, "${project.property("fabric_version")}"))
    }
    include(modImplementation("me.lucko:fabric-permissions-api:0.3.1")!!)

    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
    modImplementation("com.cobblemon:fabric:1.6.1+1.21.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    // commands
    shade("io.github.revxrsal:lamp.common:4.0.0-rc.9")
    modShade("io.github.revxrsal:lamp.fabric:4.0.0-rc.9")
    shade("io.github.revxrsal:lamp.brigadier:4.0.0-rc.9")

    // yaml
    implementation(include("dev.dejvokep:boosted-yaml:1.3.6")!!)

    // kyori
    shade("net.kyori:adventure-text-minimessage:4.18.0")
    shade("net.kyori:adventure-api:4.18.0")
    shade("net.kyori:examination-api:1.3.0")
    shade("net.kyori:adventure-key:4.18.0")
    shade("net.kyori:adventure-text-serializer-plain:4.18.0")
    shade("net.kyori:adventure-text-serializer-gson:4.18.0")
    shade("net.kyori:adventure-text-serializer-json:4.18.0")
    shade("net.kyori:option:1.1.0")

    modImplementation(include("eu.pb4:placeholder-api:2.4.2+1.21")!!)

}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(project.properties)
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.withType<JavaCompile> {
    // Preserve parameter names in the bytecode
    options.compilerArgs.add("-parameters")
}

// optional: if you're using Kotlin
tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        javaParameters = true
    }
}

tasks.shadowJar {
    configurations = listOf(shade, modShade)
//    relocate("dev.dejvokep.boostedyaml", "com.github.kuramastone.$modid.shade.dev.dejvokep.boostedyaml")
    relocate("revxrsal", "com.github.kuramastone.$modid.shade.revxrsal")
    relocate("net.kyori", "com.github.kuramastone.$modid.shade.net.kyori")
    archiveClassifier.set("shaded")
}

tasks.remapJar {
    dependsOn(tasks.shadowJar) // Ensure shadowJar runs first
    inputFile.set(tasks.shadowJar.get().archiveFile) // Use the shadowed JAR for remapping
    archiveFileName.set("${project.name}-${project.version}.jar") // Rename output
//    destinationDirectory = file("run/mods")
}

// configure the maven publication
tasks.named("publishToMavenLocal") {
    dependsOn(tasks.remapJar)
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString() // ✅ required
            artifactId = project.property("modid").toString()
            version = project.version.toString() // ✅ required
            artifact(tasks.remapJar.get()) {
                builtBy(tasks.remapJar)
            }

            // Optionally publish sources
            artifact(tasks.named("sourcesJar").get())
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        mavenLocal()
    }
}