package test

object Main {
  def main(args: Array[String]): Unit = {
    LibraryB.fancyConcat(Seq("Hello", " ", "World!"))
  }
}