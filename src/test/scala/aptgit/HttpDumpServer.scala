package aptgit

import java.nio.file.Paths

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.LogsParam
import com.whisk.docker.{DockerContainer, DockerContainerManager}

import scala.concurrent._
import duration._

final case class HttpDumpServer(httpDumpServerContainer: DockerContainer,
                                spotifyDockerClient: DockerClient,
                                containerManager: DockerContainerManager) {

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

  def build(): Option[String] = {
    Option {
      spotifyDockerClient.build(
        Paths.get("src/test/resources/aptgit/hub-http-dump-server"),
        httpDumpServerContainer.image)
    }
  }

}
object HttpDumpServer {
  type BuiltImageId = Option[String]
}
