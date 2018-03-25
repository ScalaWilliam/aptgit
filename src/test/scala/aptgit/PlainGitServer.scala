package aptgit

import java.io.ByteArrayInputStream
import java.nio.file.Paths

import aptgit.PlainGitServer.GitRepositoryPath
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.command.ExecStartResultCallback
import com.whisk.docker.{DockerContainer, DockerContainerManager}

import scala.concurrent.Await
import scala.concurrent.duration._

final case class PlainGitServer(
    gitServerContainer: DockerContainer,
    executeDockerCommand: ExecuteDockerCommand,
    plainDockerClient: DockerClient,
    spotifyDockerClient: com.spotify.docker.client.DockerClient,
    containerManager: DockerContainerManager) {

  def createRepository(
      name: String): Either[PlainGitServer.Error, GitRepositoryPath] = {
    val result = executeDockerCommand(
      gitServerContainer,
      Array("/test-setup/prepare-git-repo.sh", name))
    if (!result.contains("Initialized empty Git repository"))
      Left(result)
    else Right(s"/git-server/repos/$name.git")
  }

  def setupWebSub(hubUrl: String,
                  topicUrl: String,
                  gitServerTopicFileLocation: String,
                  repositoryName: String): Unit = {
    executeDockerCommand(
      gitServerContainer,
      Array("/test-setup/prepare-websub-publish.sh",
            hubUrl,
            topicUrl,
            gitServerTopicFileLocation,
            repositoryName)
    )
  }

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
object PlainGitServer {
  type GitRepositoryPath = String
  type Error = String
}
