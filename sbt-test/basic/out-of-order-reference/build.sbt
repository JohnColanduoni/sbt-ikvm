lazy val libraryA = Project(id = "libraryA", base = file("libraryA"))
  .settings(
    exportJars := true,
    crossPaths := false,
    autoScalaLibrary := false
  )

lazy val libraryB = Project(id = "libraryB", base = file("libraryB"))
  .settings(
    exportJars := true,
    crossPaths := false,
    autoScalaLibrary := false
  )
  .dependsOn(libraryA)

lazy val executable = Project(id = "executable", base = file("executable"))
  .settings(
    netSettings,
    netAssemblyName := "Test",
    crossPaths := false,
    autoScalaLibrary := false
  )
  .dependsOn(libraryA, libraryB)

