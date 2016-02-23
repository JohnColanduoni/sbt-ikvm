package test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class LibraryA {
    public static String wasteTime(String input) {
        Charset utf8 = Charset.forName("UTF-8");
        ByteBuffer buffer = utf8.encode(input);
        return utf8.decode(buffer).toString();
    }
}