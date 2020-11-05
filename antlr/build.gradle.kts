@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

plugins {
  `java-library`
  antlr
  id("com.github.johnrengelman.shadow") version "6.1.0"
  `maven-publish`
  signing
  groovy
}

repositories {
  jcenter()
}

group = "com.autonomousapps"
val antlrVersion: String by rootProject.extra // e.g., 4.8
val internalAntlrVersion: String by rootProject.extra // e.g., 4.8.0
val VERSION_GRAMMAR: String by project
version = VERSION_GRAMMAR

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8

  withJavadocJar()
  withSourcesJar()
}

// https://docs.gradle.org/current/userguide/antlr_plugin.html
// https://discuss.gradle.org/t/using-gradle-2-10s-antlr-plugin-to-import-an-antlr-4-lexer-grammar-into-another-grammar/14970/6
tasks.generateGrammarSource {
  /*
   * Ignore implied package structure for .g4 files and instead use this for all generated source.
   */
  val pkg = "com.autonomousapps.internal.grammar"
  val dir = pkg.replace(".", "/")
  outputDirectory = file("$buildDir/generated-src/antlr/main/$dir")
  arguments = arguments + listOf(
    // Specify the package declaration for generated Java source
    "-package", pkg,
    // Specify that generated Java source should go into the outputDirectory, regardless of package structure
    "-Xexact-output-dir",
    // Specify the location of "libs"; i.e., for grammars composed of multiple files
    "-lib", "src/main/antlr/$dir"
  )
}

dependencies {
  antlr("org.antlr:antlr4:$antlrVersion")
  implementation("org.antlr:antlr4-runtime:$antlrVersion")

  testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
    exclude(module = "groovy-all")
    because("For Spock tests")
  }
  testImplementation("com.google.truth:truth:1.0.1") {
    because("Groovy's == behavior on Comparable classes is beyond stupid")
  }
}

// Publish with `./gradlew antlr:publishShadowPublicationToMavenRepository`
publishing {
  publications {
    create<MavenPublication>("shadow") {
      groupId = "com.autonomousapps"
      artifactId = "simple-grammar"
//      version = internalAntlrVersion

      from(components["java"])
//      project.shadow.component(this)

      configurePom(pom)
      signing.sign(this)

      versionMapping {
        usage("java-api") {
          fromResolutionOf("runtimeClasspath")
        }
        usage("java-runtime") {
          fromResolutionResult()
        }
      }
    }
  }
  repositories {
    maven {
      url = uri("$buildDir/repo")
    }
  }
}

fun configurePom(pom: org.gradle.api.publish.maven.MavenPom) {
  pom.apply {
    name.set("Java/Kotlin Simple Grammar")
    description.set("A simple grammar for Java and Kotlin source")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2020")
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    developers {
      developer {
        id.set("autonomousapps")
        name.set("Tony Robalik")
      }
    }
    scm {
      connection.set("scm:git:git://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git")
      developerConnection.set("scm:git:ssh://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git")
      url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    }
  }
}

tasks.withType<Sign>().configureEach {
//  onlyIf {
//    val isNotSnapshot = !internalAntlrVersion.endsWith("SNAPSHOT")
//    isNotSnapshot && gradle.taskGraph.hasTask(publishToMavenCentral.get())
//  }
  doFirst {
    logger.quiet("Signing v$internalAntlrVersion")
  }
}

//jar.enabled = false

tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
  target = tasks.shadowJar.get()
}

tasks.shadowJar {
  dependsOn(tasks.getByName("relocateShadowJar"))
  archiveClassifier.set("")
  relocate("org.antlr", "com.autonomousapps.internal.antlr")
}
