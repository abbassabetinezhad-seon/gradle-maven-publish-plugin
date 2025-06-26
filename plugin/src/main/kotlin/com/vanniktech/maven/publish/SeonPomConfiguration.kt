package com.vanniktech.maven.publish

import org.gradle.api.publish.maven.MavenPublication

/**
 * Helper extension to configure POM for SEON's modified version of the plugin
 */
fun MavenPublication.configureSeonPom() {
  pom {
    name.set("SEON Gradle Maven Publish Plugin")
    description.set("Fixed version of Gradle Maven Publish Plugin that handles Sonatype service shutdown gracefully")
    url.set("https://github.com/seon-io/gradle-maven-publish-plugin")
    
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("repo")
      }
    }
    
    developers {
      developer {
        id.set("seon")
        name.set("SEON")
        url.set("https://github.com/seon-io")
      }
      developer {
        id.set("vanniktech")
        name.set("Niklas Baudy")
        url.set("https://github.com/vanniktech")
      }
    }
    
    scm {
      url.set("https://github.com/seon-io/gradle-maven-publish-plugin")
      connection.set("scm:git:git://github.com/seon-io/gradle-maven-publish-plugin.git")
      developerConnection.set("scm:git:ssh://git@github.com/seon-io/gradle-maven-publish-plugin.git")
    }
    
    // Add original project information
    properties.set(mapOf(
      "originalProject" to "https://github.com/vanniktech/gradle-maven-publish-plugin",
      "modifiedBy" to "SEON",
      "modificationPurpose" to "Fix Sonatype service shutdown handling"
    ))
  }
}
