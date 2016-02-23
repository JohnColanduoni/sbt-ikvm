package test;

public class LibraryB {
    public static String FancyConcat(String x, String y) {
        return LibraryA.wasteTime(x) + y;
    }
}