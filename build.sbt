organization := "com.hevylight"

name := "sbt-ikvm"

version := "0.1-SNAPSHOT"

sbtPlugin := true

scriptedSettings

sbtTestDirectory <<= baseDirectory (_ / "sbt-test")

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value, "-Dikvm.path=" + (target.value / "ikvm"))
}

scriptedBufferLog := false
    