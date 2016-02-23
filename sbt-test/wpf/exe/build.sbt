ikvmPath := file(System.getProperty("ikvm.path"))

netSettings

netReferences ++= Seq(
  "WindowsBase",
  "PresentationCore",
  "PresentationFramework",
  "System.Xaml"
)

netAssemblyName := "Test"