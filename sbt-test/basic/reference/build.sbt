lazy val library = Project(id = "library", base = file("library"))
  .settings(
    exportJars := true,
    crossPaths := false,
    autoScalaLibrary := false
  )

lazy val executable = Project(id = "executable", base = file("executable"))
  .settings(
    netSettings,
    netAssemblyName := "Test",
    crossPaths := false,
    autoScalaLibrary := false
  )
  .dependsOn(library)

