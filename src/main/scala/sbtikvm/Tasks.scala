package sbtikvm

import java.io.FilenameFilter

import sbt._
import sbt.Keys._

import Keys._

import scala.collection.mutable.ArrayBuffer

object Tasks {
  val netSettings = Seq(
    netOutputPath := target.value / "net",
    netFrameworkVersion := "4.5",
    netReferencePaths := {
      val version = netFrameworkVersion.value
      val VersionPattern = """(\d+)\.\d+(\.\d+)?""".r
      val major = version match {
        case VersionPattern(m, _*) => m.toInt
        case _ => throw new RuntimeException("Invalid .NET framework version.")
      }
      val paths = file("""C:\Program Files (x86)\Reference Assemblies\Microsoft\Framework\.NETFramework\v""" + version) :: {
        val frameworksPath = file("""C:\Windows\Microsoft.NET\Framework""")
        frameworksPath.list()
          .view
          .map { frameworksPath / _  }
          .find { _.getName.startsWith(s"v$major.") }
          .getOrElse { throw new RuntimeException("Invalid .NET framework version.") }
      } :: Nil

      println(paths)

      paths
    },

    netReferences := Seq(
      "mscorlib",
      "System",
      "System.Core"
    ).map { AssemblyReference(_) },
    netResolvedReferences := {
      val references = netReferences.value
      val referencePaths = netReferencePaths.value

      references.map { ref =>
        referencePaths
          .collectFirst(Function.unlift { basePath =>
            val path = basePath / (ref.name + ".dll")
            if(path.exists)
              Some(path)
            else
              None
          })
          .getOrElse { throw new RuntimeException(s"Could not find assembly ${ref.name}") }
      }
    },
    makeNetStubs := {
      val references = netReferences.value
      val resolvedReferences = netResolvedReferences.value
      val outputPath = netOutputPath.value / "stubs"
      val referencePaths = netReferencePaths.value

      IO.createDirectory(outputPath)

      val ikvmstubPath = ikvmPath.value / "bin" / "ikvmstub.exe"
      val stubPaths = references.map { ref => outputPath / s"${ref.name}.jar" }

      resolvedReferences.zip(stubPaths).foreach { case (assemblyPath, jarPath) =>
        val ret = Seq(ikvmstubPath.toString, assemblyPath.toString, s"-out:$jarPath").!
        if(ret != 0)
          throw new RuntimeException("ikvmstub.exe failed")
      }

      stubPaths
    },
    unmanagedJars in Compile ++= makeNetStubs.value.classpath,

    netOutputType := OutputType.Executable,
    netAssemblyName := name.value,
    netPackage := {
      val ikvmcPath = ikvmPath.value / "bin" / "ikvmc.exe"

      val jarPath = (packageBin in Compile).value
      val outputType = netOutputType.value
      val extension = outputType match {
        case OutputType.Executable => "exe"
        case OutputType.Library => "dll"
      }
      val outputPath = netOutputPath.value / (netAssemblyName.value + "." + extension)

      val resolvedReferences = netResolvedReferences.value

      val command = ArrayBuffer(ikvmcPath.toString, jarPath.toString)
      command += s"-out:$outputPath"

      // Add references
      command ++= resolvedReferences.map { path => s"-r:$path" }

      if(outputType == OutputType.Executable) {
        command +=
          s"-main:${(mainClass in Compile).value.getOrElse { throw new RuntimeException("Main class required for executable.") }}"
      }

      val ret = command.!
      if(ret != 0)
        throw new RuntimeException("ikvmc.exe failed")

      outputPath
    }
  )
}
