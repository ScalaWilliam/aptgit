package aptgit

import com.github.dockerjava.api
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}

trait DockerClients {

  protected lazy val spotifyDockerClient: DockerClient =
    DefaultDockerClient.fromEnv.build

  /**
    * https://github.com/docker-java/docker-java/issues/481#issuecomment-189147787
    */
  protected lazy val plainDockerClient: api.DockerClient = DockerClientBuilder
    .getInstance()
    .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
    .build()

}
