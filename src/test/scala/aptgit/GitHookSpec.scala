package aptgit
import java.nio.file.Paths

import com.spotify.docker.client.DockerClient.LogsParam
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
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

import scala.concurrent.Await
import scala.concurrent.duration._

class GitHookSpec extends FreeSpec with DockerTestKit with DockerKitSpotify {

  private val gitDockerImageName = "test-server"
//  private val gitDockerImageName = "jkarlos/git-server-docker"
  private val simpleHttpServerImageName = "trinitronx/python-simplehttpserver"

  s"Verify that we can execute Git hooks against Docker image '${gitDockerImageName}'" - {

    info(
      """
           |Using test-driven development, we can safely iterate to the full solution
           |The set-up was a little painful but dividends will pay off massively...
           |When we begin adding complexity to the system
           |For example, we will want to do user permissions and will be required to verify whether a push fails or not
           |But we'd rather not live in a test-free world where we have no idea what is going on.
           |I want this to work!
           |""".stripMargin)

    "Prepare environment" - {
      "1. Configure Git" in {
        executeCommand("/test-setup/configure-git.sh") shouldBe empty
      }
      "2. Prepare SSH key" in {
        executeCommand("/test-setup/prepare-ssh-key.sh") should include(
          "public key has been saved")
      }
      "3. Prepare Git repository" in {
        executeCommand("/test-setup/prepare-git-repo.sh") should include(
          "Initialized empty Git repository")
      }
    }

    "Receive a Hook when a push is made" in {
      executeCommand("/test-setup/clone-and-push.sh") should include(
        "Received!!")
    }
  }

  s"Verify that we can execute WebSub hooks against Docker image '${gitDockerImageName}'" - {

    "Prepare environment" - {
      "1. Configure WebSub publisher" in {
        executeCommand("/test-setup/prepare-websub-publish.sh")
      }
      "2. Ensure the static HTTP server is running" in {
        executeCommand("wget -O - -q http://simple_http_server:8080/blah.html") should include(
          "never")
      }
    }

    "Discover an updated HTML page when a push is made" in {
      val pushResult = executeCommand("/test-setup/clone-and-push.sh")
      withClue(s"Push result was: ${pushResult}") {
        executeCommand("wget -O - -q http://simple_http_server:8080/blah.html") should not include ("never")
      }
    }

    "Discover a HUB HTTP POST item" in {
      info("This is the WebSub notify POST")
      val dockerContainerState =
        containerManager.getContainerState(simpleHttpServerContainer)
      val id = Await.result(dockerContainerState.id, 5.seconds)
      val logStream = spotifyDockerClient.logs(id,
                                               LogsParam.stderr(),
                                               LogsParam.stdout(),
                                               LogsParam.tail(5))
      logStream.readFully() should include("POST /notify")
    }

  }

  private val testSetupVolume = VolumeMapping(
    host = Paths.get("test-setup").toAbsolutePath.toString,
    container = "/test-setup/",
    rw = false,
  )

  private val targetVolume = VolumeMapping(
    host = Paths.get("target/docker-env").toAbsolutePath.toString,
    container = "/target/",
    rw = true,
  )

  private val targetVolume2 = VolumeMapping(
    host = Paths.get("target/docker-env").toAbsolutePath.toString,
    container = "/var/www/",
    rw = true,
  )

  private val simpleHttpServerContainer =
    DockerContainer(simpleHttpServerImageName,
                    name = Some("simple_http_server"))
      .withVolumes(List(targetVolume2))
      .withPortMapping(8080 -> DockerPortMapping())

  private val gitServerContainer = DockerContainer(gitDockerImageName)
    .withVolumes(List(testSetupVolume, targetVolume))
    .withLinks(
      ContainerLink(simpleHttpServerContainer, alias = "simple_http_server"))

  override def dockerContainers: List[DockerContainer] = {
    val containers = super.dockerContainers.toBuffer
    containers += simpleHttpServerContainer
    containers += gitServerContainer
    containers.toList
  }

  private val spotifyDockerClient: DockerClient =
    DefaultDockerClient.fromEnv.build

  private def executeCommand(command: String): String = {
    executeCommand(command.split(" "))
  }

  private def executeCommand(commandParts: Array[String]): String = {
    val dockerContainerState =
      containerManager.getContainerState(gitServerContainer)
    val id = Await.result(dockerContainerState.id, 5.seconds)
    val execCreation =
      spotifyDockerClient.execCreate(id,
                                     commandParts,
                                     DockerClient.ExecCreateParam.attachStdout,
                                     DockerClient.ExecCreateParam.attachStderr)
    val output = spotifyDockerClient.execStart(execCreation.id())
    output.readFully
  }

}
