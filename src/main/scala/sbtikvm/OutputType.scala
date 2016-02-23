package sbtikvm

sealed abstract class OutputType

object OutputType {
  case object Executable extends OutputType
  case object Library extends OutputType
}
