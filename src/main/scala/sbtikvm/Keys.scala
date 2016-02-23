package sbtikvm

import sbt._


object Keys {
  val netOutputPath = SettingKey[File]("netOutputPath", "Path in which to place .NET build products.")
  val ikvmPath = SettingKey[File]("ikvmPath", "Path to IKVM installation.")
  val netFrameworkVersion = TaskKey[String]("netFrameworkVersion", "Version of .NET framework to build against.")
  val netReferencePaths = TaskKey[Seq[File]]("netReferencePaths", "Paths to search for referenced assemblies.")

  // Stub-related keys
  val netReferences =
    SettingKey[Seq[AssemblyReference]]("netReferences", ".NET assemblies to expose to Java/Scala code.")
  val netResolvedReferences =
    TaskKey[Seq[File]]("netResolvedReferences", "Resolved paths to .NET assemblies to reference.")
  val makeNetStubs = TaskKey[Seq[File]]("makeNetStubs", "Create .jar stubs for referenced .NET assemblies")

  // ikvmc-related keys
  val netOutputType = SettingKey[OutputType]("netOutputType", ".NET assembly output type")
  val netAssemblyName = SettingKey[String]("netAssemblyName", ".NET assembly name")
  val netTranspileDependencies = TaskKey[Seq[File]]("netTranspileDependencies", "Transpile dependency jars to .NET assemblies.")
  val netCopyReferences = TaskKey[Unit]("netCopyReferences", "Copy .NET references to output directory.")
  val netPackage = TaskKey[File]("netPackage", "Create .NET assembly.")
}
