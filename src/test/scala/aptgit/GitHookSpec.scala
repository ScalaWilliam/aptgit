package aptgit
import java.io.ByteArrayOutputStream
import java.nio.file.Paths

import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.ExecStartResultCallback
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

trait GitHookSpec extends FreeSpec with DockerTestKit with DockerKitSpotify {

  private val gitDockerImageName =
    "scalawilliam/aptgit-test-server"
  private val httpDumpServerImageName =
    "scalawilliam/aptgit-http-dump-server"
  private val simpleHttpServerImageName =
    "trinitronx/python-simplehttpserver"

  "Prepare environment" - {
    "1. Configure Git" in {
      executeCommand(gitServerContainer, "/test-setup/configure-git.sh") shouldBe empty
    }
    "2. Prepare SSH key" in {
      executeCommand(gitServerContainer, "/test-setup/prepare-ssh-key.sh") should include(
        "public key has been saved")
    }
    "3. Prepare Git repository" in {
      executeCommand(gitServerContainer, "/test-setup/prepare-git-repo.sh") should include(
        "Initialized empty Git repository")
    }
    "4. Configure WebSub publisher" in {
      executeCommand(gitServerContainer,
                     "/test-setup/prepare-websub-publish.sh")
    }
  }

  s"Verify that we can execute WebSub hooks against Docker image '${gitDockerImageName}'" - {
    "Ensure the HTTP resource can be read" in {
      executeCommand(
        gitServerContainer,
        "wget -O - -q http://simple_http_server:8080/blah.html") should include(
        "never")
    }

    "Discover an updated HTML page when a push is made" in {
      val pushResult =
        executeCommand(gitServerContainer, "/test-setup/clone-and-push.sh")
      withClue(s"Push result was: ${pushResult}") {
        executeCommand(
          gitServerContainer,
          "wget -O - -q http://simple_http_server:8080/blah.html") should not include ("never")
      }
    }

    "Discover a HUB HTTP POST item for the prior push" in {
      info("This is the WebSub notify POST")
      val logLines = httpLines()
      logLines should include("POST /notify")
      logLines should include(
        "hub.url=http%3A%2F%2Fsimple_http_server%3A8080%2Fblah.html&hub.mode=publish")
    }

  }

  def httpLines(): String = {
    val dockerContainerState =
      containerManager.getContainerState(httpDumpServerContainer)
    val id =
      Await.result(dockerContainerState.id, 5.seconds)
    val logStream =
      spotifyDockerClient.logs(id,
                               LogsParam.stderr(),
                               LogsParam.stdout(),
                               LogsParam.tail(20))
    try logStream.readFully()
    finally logStream.close()
  }

  private val testSetupVolume = VolumeMapping(
    host = Paths.get("test-setup").toAbsolutePath.toString,
    container = "/test-setup/",
    rw = false,
  )

  private val targetVolume = VolumeMapping(
    host = Paths
      .get("target/docker-env")
      .toAbsolutePath
      .toString,
    container = "/target/",
    rw = true,
  )

  private val targetVolume2 = VolumeMapping(
    host = Paths
      .get("target/docker-env")
      .toAbsolutePath
      .toString,
    container = "/var/www/",
    rw = true,
  )

  private val simpleHttpServerContainer =
    DockerContainer(simpleHttpServerImageName,
                    name = Some("simple_http_server"))
      .withVolumes(List(targetVolume2))
      .withPortMapping(8080 -> DockerPortMapping())

  private val httpDumpServerContainer =
    DockerContainer(httpDumpServerImageName, name = Some("http_dump_server"))
      .withVolumes(List(testSetupVolume, targetVolume))

  private val gitServerContainer =
    DockerContainer(gitDockerImageName)
      .withVolumes(List(testSetupVolume, targetVolume))
      .withLinks(
        ContainerLink(simpleHttpServerContainer, alias = "simple_http_server"),
        ContainerLink(httpDumpServerContainer, alias = "http_dump_server")
      )

  override def dockerContainers: List[DockerContainer] = {
    val containers = super.dockerContainers.toBuffer
    containers += simpleHttpServerContainer
    containers += gitServerContainer
    containers += httpDumpServerContainer
    containers.toList
  }

  private val spotifyDockerClient: DockerClient =
    DefaultDockerClient.fromEnv.build

  private val plainDockerClient = DockerClientBuilder.getInstance().build()

  private def executeCommand(dockerContainer: DockerContainer,
                             command: String): String = {
    executeCommand(dockerContainer, command.split(" "))
  }

  /**
    * https://github.com/spotify/docker-client/issues/513#issuecomment-351797933
    */
  private def executeCommand(container: DockerContainer,
                             commandParts: Array[String]): String = {
    val dockerContainerState =
      containerManager.getContainerState(container)
    val id =
      Await.result(dockerContainerState.id, 5.seconds)

    val response =
      plainDockerClient
        .execCreateCmd(id)
        .withCmd(commandParts: _*)
        .withAttachStdout(true)
        .withTty(false)
        .exec()

    val baos = new ByteArrayOutputStream()
    try {
      plainDockerClient
        .execStartCmd(response.getId)
        .withDetach(false)
        .withTty(false)
        .exec(new ExecStartResultCallback(baos, System.err))
        .awaitCompletion()
      new String(baos.toByteArray, "UTF-8")
    } finally baos.close()
  }

  override def startAllOrFail(): Unit = {

    /** This could take a bit of time **/
    val buildResult =
      spotifyDockerClient.build(Paths.get("test-server"), gitDockerImageName)
    assert(buildResult != null)

    /** This could take a bit of time **/
    val buildResultHttpDumpServer =
      spotifyDockerClient.build(Paths.get("http-dump-server"),
                                httpDumpServerImageName)

    assert(buildResultHttpDumpServer != null)
    super.startAllOrFail()
  }

}
