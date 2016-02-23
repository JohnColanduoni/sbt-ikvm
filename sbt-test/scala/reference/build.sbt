lazy val library = Project(id = "library", base = file("library"))
  .settings(
    exportJars := true
  )

lazy val executable = Project(id = "executable", base = file("executable"))
  .settings(
    netSettings,
    netAssemblyName := "Test"
  )
  .dependsOn(library)

