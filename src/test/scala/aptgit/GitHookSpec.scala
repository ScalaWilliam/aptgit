package aptgit
import java.nio.file.Paths

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{
  ContainerLink,
  DockerContainer,
  DockerPortMapping,
  VolumeMapping
}
import org.scalatest.Matchers._
import org.scalatest._

class GitHookSpec
    extends FreeSpec
    with DockerTestKit
    with DockerKitSpotify
    with DockerClients {

  private val gitServerDockerImageName =
    "scalawilliam/aptgit-test-server"
  private val httpDumpServerImageName =
    "scalawilliam/aptgit-http-dump-server"
  private val simpleHttpServerImageName =
    "trinitronx/python-simplehttpserver"

  private val httpDumpServerName = "http-dump-server"

  private val httpDumpServer = HttpDumpServer(httpDumpServerContainer,
                                              spotifyDockerClient,
                                              containerManager)

  private val plainGitServer =
    PlainGitServer(gitServerContainer,
                   plainDockerClient,
                   spotifyDockerClient,
                   containerManager)

  private val executeDockerCommand =
    ExecuteDockerCommand(plainDockerClient, containerManager)

  private val gitClient = GitClient(gitClientContainer, executeDockerCommand)

  private val hubUrl = s"http://$httpDumpServerName:9001/notify"

  "Prepare environment" - {
    "1. Prepare Git repository" in {
      executeDockerCommand(gitServerContainer,
                           "/test-setup/prepare-git-repo.sh") should include(
        "Initialized empty Git repository")
    }
    "2. Configure WebSub publisher" in {
      executeDockerCommand(
        gitServerContainer,
        Array("/test-setup/prepare-websub-publish.sh", hubUrl)
      )
    }
  }

  s"Verify that we can execute WebSub hooks against Docker image '${gitServerDockerImageName}'" - {
    "Ensure the HTTP resource can be read" in {
      executeDockerCommand(
        httpDumpServerContainer,
        "wget -O - -q http://simple_http_server:8080/blah.html") should include(
        "never")
    }

    "Set up SSH key" in {
      val publicSshKey = gitClient.createSshKey()
      gitClient.setupSshConfig()
      plainGitServer.addSshKey(publicSshKey)
    }

    "Discover an updated HTML page when a push is made" in {
      val pushResult = gitClient.push()
      withClue(s"Push result was: '${pushResult}'") {
        executeDockerCommand(
          httpDumpServerContainer,
          "wget -O - -q http://simple_http_server:8080/blah.html") should not include ("never")
      }
    }

    "Discover a HUB HTTP POST item for the prior push" in {
      info("This is the WebSub notify POST")
      val logLines = httpDumpServer.httpLines()
      logLines should include("POST /notify")
      logLines should include(
        "hub.url=http%3A%2F%2Fsimple_http_server%3A8080%2Fblah.html&hub.mode=publish")
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

  private lazy val simpleHttpServerContainer =
    DockerContainer(simpleHttpServerImageName,
                    name = Some("simple_http_server"))
      .withVolumes(List(targetVolume2))
      .withPortMapping(8080 -> DockerPortMapping())

  private lazy val httpDumpServerContainer =
    DockerContainer(image = httpDumpServerImageName,
                    name = Some(httpDumpServerName))
      .withLinks(
        ContainerLink(simpleHttpServerContainer, alias = "simple_http_server")
      )

  private lazy val gitServerContainer =
    DockerContainer(gitServerDockerImageName, name = Some("git-server"))
      .withVolumes(List(targetVolume))
      .withLinks(
        ContainerLink(simpleHttpServerContainer, alias = "simple_http_server"),
        ContainerLink(httpDumpServerContainer, alias = httpDumpServerName)
      )

  private lazy val gitClientContainer =
    DockerContainer(gitServerDockerImageName, name = Some("git-client"))
      .withVolumes(GitClient.mappedVolumes)
      .withLinks(
        ContainerLink(gitServerContainer, alias = "git-server")
      )

  override def dockerContainers: List[DockerContainer] = {
    val containers = super.dockerContainers.toBuffer
    containers += simpleHttpServerContainer
    containers += gitServerContainer
    containers += gitClientContainer
    containers += httpDumpServerContainer
    containers.toList
  }

  override def startAllOrFail(): Unit = {

    /** These could take a bit of time **/
    plainGitServer.build() should not be empty
    httpDumpServer.build() should not be empty
    super.startAllOrFail()
  }

}
