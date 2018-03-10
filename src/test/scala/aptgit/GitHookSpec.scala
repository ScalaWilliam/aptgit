package aptgit
import java.nio.file.{Files, Paths}

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{DockerContainer, DockerReadyChecker}
import org.scalatest._

class GitHookSpec extends FreeSpec with DockerTestKit with DockerKitSpotify {

  private val DefaultMongodbPort = 27017

  private val mongodbContainer = DockerContainer("mongo:3.0.6")
    .withPorts(DefaultMongodbPort -> None)
    .withReadyChecker(
      DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers

  def pushToGitServer(): Unit = {}

  "It works" in {
    val expectedFile = Paths.get("received")
    assert(!Files.exists(expectedFile))
    info("Pushing to the server...")
    assert(Files.exists(expectedFile))
  }

}
