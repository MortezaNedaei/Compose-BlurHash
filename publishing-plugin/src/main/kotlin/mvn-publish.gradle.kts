plugins {
    `maven-publish`
    signing
}

val mavenCentralUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
val mavenSnapshotUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
val snapshotIdentifier = "-SNAPSHOT"

val libVersionName = project.findProperty("VERSION_NAME") as String
val group = project.findProperty("GROUP") as String
val artifactName = project.findProperty("ARTIFACT_NAME") as String

val pomName = project.findProperty("POM_NAME") as String
val pomDescription = project.findProperty("POM_DESCRIPTION") as String
val projectUrl = project.findProperty("POM_URL") as String

val licenseName = project.findProperty("LICENCE_NAME") as String
val licenseUrl = project.findProperty("LICENCE_URL") as String

val developerId = project.findProperty("DEVELOPER_ID") as String
val developerName = project.findProperty("DEVELOPER_NAME") as String

val scmConnection = project.findProperty("SCM_CONNECTION") as String
val scmDevConnection = project.findProperty("SCM_DEV_CONNECTION") as String

val repositoryUsername = project.findProperty("mavenCentralUsername") as? String ?: ""
val repositoryPassword = project.findProperty("mavenCentralPassword") as? String ?: ""

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = group
            artifactId = artifactName
            version = libVersionName
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set(pomName)
                description.set(pomDescription)
                url.set(projectUrl)
                licenses {
                    license {
                        name.set(licenseName)
                        url.set(licenseUrl)
                    }
                }
                developers {
                    developer {
                        id.set(developerId)
                        name.set(developerName)
                    }
                }
                scm {
                    connection.set(scmConnection)
                    developerConnection.set(scmDevConnection)
                    url.set(projectUrl)
                }
            }
        }
        repositories {
            maven {
                credentials {
                    username = repositoryUsername
                    password = repositoryPassword
                }
                url = when {
                    libVersionName.endsWith(snapshotIdentifier) -> {
                        mavenSnapshotUrl
                    }
                    else -> {
                        mavenCentralUrl
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["release"])
}
