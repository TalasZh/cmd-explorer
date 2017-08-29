package kg.talaszh.cmdexplorer;

/**
 * Created by talas on 8/28/17.
 */
public class ConsoleHelper {

    public static void printlnRedBold(String str) {
        System.out.println("\033[31;1m" + str + "\033[0m");
    }

    public static void printGreenBold(String str) {
        System.out.print("\033[32;1;2m" + str + "\033[0m");
    }

    public static void printYellow(String str) {
        System.out.println("\033[33m" + str + "\033[0m");

    }
}
