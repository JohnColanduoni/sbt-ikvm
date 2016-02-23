package test

object LibraryB {
  def fancyConcat(strs: Seq[String]): String = {
    LibraryA.wasteTime(strs.mkString)
  }
}
