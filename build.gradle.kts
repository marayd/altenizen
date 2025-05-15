import de.nilsdruyen.gradle.ftp.UploadExtension


plugins {
    id("java")
//    kotlin("jvm") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("de.nilsdruyen.gradle-ftp-upload-plugin") version "0.4.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}
group = "org.mryd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()

    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }

    maven {
        url = uri("https://maven.citizensnpcs.co/repo")
    }

    maven {
        url = uri("https://nexus.scarsz.me/content/groups/public/")
    }

    maven {
        name = "plasmoverse-releases"
        url = uri("https://repo.plasmoverse.com/releases")
    }

    maven {
        name = "plasmoverse-snapshots"
        url = uri("https://repo.plasmoverse.com/snapshots")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        name = "TarsosDSP repository"
        url = uri("https://mvn.0110.be/releases")
    }

    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }
}


dependencies {
    // Kotlin & Coroutines
//    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // Minecraft-related dependencies
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

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    enabled = false
}

tasks.build {

    dependsOn("uploadFilesToFtp")
}

tasks.uploadFilesToFtp {
    dependsOn("shadowJar")
}

configure<UploadExtension> {
    host = properties.getOrDefault("ftp.host", "game5.gamely.pro").toString()
    port = properties.getOrDefault("ftp.port", 2023).toString().toInt()
    username = properties.getOrDefault("ftp.username", "a").toString()
    password = properties.getOrDefault("ftp.password", "a").toString()
    sourceDir = "${project.buildDir}/libs"
    targetDir = "/plugins/"
}
