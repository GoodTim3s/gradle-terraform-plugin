import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.text.SimpleDateFormat
import java.util.Date

abstract class VersioningExtension {
    abstract val version: Property<String>
}

plugins {
    groovy
    kotlin("jvm") version libs.versions.kotlin
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.grgit)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    // bump to groovy 4 when supported, or use kotlin (hard atm)
    testImplementation("org.spockframework:spock-core:2.4-M6-groovy-3.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks
    .withType<KotlinJvmCompile>()
    .configureEach {
        compilerOptions
            .jvmTarget
            .set(JvmTarget.JVM_17)
    }

fun Map<String, Any?>.getOrSystemEnvOrDefault(
    key: String,
    defaultValue: String,
): String =
    this.getOrElse(key) {
        System.getenv().getOrElse(key) {
            logger.warn("'$key' property is not defined, defaulting to '$defaultValue'")
            defaultValue
        }
    }.toString()

val githubUsername = project.properties.getOrSystemEnvOrDefault("GH_USERNAME", "jordanst3wart")
/*
 * this is a github classic token that requires publishing permissions to write a new package
 */
val githubToken = project.properties.getOrSystemEnvOrDefault("GH_PACKAGES_TOKEN", "dummyToken")

fun String.formatBranchName(): String {
    // Defines the regex pattern for special characters
    val specialCharsRegex = Regex("[@!#\$%^&*()\\[\\]{}|\\\\/:;\"'<>, ]")
    return this.replace(specialCharsRegex, "-").uppercase()
}

val defaultBranches = listOf("main", "master")
fun createVersion(): Provider<String> {
    val gitShortHash = grgit.head().abbreviatedId
    val dateTag = SimpleDateFormat("yyyyMMdd").format(Date())
    return if (grgit.branch.current().name in defaultBranches) {
        provider { "$dateTag-$gitShortHash" }
    } else {
        provider { "${grgit.branch.current().name.formatBranchName()}-SNAPSHOT" }
    }
}

val extension = extensions.create("versioning", VersioningExtension::class.java)
extension.version.set(createVersion())

val versioning = extensions.getByType<VersioningExtension>()
tasks.register("printVersion") {
    group = "versioning"
    description = "Prints the calculated project version for use outside of gradle, i.e. CICD pipelines/docker image.\n" +
            "Run with `./gradlew :build-plugin:plugin:printVersion -q`"
    doLast {
        println(versioning.version.get())
    }
}

// create a gradle plugin uploading to gradle plugins website
gradlePlugin {
    plugins {
        create("terraformPlugin") {
            id = "org.ysb33r.terraform" // property("ID").toString()
            implementationClass = "org.ysb33r.gradle.terraform.plugins.TerraformPlugin"
            version = createVersion().get()
            displayName = "Terraform Plugin"
            description =
                """
                Provides Terraform extension and tasks. No need to have terraform installed as plugin will take care of
                caching and installation in a similar fashion as to have Gradle distributions are cached
                """.trimIndent()
            tags = listOf("terraform")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            credentials {
                username = githubUsername
                password = githubToken
            }
            url = uri("https://maven.pkg.github.com/jordanst3wart/gradle-terraform-plugin")
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED")
}