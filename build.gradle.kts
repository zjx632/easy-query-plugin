plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellijPlatform {
    pluginConfiguration {
        name = "com.easy-query"
        version = "0.0.74"
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2024.3")

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.database")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }

    implementation("cn.hutool:hutool-core:5.8.25")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.41")
}
