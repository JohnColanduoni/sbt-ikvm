package sbtikvm

import java.io.FilenameFilter

import sbt._
import sbt.Keys._

import Keys._

import scala.collection.mutable.ArrayBuffer

object Tasks {
  val netTasks = Seq(
    ikvmPath := {
      val outputPath = target.value / "ikvm"
      IO.createDirectory(outputPath)
      ikvm.Extract.extract(outputPath)
      outputPath
    },

    netReferencePaths := {
      val version = netFrameworkVersion.value
      val VersionPattern = """(\d+)\.\d+(\.\d+)?""".r
      val major = version match {
        case VersionPattern(m, _*) => m.toInt
        case _ => throw new RuntimeException("Invalid .NET framework version.")
      }

      val referenceAssemblies = if(isWindows) {
        file("""C:\Program Files (x86)\Reference Assemblies\Microsoft\Framework\.NETFramework\v""" + version)
      } else {
        // Find mono library path
        if(System.getProperty("os.name") == "Mac OS X") {
          file("/Library/Frameworks/Mono.framework/Versions/Current/lib/mono/" + version)
        } else {
          file("/usr/lib/mono/" + version)
        }
      }

      referenceAssemblies :: Nil
    },
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

      val args = ArrayBuffer[String]()
      args += "-nostdlib"
      args ++= referencePaths.map { path => s"-lib:$path" }

      resolvedReferences.zip(stubPaths).foreach { case (assemblyPath, jarPath) =>
        val ret = netExec(Seq(ikvmstubPath.toString, assemblyPath.toString) ++ args ++ Seq(s"-out:$jarPath"))
        if(ret != 0)
          throw new RuntimeException("ikvmstub.exe failed")
      }

      stubPaths
    },
    unmanagedJars in Compile ++= makeNetStubs.value.classpath,

    netTranspileDependencies := {
      val s = streams.value
      val ikvmcPath = ikvmPath.value / "bin" / "ikvmc.exe"

      val outputPath = netOutputPath.value
      val stubs = makeNetStubs.value.map { _.getCanonicalPath }
      val resolvedReferences = netResolvedReferences.value

      val dependencies = (dependencyClasspath in Compile).value
        // Filter stubs
        .filter { cp => !stubs.contains(cp.data.getCanonicalPath) }

      val assemblies = dependencies.map { cp =>
        val assemblyName = cp.metadata.get(AttributeKey[ModuleID]("module-id"))
          .map { id =>
            id.organization + "." + id.name
          }
          .getOrElse {
            val fileName = cp.data.getName
            val assemblyName = if(fileName.endsWith(".jar")) {
              fileName.substring(0, fileName.length - 4)
            } else {
              fileName
            }
          }
        outputPath / (assemblyName + ".dll")
      }

      // Sort dependencies by reference order
      val configurationReport = update.value.configuration("compile").get

      val edges = for {
        moduleReport <- configurationReport.modules
        caller <- moduleReport.callers
      } yield (caller.caller, moduleReport.module)

      object ModuleOrdering extends Ordering[ModuleID] {
        def compare(x: ModuleID, y: ModuleID): Int = {
          if(dependsOn(x, y))
            1
          else if(dependsOn(y, x))
            -1
          else
            0
        }

        private def dependsOn(x: ModuleID, y: ModuleID): Boolean = {
          edges
            .exists { case (greater, lesser) =>
                moduleEqual(greater, x) && (moduleEqual(lesser, y) || dependsOn(lesser, y))
            }
        }

        private def moduleEqual(x: ModuleID, y: ModuleID): Boolean = {
          def crossCheck(a: ModuleID, b: ModuleID): Boolean = {
            a.name.startsWith(b.name) &&
              a.crossVersion == CrossVersion.Disabled &&
              a.name.substring(b.name.length).matches("""_\d+.\d+""")
          }

          x.organization == y.organization && {
            x.name == y.name || {
              crossCheck(x, y) || crossCheck(y, x)
            }
          } && x.revision == y.revision
        }
      }

      val alreadyTranspiled = ArrayBuffer[File]()
      dependencies.zip(assemblies)
        .sortBy { _._1.metadata(AttributeKey[ModuleID]("module-id")) }(ModuleOrdering)
        // We do this in reverse to properly resolve dependencies between class-path dependencies
        .foreach { case (cp, assembly) =>
          if(!cp.data.isFile)
            throw new RuntimeException("Dependencies must be in the form of jar files. " +
              "You must set 'exportJars := true' for any project dependencies.")

          s.log.info(s"Transpiling ${cp.data}")
          val ret = ikvmc(ikvmcPath, assembly, Seq(cp.data), resolvedReferences ++ alreadyTranspiled)
          if(ret != 0)
            throw new RuntimeException("ikvmc.exe failed")

          alreadyTranspiled += assembly
      }

      assemblies
    },
    netCopyReferences := {
      val outputPath = netOutputPath.value

      // Copy resolved references
      val resolvedReferences = netResolvedReferences.value
      resolvedReferences
        // Filter framework assemblies
        .filter { file =>
          !file.toString.startsWith("""C:\Program Files (x86)\Reference Assemblies\Microsoft\Framework\.NETFramework""")
        }
        .foreach { file =>
        IO.copyFile(file, outputPath / file.getName, preserveLastModified = true)
      }

      // Copy IKVM libraries
      val ikvmBinPath = ikvmPath.value / "bin"
      if(!ikvmBinPath.exists())
        throw new RuntimeException("Invalid IKVM path.")
      ikvmBinPath
        .list()
        .filter { _.endsWith(".dll") }
        .map { ikvmBinPath / _ }
        .foreach { file =>
          IO.copyFile(file, outputPath / file.getName, preserveLastModified = true)
        }
    },
    netPackage := {
      val s = streams.value
      netCopyReferences.value

      val ikvmcPath = ikvmPath.value / "bin" / "ikvmc.exe"

      val inputJar = (packageBin in Compile).value
      val outputType = netOutputType.value
      val extension = outputType match {
        case OutputType.Executable => "exe"
        case OutputType.Library => "dll"
      }
      val outputPath = netOutputPath.value / (netAssemblyName.value + "." + extension)
      val resolvedReferences = netResolvedReferences.value
      val transpiledDependencies = netTranspileDependencies.value

      val extraArgs = ArrayBuffer[String]()

      if(outputType == OutputType.Executable) {
        extraArgs +=
          s"-main:${(mainClass in Compile).value.getOrElse { throw new RuntimeException("Main class required for executable.") }}"
      }

      s.log.info(s"Transpiling $inputJar")
      val ret = ikvmc(ikvmcPath, outputPath, Seq(inputJar), resolvedReferences ++ transpiledDependencies, extraArgs)
      if(ret != 0)
        throw new RuntimeException("ikvmc.exe failed")

      outputPath
    }
  )

  private def ikvmc(ikvmcPath: File,
                    outputPath: File,
                    inputs: Seq[File],
                    netReferences: Seq[File],
                    extraArgs: Seq[String] = Nil): Int = {
    val command = ArrayBuffer(ikvmcPath.toString)
    command ++= inputs.map { _.toString }
    command += s"-out:$outputPath"
    command += "-nostdlib"
    command += "-nologo"
    command += "-warnaserror:0100"

    command ++= netReferences.map { path => s"-r:$path" }
    command ++= extraArgs

    netExec(command)
  }

  private def netExec(command: Seq[String]): Int = {
    if(isWindows) {
      command.!
    } else {
      (Seq("mono") ++ command).!
    }
  }

  private def isWindows: Boolean = System.getProperty("os.name").startsWith("Windows")
}
