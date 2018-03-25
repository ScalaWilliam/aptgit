package aptgit

import com.whisk.docker.DockerContainer

final case class GitClient(gitClientContainer: DockerContainer,
                           executeDockerCommand: ExecuteDockerCommand) {

  def createSshKey(): String = {
    executeDockerCommand(
      gitClientContainer,
      Array("ssh-keygen", "-t", "rsa", "-N", "", "-f", "/root/.ssh/id_rsa")
    )
    executeDockerCommand(gitClientContainer, "cat /root/.ssh/id_rsa.pub")
  }

  def setupSshConfig(): Unit = {
    executeDockerCommand(
      gitClientContainer,
      Array("cp", "/sshconfig", "/root/.ssh/config")
    )
  }

  def push(): GitClient.PushResult = {
    executeDockerCommand(gitClientContainer, "/clone-and-push.sh")
  }

}
object GitClient {
  type CommandLineOutput = String
  type PushResult = CommandLineOutput
}
