package test

import java.nio.charset.Charset

object LibraryA {
  def wasteTime(input: String): String  = {
    val utf8 = Charset.forName("UTF-8")
    val buffer = utf8.encode(input)
    utf8.decode(buffer).toString
  }
}
