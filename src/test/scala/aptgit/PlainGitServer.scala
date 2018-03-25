package aptgit

import java.io.ByteArrayInputStream
import java.nio.file.Paths

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.command.ExecStartResultCallback
import com.whisk.docker.{DockerContainer, DockerContainerManager}

import scala.concurrent.Await
import scala.concurrent.duration._

final case class PlainGitServer(
    gitServerContainer: DockerContainer,
    plainDockerClient: DockerClient,
    spotifyDockerClient: com.spotify.docker.client.DockerClient,
    containerManager: DockerContainerManager) {

  def addSshKey(publicKey: String): Unit = {
    val dockerContainerState =
      containerManager.getContainerState(gitServerContainer)
    val id =
      Await.result(dockerContainerState.id, 5.seconds)
    val response =
      plainDockerClient
        .execCreateCmd(id)
        .withCmd("tee", "-a", "/home/git/.ssh/authorized_keys")
        .withAttachStdin(true)
        .withAttachStdout(false)
        .withAttachStderr(false)
        .withTty(false)
        .exec()
    val bais = new ByteArrayInputStream(publicKey.getBytes("UTF-8"))
    try {
      plainDockerClient
        .execStartCmd(response.getId)
        .withDetach(false)
        .withStdIn(bais)
        .exec(new ExecStartResultCallback(null, null))
        .awaitCompletion()
    } finally bais.close()
  }

  def build(): Option[String] = {
    Option {
      spotifyDockerClient.build(
        Paths.get("src/test/resources/aptgit/test-git-server"),
        gitServerContainer.image)
    }
  }

}
