package virtual.machine.execution;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import virtual.machine.core.Pair;

/**
 *
 * @author aniket
 */
public class Compiler {

    private final HashMap<String, Byte> mappings = new HashMap<String, Byte>() {
        {
            put("halt", (byte) 0x00);
            put("nop", (byte) 0x10);
            put("rrmovq", (byte) 0x20);
            put("cmovle", (byte) 0x21);
            put("cmovl", (byte) 0x22);
            put("cmove", (byte) 0x23);
            put("cmovne", (byte) 0x24);
            put("cmovge", (byte) 0x25);
            put("cmovg", (byte) 0x26);
            put("irmovq", (byte) 0x30);
            put("rmmovq", (byte) 0x40);
            put("mrmovq", (byte) 0x50);
            put("addq", (byte) 0x60);
            put("subq", (byte) 0x61);
            put("andq", (byte) 0x62);
            put("xorq", (byte) 0x63);
            put("jmp", (byte) 0x70);
            put("jle", (byte) 0x71);
            put("jl", (byte) 0x72);
            put("je", (byte) 0x73);
            put("jne", (byte) 0x74);
            put("jge", (byte) 0x75);
            put("jg", (byte) 0x76);
            put("call", (byte) 0x80);
            put("ret", (byte) 0x90);
            put("pushq", (byte) 0xA0);
            put("popq", (byte) 0xB0);

            put("%rax", (byte) 0x0);
            put("%rcx", (byte) 0x1);
            put("%rdx", (byte) 0x2);
            put("%rbx", (byte) 0x3);
            put("%rsp", (byte) 0x4);
            put("%rbp", (byte) 0x5);
            put("%rsi", (byte) 0x6);
            put("%rdi", (byte) 0x7);
            put("%r8", (byte) 0x8);
            put("%r9", (byte) 0x9);
            put("%r10", (byte) 0xA);
            put("%r11", (byte) 0xB);
            put("%r12", (byte) 0xC);
            put("%r13", (byte) 0xD);
            put("%r14", (byte) 0xE);
            put("F", (byte) 0xF);
        }
    };

    private Compiler() {
    }

    private final HashMap<String, Byte> byteMap = new HashMap<String, Byte>() {
        {
            put("halt", (byte) 1);
            put("nop", (byte) 1);
            put("rrmovq", (byte) 2);
            put("cmovle", (byte) 2);
            put("cmovl", (byte) 2);
            put("cmove", (byte) 2);
            put("cmovne", (byte) 2);
            put("cmovge", (byte) 2);
            put("cmovg", (byte) 2);
            put("irmovq", (byte) 10);
            put("rmmovq", (byte) 10);
            put("mrmovq", (byte) 10);
            put("addq", (byte) 2);
            put("subq", (byte) 2);
            put("andq", (byte) 2);
            put("xorq", (byte) 2);
            put("jmp", (byte) 9);
            put("jle", (byte) 9);
            put("jl", (byte) 9);
            put("je", (byte) 9);
            put("jne", (byte) 9);
            put("jge", (byte) 9);
            put("jg", (byte) 9);
            put("call", (byte) 9);
            put("ret", (byte) 1);
            put("pushq", (byte) 2);
            put("popq", (byte) 2);
        }
    };

    private static Compiler compiler;

    public static Compiler getInstance() {
        if (compiler == null) {
            compiler = new Compiler();
        }
        return compiler;
    }

    private final Set<String> lab = new HashSet<>();

    public Set<String> getLabels() {
        return lab;
    }

    //catch index out of bounds?
    public synchronized ArrayList<Pair<String, ArrayList<Byte>>> compile(String s) throws CompilerException {
        ArrayList<Pair<String, ArrayList<Byte>>> oper = new ArrayList<>();
        //ArrayList<Byte> operations = new ArrayList<>();
        long indexPos = 0;
        String[] lines = s.split("\n");
        HashMap<String, Long> labels = new HashMap<>();
        for (int x = 0; x < lines.length; x++) {
            String val = lines[x].trim();
            if (val.contains("#")) {
                val = val.substring(0, val.indexOf("#"));
            }
            lines[x] = val;
        }
        long totalBytes = 0;
        int line = 0;
        try {
            for (String val : lines) {
                Pair<String, ArrayList<Byte>> p = new Pair<>(val, null);
                if (!val.isEmpty()) {
                    String[] spl = val.split("\\s+");
                    String token = spl[0];
                    if (byteMap.containsKey(token)) {
                        totalBytes += byteMap.get(token);
                    } else if (token.equals(".align")) {
                        //extra argument here
                        totalBytes += Long.BYTES - (totalBytes % 8);
                    } else if (token.equals(".quad")) {
                        totalBytes += Long.BYTES;
                    } else if (token.startsWith(".pos")) {
                        if (spl.length < 2) {
                            throw new CompilerException(line + 1, "Missing operation for .pos directive");
                        }
                        int base = 10;
                        if (spl[1].startsWith("0x")) {
                            spl[1] = spl[1].substring(2);
                            base = 16;
                        }
                        totalBytes = Long.parseLong(spl[1], base);
                    } else if (token.endsWith(":")) {
                        labels.put(token.substring(0, token.length() - 1), totalBytes);
                    } else {
                        throw new CompilerException(line + 1, "Unrecognized token : " + token);
                    }
                }
                oper.add(p);
                line++;
            }
            line = 0;
            for (String val : lines) {
                Pair<String, ArrayList<Byte>> bi = oper.get(line);
                ArrayList<Byte> operations = new ArrayList<>();
                if (!val.isEmpty()) {
                    if (val.endsWith(":")) {
                    } else if (val.startsWith(".")) {
                        if (val.startsWith(".quad")) {
                            //argument not there
                            String num = val.split("\\s+")[1];
                            int base = 10;
                            if (num.startsWith("0x")) {
                                num = num.substring(2);
                                base = 16;
                            }
                            int diff = 16 - num.length();
                            for (int n = 0; n < diff; n++) {
                                num += "0";
                            }
                            //this is incorrect for base 10
                            for (int n = 14; n >= 0; n -= 2) {
                                operations.add((byte) Integer.parseInt(num.substring(n, n + 2), base));
                            }
                            indexPos += Long.BYTES;
                        } else if (val.startsWith(".pos")) {
                            //missing argument
                            String num = val.split("\\s+")[1];
                            int base = 10;
                            if (num.startsWith("0x")) {
                                num = num.substring(2);
                                base = 16;
                            }
                            long al = Long.parseLong(num, base);
                            long diff = al - indexPos;
                            for (long a = 0; a < diff; a++) {
                                operations.add((byte) 0);
                            }
                            indexPos += diff;
                        } else if (val.startsWith(".align")) {
                            //missing argument
                            long al = Long.parseLong(val.split("\\s+")[1]);
                            long diff = al - (indexPos % al);
                            indexPos += diff;
                            for (long a = 0; a < diff; a++) {
                                operations.add((byte) 0);
                            }
                        }
                    } else {
                        ArrayList<Byte> arr = getArray(val, labels, line + 1);
                        for (byte b : arr) {
                            operations.add(b);
                        }
                        indexPos += arr.size();
                    }
                }
                bi.setValue(operations);
                line++;
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new CompilerException(line + 1, "Unrecognized tokens on line : " + lines[line]);
        }
        lab.addAll(labels.keySet());
        return oper;
    }

    private ArrayList<Byte> getArray(String s, HashMap<String, Long> labels, int line) throws CompilerException {
        s = s.replaceAll(", ", ",");
        ArrayList<Byte> byt = new ArrayList<>();
        if (s.startsWith("irmovq")) {
            //no argument, empty string or indexoutofbounds on idnexOF
            String val = s.substring(s.indexOf(" ") + 1, s.indexOf(",")).trim();
            long l;
            if (labels.containsKey(val)) {
                l = labels.get(val);
            } else {
                //double check dollar sign is there
                val = val.substring(1);
                int base = 10;
                if (val.startsWith("0x")) {
                    base = 16;
                    val = val.substring(2);
                }
                l = Long.parseLong(val, base);
            }
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(0, l);
            buffer.flip();
            byte[] array = buffer.array();
            reverse(array);
            byt.add(mappings.get("irmovq"));
            String register = s.substring(s.indexOf(",") + 1).trim();
            byt.add((byte) (mappings.get(register) | (0xF << 4)));
            for (byte b : array) {
                byt.add(b);
            }
        } else if (s.startsWith("rmmovq")) {
            byt.add(mappings.get("rmmovq"));
            s = s.substring(7);
            byte sourceRegister = mappings.get(s.substring(0, 4));
            s = s.substring(5).trim();
            String offset = s.substring(0, s.indexOf("("));
            String register = s.substring(s.indexOf("(") + 1, s.indexOf(")"));
            byt.add((byte) (sourceRegister << 4 | mappings.get(register)));
            int base = 10;
            if (offset.startsWith("0x")) {
                base = 16;
                offset = offset.substring(2);
            }
            long l = Long.parseLong(offset, base);
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(0, l);
            buffer.flip();
            byte[] array = buffer.array();
            reverse(array);
            for (byte b : array) {
                byt.add(b);
            }
        } else if (s.startsWith("mrmovq")) {
            byt.add(mappings.get("mrmovq"));
            s = s.substring(7);
            String offset = s.substring(0, s.indexOf("("));
            String sourceReg = s.substring(s.indexOf("(") + 1, s.indexOf(")"));
            s = s.substring(s.indexOf(",") + 1).trim();
            byt.add((byte) (mappings.get(sourceReg) << 4 | mappings.get(s)));
            int base = 10;
            if (offset.startsWith("0x")) {
                base = 16;
                offset = offset.substring(2);
            }
            long l = Long.parseLong(offset, base);
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(0, l);
            buffer.flip();
            byte[] array = buffer.array();
            reverse(array);
            for (byte b : array) {
                byt.add(b);
            }
        } else if (s.startsWith("popq") || s.startsWith("pushq")) {
            String[] split = s.split("\\s+");
            String register = split[1];
            byt.add(mappings.get(split[0]));
            byt.add((byte) (mappings.get(register) << 4 | 0xF));
        } else {
            String[] spl = s.split(",");
            ArrayList<String> args = new ArrayList<>();
            for (String sa : spl) {
                String[] sp = sa.trim().split("\\s+");
                for (String sap : sp) {
                    args.add(sap.trim());
                }
            }
            boolean lessThan = false;
            for (String val : args) {
                if (mappings.containsKey(val)) {
                    byte b = mappings.get(val);
                    if (val.startsWith("%")) {
                        if (lessThan) {
                            byt.set(byt.size() - 1, (byte) (byt.get(byt.size() - 1) << 8 | b));
                            lessThan = false;
                        } else {
                            byt.add(b);
                            lessThan = true;
                        }
                    } else {
                        byt.add(b);
                    }
                } else if (labels.containsKey(val)) {
                    long l = labels.get(val);
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                    buffer.putLong(0, l);
                    byte[] array = buffer.array();
                    reverse(array);
                    for (byte b : array) {
                        byt.add(b);
                    }
                } else {
                    throw new CompilerException(line + 1, "Unrecognized tokens on line : " + s);
                }
            }
        }
        if (byt.size() != byteMap.get(s.split("\\s+")[0])) {
            throw new CompilerException(line, "missing arguments");
        }
        return byt;

    }

    public static void reverse(byte[] b) {
        for (int x = 0; x < b.length / 2; x++) {
            int end = b.length - (x + 1);
            byte temp = b[x];
            b[x] = b[end];
            b[end] = temp;
        }
    }
}
