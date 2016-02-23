lazy val libraryA = Project(id = "libraryA", base = file("libraryA"))
  .settings(
    exportJars := true
  )

lazy val libraryB = Project(id = "libraryB", base = file("libraryB"))
  .settings(
    exportJars := true
  )
  .dependsOn(libraryA)

lazy val executable = Project(id = "executable", base = file("executable"))
  .settings(
    netSettings,
    netAssemblyName := "Test"
  )
  .dependsOn(libraryA, libraryB)

