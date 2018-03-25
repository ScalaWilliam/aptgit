package aptgit
import java.net.URLEncoder
import java.nio.file.Paths

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{
  ContainerLink,
  DockerContainer,
  DockerPortMapping,
  VolumeMapping
}
import org.scalatest._
import OptionValues._
import Matchers._
import EitherValues._

class GitHookSpec
    extends FreeSpec
    with DockerTestKit
    with DockerKitSpotify
    with DockerClients {

  private val gitServerDockerImageName =
    "scalawilliam/aptgit-test-server"
  private val hubServerImageName =
    "scalawilliam/aptgit-http-dump-server"
  private val topicHttpServerImageName =
    "trinitronx/python-simplehttpserver"

  private val hubServerName = "http-dump-server"

  private val hubServer =
    HubServer(hubServerContainer, spotifyDockerClient, containerManager)

  private val executeDockerCommand =
    ExecuteDockerCommand(plainDockerClient, containerManager)

  private val plainGitServer =
    PlainGitServer(gitServerContainer,
                   executeDockerCommand,
                   plainDockerClient,
                   spotifyDockerClient,
                   containerManager)

  private val gitClient = GitClient(gitClientContainer, executeDockerCommand)

  private val hubPath = "/notify-me"
  private val hubUrl =
    s"http://$hubServerName:${HubServer.ExposedPort}$hubPath"

  private val topicFilename = "index.html"
  private lazy val staticServerName = "static-http-server"

  /**
    *  Topic: https://www.w3.org/TR/websub/#definitions
    */
  private val topicUrl =
    s"http://$staticServerName:${StaticHttpServer.ExposedPort}/$topicFilename"

  private val gitServerTopicFileLocation = s"/target/$topicFilename"
  private var repositoryPath = Option.empty[String]
  private val repositoryName = "sample-repo"

  "Prepare environment" - {
    "1. Prepare Git repository" in {
      repositoryPath =
        Some(plainGitServer.createRepository(repositoryName).right.value)
      info(s"Found repo path: $repositoryPath")
    }
    "2. Configure WebSub publisher" in {
      executeDockerCommand(
        gitServerContainer,
        Array("/test-setup/prepare-websub-publish.sh",
              hubUrl,
              topicUrl,
              gitServerTopicFileLocation,
              repositoryName)
      )
    }
  }

  s"Verify that we can execute WebSub hooks against Docker image '$gitServerDockerImageName'" - {
    "Ensure the HTTP resource can be read" in {
      executeDockerCommand(hubServerContainer, s"wget -O - -q $topicUrl") should include(
        "never")
    }

    "Set up SSH key" in {
      val publicSshKey = gitClient.createSshKey()
      gitClient.setupSshConfig()
      plainGitServer.addSshKey(publicSshKey)
    }

    "Discover an updated HTML page when a push is made" in {
      val pushResult = gitClient.push(repositoryPath.value)
      withClue(s"Push result was: '$pushResult'") {
        executeDockerCommand(hubServerContainer, s"wget -O - -q $topicUrl") should not include ("never")
      }
    }

    "Discover a HUB HTTP POST item for the prior push" in {
      info("This is the WebSub notify POST")
      val logLines = hubServer.httpLines()
      logLines should include(s"POST $hubPath")
      val encodedStaticEndpoint = URLEncoder.encode(topicUrl, "UTF-8")
      logLines should include(
        s"hub.url=$encodedStaticEndpoint&hub.mode=publish")
    }

  }

  private lazy val targetVolume = VolumeMapping(
    host = Paths
      .get("target/docker-env")
      .toAbsolutePath
      .toString,
    container = "/target/",
    rw = true,
  )

  private lazy val targetVolume2 =
    targetVolume.copy(container = "/var/www/")

  private lazy val topicHttpServerContainer =
    DockerContainer(topicHttpServerImageName, name = Some(staticServerName))
      .withVolumes(List(targetVolume2))
      .withPortMapping(StaticHttpServer.ExposedPort -> DockerPortMapping())

  private lazy val hubServerContainer =
    DockerContainer(image = hubServerImageName, name = Some(hubServerName))
      .withLinks(
        ContainerLink(topicHttpServerContainer, alias = staticServerName)
      )

  private lazy val gitServerContainer =
    DockerContainer(gitServerDockerImageName, name = Some("git-server"))
      .withVolumes(List(targetVolume))
      .withLinks(
        ContainerLink(topicHttpServerContainer, alias = staticServerName),
        ContainerLink(hubServerContainer, alias = hubServerName)
      )

  private lazy val gitClientContainer =
    DockerContainer(gitServerDockerImageName, name = Some("git-client"))
      .withVolumes(GitClient.mappedVolumes)
      .withLinks(
        ContainerLink(gitServerContainer, alias = "git-server")
      )

  override def dockerContainers: List[DockerContainer] = {
    val containers = super.dockerContainers.toBuffer
    containers += topicHttpServerContainer
    containers += gitServerContainer
    containers += gitClientContainer
    containers += hubServerContainer
    containers.toList
  }

  override def startAllOrFail(): Unit = {

    /** These could take a bit of time **/
    plainGitServer.build() should not be empty
    hubServer.build() should not be empty
    super.startAllOrFail()
  }

}
