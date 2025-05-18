import de.nilsdruyen.gradle.ftp.UploadExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("de.nilsdruyen.gradle-ftp-upload-plugin") version "0.4.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

group = "org.mryd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://maven.citizensnpcs.co/repo")
    maven("https://nexus.scarsz.me/content/groups/public/")
    maven("https://repo.plasmoverse.com/releases")
    maven("https://repo.plasmoverse.com/snapshots")
    maven("https://jitpack.io")
    maven("https://mvn.0110.be/releases")
    maven("https://repo.dmulloy2.net/repository/public/")

}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    compileOnly("su.plo.voice.server:paper:2.1.4")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.denizenscript:denizen:1.3.0-SNAPSHOT")

    // External libraries
    implementation("com.google.code.gson:gson:2.12.1")
    implementation("com.alphacephei:vosk:0.3.45")

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

// ==========================
// Denizen 1.3.0 JAR Task
// ==========================
val denizen130 by configurations.creating

dependencies {
    denizen130("com.denizenscript:denizen:1.3.0-SNAPSHOT")
}

val shadowJar130 = tasks.register<ShadowJar>("shadowJar130") {
    archiveClassifier.set("denizen130")
    configurations = listOf(
        project.configurations.runtimeClasspath.get(),
        denizen130
    )
    from(sourceSets.main.get().output)
    dependencies {
        exclude(dependency("com.denizenscript:denizen"))
    }
}


// ==========================
// Denizen 1.3.1 JAR Task
// ==========================
val denizen131 by configurations.creating

dependencies {
    denizen131("com.denizenscript:denizen:1.3.1-SNAPSHOT")
}

val shadowJar131 = tasks.register<ShadowJar>("shadowJar131") {
    archiveClassifier.set("denizen131")
    configurations = listOf(
        project.configurations.runtimeClasspath.get(),
        denizen131
    )
    from(sourceSets.main.get().output)
    dependencies {
        exclude(dependency("com.denizenscript:denizen"))
    }
}

// Disable default jar
tasks.jar {
    enabled = false
}

// Make `build` depend on both jars
tasks.build {
    dependsOn(shadowJar130, shadowJar131)
}


configure<UploadExtension> {
    host = properties.getOrDefault("ftp.host", "game5.gamely.pro").toString()
    port = properties.getOrDefault("ftp.port", 2023).toString().toInt()
    username = properties.getOrDefault("ftp.username", "a").toString()
    password = properties.getOrDefault("ftp.password", "a").toString()
    sourceDir = "${project.buildDir}/libs"
    targetDir = "/plugins/"
}
