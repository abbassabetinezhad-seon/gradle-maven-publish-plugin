plugins {
  id("shared")
  id("java-gradle-plugin")
  alias(libs.plugins.buildconfig)
  alias(libs.plugins.android.lint)
}

gradlePlugin {
  plugins {
    create("mavenPublishPlugin") {
      id = "io.seon.gradle.maven.publish"
      implementationClass = "com.vanniktech.maven.publish.MavenPublishPlugin"
      displayName = "Gradle Maven Publish Plugin (SEON Fixed)"
      description = "Fixed version of Gradle Maven Publish Plugin that handles Sonatype service shutdown gracefully"
    }
    create("mavenPublishBasePlugin") {
      id = "io.seon.gradle.maven.publish.base"
      implementationClass = "com.vanniktech.maven.publish.MavenPublishBasePlugin"
      displayName = "Gradle Maven Publish Base Plugin (SEON Fixed)"
      description = "Fixed version of Gradle Maven Publish Base Plugin that handles Sonatype service shutdown gracefully"
    }
  }
}

val integrationTestSourceSet = sourceSets.create("integrationTest") {
  compileClasspath += sourceSets["main"].output
  compileClasspath += configurations.testRuntimeClasspath.get()
  runtimeClasspath += output + compileClasspath
}
val integrationTestImplementation = configurations["integrationTestImplementation"]
  .extendsFrom(configurations.testImplementation.get())

buildConfig {
  packageName("com.vanniktech.maven.publish")
  buildConfigField("String", "NAME", "\"io.seon.gradle.maven.publish\"")
  buildConfigField("String", "VERSION", "\"${project.findProperty("VERSION_NAME") ?: "dev"}\"")

  sourceSets.getByName(integrationTestSourceSet.name) {
    buildConfigField(
      "GRADLE_ALPHA",
      alpha.versions.gradle
        .asProvider()
        .get(),
    )
    buildConfigField(
      "GRADLE_BETA",
      beta.versions.gradle
        .asProvider()
        .get(),
    )
    buildConfigField(
      "GRADLE_RC",
      rc.versions.gradle
        .asProvider()
        .get(),
    )
    buildConfigField(
      "GRADLE_STABLE",
      libs.versions.gradle
        .asProvider()
        .get(),
    )
    buildConfigField(
      "ANDROID_GRADLE_ALPHA",
      alpha.versions.android.gradle
        .get(),
    )
    buildConfigField(
      "ANDROID_GRADLE_BETA",
      beta.versions.android.gradle
        .get(),
    )
    buildConfigField(
      "ANDROID_GRADLE_RC",
      rc.versions.android.gradle
        .get(),
    )
    buildConfigField(
      "ANDROID_GRADLE_STABLE",
      libs.versions.android.gradle
        .get(),
    )
    buildConfigField("KOTLIN_ALPHA", alpha.versions.kotlin.get())
    buildConfigField("KOTLIN_BETA", beta.versions.kotlin.get())
    buildConfigField("KOTLIN_RC", rc.versions.kotlin.get())
    buildConfigField("KOTLIN_STABLE", libs.versions.kotlin.get())
    buildConfigField(
      "GRADLE_PUBLISH_ALPHA",
      alpha.versions.gradle.plugin.publish
        .get(),
    )
    buildConfigField(
      "GRADLE_PUBLISH_BETA",
      beta.versions.gradle.plugin.publish
        .get(),
    )
    buildConfigField(
      "GRADLE_PUBLISH_RC",
      rc.versions.gradle.plugin.publish
        .get(),
    )
    buildConfigField(
      "GRADLE_PUBLISH_STABLE",
      libs.versions.gradle.plugin.publish
        .get(),
    )
  }
}

lint {
  baseline = file("lint-baseline.xml")
  ignoreTestSources = true
  warningsAsErrors = true
}

dependencies {
  api(gradleApi())

  compileOnly(libs.dokka)
  compileOnly(libs.kotlin.plugin)
  compileOnly(libs.android.plugin)

  implementation(projects.centralPortal)
  implementation(projects.nexus)

  testImplementation(gradleTestKit())
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.engine)
  testImplementation(libs.junit.launcher)
  testImplementation(libs.testParameterInjector)
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
  testImplementation(libs.truth.testKit)
  testImplementation(libs.maven.model)

  lintChecks(libs.androidx.gradlePluginLints)
}

tasks.whenTaskAdded {
  if (name.contains("lint") && this::class.java.name.contains("com.android.build")) {
    // TODO: lints can be run on Java 17 or above, remove this once we bump the min Java version to 17.
    enabled = JavaVersion.current() >= JavaVersion.VERSION_17
  }
}

val integrationTest by tasks.registering(Test::class) {
  dependsOn(
    tasks.publishToMavenLocal,
    project(projects.nexus.path).tasks.publishToMavenLocal,
    project(projects.centralPortal.path).tasks.publishToMavenLocal,
  )
  mustRunAfter(tasks.test)

  description = "Runs the integration tests."
  group = "verification"

  testClassesDirs = integrationTestSourceSet.output.classesDirs
  classpath = integrationTestSourceSet.runtimeClasspath

  useJUnitPlatform()
  testLogging.showStandardStreams = true
  maxHeapSize = "2g"
  maxParallelForks = Runtime.getRuntime().availableProcessors()
  jvmArgs(
    "--add-opens",
    "java.base/java.lang.invoke=ALL-UNNAMED",
    "--add-opens",
    "java.base/java.net=ALL-UNNAMED",
    "--add-opens",
    "java.base/java.util=ALL-UNNAMED",
  )

  // We must provide the plugin version here instead of using `withPluginClasspath` for GradleRunner. As there are
  // various AGP / KGP and other plugins tested in the matrix, `withPluginClasspath` will mess the whole classpath.
  systemProperty("com.vanniktech.publish.version", project.property("VERSION_NAME").toString())
  systemProperty("testConfigMethod", System.getProperty("testConfigMethod"))
  systemProperty("quickTest", System.getProperty("quickTest"))

  beforeTest(
    closureOf<TestDescriptor> {
      logger.lifecycle("Running test: ${this.className} ${this.displayName}")
    },
  )
}

val quickIntegrationTest by tasks.registering {
  description = "Runs the integration tests quickly."
  group = "verification"

  dependsOn(integrationTest)
  System.setProperty("quickTest", "true")
}

tasks.check {
  dependsOn(integrationTest)
}

// Configure publishing with proper attribution
plugins.apply("maven-publish")
plugins.apply("signing")

publishing {
  publications {
    withType<MavenPublication>().configureEach {
      // Customize POM with attribution to original project
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
      
      // Disable Gradle module metadata
      suppressAllPomMetadataWarnings()
      
      // Remove the Gradle module metadata artifact
      artifacts.removeIf { it.classifier == null && it.extension == "module" }
    }
  }
}
