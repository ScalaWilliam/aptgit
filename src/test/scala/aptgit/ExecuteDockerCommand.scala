package aptgit

import java.io.ByteArrayOutputStream

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.command.ExecStartResultCallback
import com.whisk.docker.{DockerContainer, DockerContainerManager}

import scala.concurrent.Await
import scala.concurrent.duration._

final case class ExecuteDockerCommand(
    plainDockerClient: DockerClient,
    containerManager: DockerContainerManager) {
  def apply(dockerContainer: DockerContainer, command: String): String = {
    apply(dockerContainer, command.split(" "))
  }

  /**
    * https://github.com/spotify/docker-client/issues/513#issuecomment-351797933
    */
  def apply(dockerContainer: DockerContainer,
            commandParts: Array[String]): String = {
    val dockerContainerState =
      containerManager.getContainerState(dockerContainer)
    val id =
      Await.result(dockerContainerState.id, 5.seconds)

    val response =
      plainDockerClient
        .execCreateCmd(id)
        .withCmd(commandParts: _*)
        .withAttachStderr(false)
        .withAttachStdout(true)
        .withAttachStderr(true)
        .withTty(false)
        .exec()

    val baos = new ByteArrayOutputStream()
    try {
      plainDockerClient
        .execStartCmd(response.getId)
        .withDetach(false)
        .withTty(false)
        .exec(new ExecStartResultCallback(baos, baos))
        .awaitCompletion()
      new String(baos.toByteArray, "UTF-8")
    } finally baos.close()
  }
}
