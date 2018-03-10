package aptgit
import java.nio.file.{Files, Paths}

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{DockerContainer, VolumeMapping}
import org.scalatest.Matchers._
import org.scalatest._

class GitHookSpec extends FreeSpec with DockerTestKit with DockerKitSpotify {

  private val StandardSshPort = 22
  private val MappedSshPort = 2222

  private val keysMapping = VolumeMapping(
    host = Paths.get("pub-sample").toAbsolutePath.toString,
    container = "/git-server/keys",
    rw = false,
  )

  private val reposMapping = VolumeMapping(
    host = Paths.get("repos").toAbsolutePath.toString,
    container = "/git-server/repos",
    rw = true,
  )

  private val gitServerContainer = DockerContainer("jkarlos/git-server-docker")
    .withPorts(StandardSshPort -> Some(MappedSshPort))
    .withVolumes(keysMapping :: reposMapping :: Nil)

  override def dockerContainers: List[DockerContainer] =
    gitServerContainer :: super.dockerContainers

  def pushToGitServer(): Unit = {}

  "Log-in is successful" in {
    val cmd = Seq("ssh",
                  "-tt",
                  "git@127.0.0.1",
                  "-p",
                  s"${MappedSshPort}",
                  "-o",
                  "StrictHostKeyChecking=no",
                  "-o",
                  "UserKnownHostsFile=/dev/null")
    import scala.sys.process._
    val result = cmd.lineStream_!
    exactly(1, result) should include("Welcome to git-server-docker")
  }

  "It works" in {
    val expectedFile = Paths.get("received")
    assert(!Files.exists(expectedFile))
    info("Pushing to the server...")
    assert(Files.exists(expectedFile))
  }

}
