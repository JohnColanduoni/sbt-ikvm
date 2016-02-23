package sbtikvm

sealed case class AssemblyReference(name: String, path: Option[String] = None)
