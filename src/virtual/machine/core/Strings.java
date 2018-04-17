package virtual.machine.core;

/**
 *
 * @author aniket
 */
public class Strings {

    private static final StringBuilder BUILD = new StringBuilder();

    public static String getHex(long a) {
        return getHexBuild().append(Long.toHexString(a).toUpperCase()).toString();
    }

    public static String getHex(int a) {
        return getHexBuild().append(Integer.toHexString(a).toUpperCase()).toString();
    }

    public static String getHexMinusPrefix(int a, int l) {
        String valA = Integer.toHexString(a).toUpperCase();
        int offset = 0;
        StringBuilder newBuild = newBuild();
        while (valA.length() + offset < l) {
            newBuild.append("0");
            offset++;
        }
        newBuild.append(valA);
        return newBuild.toString();
    }

    public static String getDecimal(long a) {
        return Long.toString(a);
    }

    public static String getDecimal(int a) {
        return Integer.toString(a);
    }

    public static String getDecimal(int a, int l) {
        StringBuilder newBuild = newBuild();
        if (a < 0) {
            a = -a;
            newBuild.append("-");
        }
        String valA = Integer.toString(a).toUpperCase();
        int offset = 0;
        while (valA.length() + offset < l) {
            newBuild.append("0");
            offset++;
        }
        newBuild.append(valA);
        return newBuild.toString();
    }

    public static String getHex(int a, int l) {
        String valA = Integer.toHexString(a).toUpperCase();
        int offset = 0;
        StringBuilder newBuild = getHexBuild();
        while (valA.length() + offset < l) {
            newBuild.append("0");
            offset++;
        }
        newBuild.append(valA);
        return newBuild.toString();
    }

    public static String getDecimal(long a, int l) {
        StringBuilder newBuild = newBuild();
        if (a < 0) {
            a = -a;
            newBuild.append("-");
        }
        String valA = Long.toString(a).toUpperCase();
        int offset = 0;
        while (valA.length() + offset < l) {
            newBuild.append("0");
            offset++;
        }
        newBuild.append(valA);
        return newBuild.toString();
    }

    public static String getHex(long a, int l) {
        String valA = Long.toHexString(a).toUpperCase();
        int offset = 0;
        StringBuilder newBuild = getHexBuild();
        while (valA.length() + offset < l) {
            newBuild.append("0");
            offset++;
        }
        newBuild.append(valA);
        return newBuild.toString();
    }

    public synchronized static StringBuilder getHexBuild() {
        return newBuild().append("0x");
    }

    public static StringBuilder newBuild() {
        BUILD.delete(0, BUILD.length());
        return BUILD;
    }
}
