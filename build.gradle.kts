plugins {
    id("java")
    id("dev.architectury.loom") version ("0.12.0-SNAPSHOT")
    id("architectury-plugin") version ("3.4-SNAPSHOT")
    kotlin("jvm") version ("1.8.10")
    id("maven-publish")
}

group = "us.timinc.mc.cobblemon.counter"
version = "1.3-fabric-1.5.0"

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    silentMojangMappingsLicense()

    mixin {
        defaultRefmapName.set("mixins.${project.name}.refmap.json")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven("https://maven.impactdev.net/repository/development/")
    maven(uri("https://maven.shedaniel.me/"))
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.2")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.14.14")

    modImplementation("net.fabricmc:fabric-language-kotlin:1.9.3+kotlin.1.8.20")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.75.1+1.19.2")
    modImplementation(fabricApi.module("fabric-command-api-v2", "0.75.1+1.19.2"))
    modImplementation("dev.architectury", "architectury-fabric", "6.5.69")
    modCompileOnly("com.cobblemon:mod:1.3.1+1.19.2-SNAPSHOT")
    modRuntimeOnly("com.cobblemon:fabric:1.3.1+1.19.2-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    modApi("me.shedaniel.cloth:cloth-config-fabric:8.3.103")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "us.timinc.mc.cobblemon.counter"
            artifactId = "cobblemon-counter"
            version = "1.0.1"

            from(components["java"])
        }
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}