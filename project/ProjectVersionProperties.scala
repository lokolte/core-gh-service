import com.typesafe.sbt.web.Import.Assets
import sbt.Keys._
import sbt._

import scala.sys.process.Process

object ProjectVersionProperties {

  private val gitSha = Process("git rev-parse HEAD").lineStream.head
  private val serviceVersion: String = {
    val branchName = Process("git name-rev --name-only HEAD").lineStream.head
    if (branchName startsWith "release/")
      branchName.replace("release/", "") + "-" + gitSha.substring(0, 7)
    else
      branchName
  }

  val makeVersionProperties =
    taskKey[Seq[File]]("Creates a version.properties file we can find at runtime.")


  val buildVersionSettings: Seq[Def.Setting[_]] = Seq(
    makeVersionProperties := {
      //public/
      val propFile = (resourceDirectory in Assets).value / "version.json"
      val content = """|{"version":"%s",
                       |"git-sha":"%s",
                       |"timestamp":"%s"}
                    """.stripMargin
        .format(
          serviceVersion,
          gitSha,
          new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new java.util.Date())
        )
      IO.write(propFile, content)
      Seq(propFile)
    },
    version := serviceVersion,
    resourceGenerators in Assets += makeVersionProperties.taskValue
  )
}
