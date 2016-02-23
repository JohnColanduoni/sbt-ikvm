import sbt.Keys._

val sharedSettings = Seq(
  organization := "com.hevylight",
  version := "0.2.2-SNAPSHOT",
  bintrayOrganization := Some("hevylight")
)

lazy val plugin = (project in file("."))
  .settings(sharedSettings)
  .settings(
    name := "sbt-ikvm",
    sbtPlugin := true
  )
  // SBT tests
  .settings(
    scriptedSettings,
    sbtTestDirectory <<= baseDirectory (_ / "sbt-test"),
    scriptedLaunchOpts ++= {
      val ikvmPackageTarget = (target in ikvmPackage).value
      val ikvmPackageVersion = (version in ikvmPackage).value
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    publishLocal := {
      val p = (publishLocal in ikvmPackage).value
      publishLocal.value
    }
  )
  .settings(
    bintrayRepository := "sbt-plugins",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")),
    publishMavenStyle := false
  )
  .dependsOn(ikvmPackage)

lazy val ikvmPackage = Project("ikvm", file("ikvmPackage"))
  .settings(sharedSettings)
  .settings(
    name := "ikvm-bin",
    version := "8.1.5717.0",
    crossPaths := false,
    autoScalaLibrary := false
  )
  // Unzip IKVM
  .settings(
    mappings in Compile in packageBin ++= {
      val targetPath = target.value
      val versionValue = version.value
      IO.unzip(baseDirectory.value / s"ikvmbin-$versionValue.zip", targetPath)
      val ikvmPath = targetPath / s"ikvm-$versionValue"

      // Write manifest
      val ikvmFiles = ikvmPath.**(-DirectoryFilter).get
      val fileListPath = targetPath / "files_list"
      IO.write(fileListPath, ikvmFiles.map { _.relativeTo(ikvmPath).get }.mkString("\n"))

      (ikvmFiles pair Path.rebase(ikvmPath, "ikvm")) :+ (fileListPath -> "ikvm/files_list")
    }
  )
  .settings(
    licenses ++= Seq(
      ("GPL-2.0", url("https://www.gnu.org/licenses/gpl-2.0.en.html"))
    )
  )

    