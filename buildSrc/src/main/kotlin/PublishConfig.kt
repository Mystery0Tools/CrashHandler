import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get

/**
 * @author mystery0
 * Create at 2019/12/25
 */
object PublishConfig {
    private const val POM_GROUP_ID = "vip.mystery0.tools"
    private const val POM_ARTIFACT_ID = "crashhandler"
    private const val POM_VERSION = "1.1.5"

    private const val SNAPSHOT_BUILD = true

    private const val PROJECT_NAME = "CrashHandler"
    private const val PROJECT_DESCRIPTION = "异常处理库。"
    private const val PROJECT_URL = "https://github.com/Mystery0Tools/CrashHandler"
    private const val PROJECT_DEVELOPER_ID = "mystery0"
    private const val PROJECT_DEVELOPER_NAME = "Mystery0 M"
    private const val PROJECT_DEVELOPER_EMAIL = "mystery0dyl520@gmail.com"

    private const val NEXUS_URL = "https://nexus3.mystery0.vip/repository"
    private const val NEXUS_USER = ""
    private const val NEXUS_KEY = ""

    private fun getPublishVersion(): String =
        if (isSnapshot()) "$POM_VERSION-SNAPSHOT" else POM_VERSION

    private fun getPublishUri(): String {
        val snapshotsRepoUrl = "${NEXUS_URL}/maven-snapshots/"
        val releasesRepoUrl = "${NEXUS_URL}/maven-releases/"
        return if (isSnapshot()) snapshotsRepoUrl else releasesRepoUrl
    }

    private fun isSnapshot(): Boolean = SNAPSHOT_BUILD

    fun MavenPublication.configPublications() {
        groupId = POM_GROUP_ID
        artifactId = POM_ARTIFACT_ID
        version = getPublishVersion()

        pom {
            name.set(PROJECT_NAME)
            description.set(PROJECT_DESCRIPTION)
            url.set(PROJECT_URL)
            developers {
                developer {
                    id.set(PROJECT_DEVELOPER_ID)
                    name.set(PROJECT_DEVELOPER_NAME)
                    email.set(PROJECT_DEVELOPER_EMAIL)
                }
            }
        }
    }

    fun PublishingExtension.configPublish(project: Project) {
        repositories {
            maven {
                url = project.uri(getPublishUri())
                credentials {
                    username = System.getenv("NEXUS_USER") ?: NEXUS_USER
                    password = System.getenv("NEXUS_KEY") ?: NEXUS_KEY
                }
            }
        }
    }
}