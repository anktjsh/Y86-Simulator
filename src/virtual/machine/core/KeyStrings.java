/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package virtual.machine.core;

import javafx.scene.input.KeyCode;

/**
 *
 * @author aniket
 */
public class KeyStrings {

    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");

    public static String getCode(KeyCode code, boolean meta, boolean shift) {
        if (meta) {
            switch (code) {
                case S:
                    if (shift) {
                        return getMetaKey() + "Shift+" + "S";
                    } else {
                        return getMetaKey() + "S";
                    }
                case O:
                    return getMetaKey() + "O";
                case N:
                    return getMetaKey() + "N";
                case Z:
                    return getMetaKey() + "Z";
                case Y:
                    return getMetaKey() + "Y";
                case X:
                    return getMetaKey() + "X";
                case C:
                    return getMetaKey() + "C";
                case V:
                    return getMetaKey() + "V";
                case F:
                    return getMetaKey() + "F";
                case R:
                    return getMetaKey() + "R";
                case A:
                    return getMetaKey() + "A";
            }
        }
        return "";
    }

    public static String getMetaKey() {
        if (IS_MAC) {
            return "Command+";
        }
        return "Control+";
    }
}
