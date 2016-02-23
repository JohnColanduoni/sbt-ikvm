package sbtikvm

import sbt._

import scala.language.implicitConversions

object Plugin extends AutoPlugin {
  object autoImport {
    val netOutputPath = Keys.netOutputPath
    val ikvmPath = Keys.ikvmPath
    val netFrameworkVersion = Keys.netFrameworkVersion
    val netReferencePaths = Keys.netReferencePaths

    // Stub-related keys
    val netReferences = Keys.netReferences
    val netResolvedReferences = Keys.netResolvedReferences
    val makeNetStubs = Keys.makeNetStubs

    // ikvmc-related keys
    val netOutputType = Keys.netOutputType
    val netAssemblyName = Keys.netAssemblyName
    val netPackage = Keys.netPackage

    val netSettings = Keys.defaultSettings ++ Tasks.netTasks

    implicit def stringToAssembly(name: String): AssemblyReference = AssemblyReference(name)

    type NetOutputType = OutputType
    def NetExecutable = OutputType.Executable
    def NetLibrary = OutputType.Library
  }
}
