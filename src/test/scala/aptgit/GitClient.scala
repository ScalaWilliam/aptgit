package aptgit

import com.whisk.docker.{DockerContainer, VolumeMapping}

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

  def push(repoPath: String): GitClient.PushResult = {
    executeDockerCommand(gitClientContainer,
                         Array("/clone-and-push.sh", repoPath))
  }

}
object GitClient {
  type CommandLineOutput = String
  type PushResult = CommandLineOutput

  def mappedVolumes: List[VolumeMapping] = {
    val sshConfig = VolumeMapping(
      host = getClass.getResource("client/sshconfig").getFile,
      container = "/sshconfig"
    )
    val cloneAndPush = VolumeMapping(
      host = getClass.getResource("client/clone-and-push.sh").getFile,
      container = "/clone-and-push.sh"
    )
    List(
      sshConfig,
      cloneAndPush
    )
  }
}
