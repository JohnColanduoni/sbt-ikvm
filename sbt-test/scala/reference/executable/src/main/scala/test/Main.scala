package test

object Main {
  def main(args: Array[String]): Unit = {
    println(Library.fancyConcat(Seq("Hello", " ", "World!")))
  }
}
