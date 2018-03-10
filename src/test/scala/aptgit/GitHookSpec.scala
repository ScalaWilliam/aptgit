package aptgit
import java.nio.file.Paths

import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{DockerContainer, VolumeMapping}
import org.scalatest.Matchers._
import org.scalatest._
import scala.concurrent.duration._
import scala.concurrent.Await

class GitHookSpec extends FreeSpec with DockerTestKit with DockerKitSpotify {

  private val gitDockerImageName = "jkarlos/git-server-docker"

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

  private val testSetupVolume = VolumeMapping(
    host = Paths.get("test-setup").toAbsolutePath.toString,
    container = "/test-setup/",
    rw = false,
  )

  private val gitServerContainer = DockerContainer(gitDockerImageName)
    .withVolumes(testSetupVolume :: Nil)

  override def dockerContainers: List[DockerContainer] =
    gitServerContainer :: super.dockerContainers

  private val spotifyDockerClient: DockerClient =
    DefaultDockerClient.fromEnv.build

  private def executeCommand(command: String): String = {
    executeCommand(Array(command))
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
