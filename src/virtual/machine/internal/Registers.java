package virtual.machine.internal;

import virtual.machine.core.Strings;
import java.util.Arrays;
import java.util.HashMap;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author aniket
 */
public class Registers {

    private final HashMap<String, Integer> registerMap;
    private final long[] registers;
    private final ObservableList<Register> regList = FXCollections.observableArrayList();
    private final String[] regs = {"%rax", "%rcx", "%rdx", "%rbx", "%rsp", "%rbp",
        "%rsi", "%rdi", "%r8", "%r9", "%r10", "%r11", "%r12", "%r13", "%r14"};

    public ObservableList<Register> registerData() {
        return regList;
    }

    public Registers() {
        registers = new long[15];
        registerMap = new HashMap<>();
        for (int x = 0; x < regs.length; x++) {
            registerMap.put(regs[x].substring(1), x);
        }
        for (String k : regs) {
            regList.add(new Register(k, registers[registerMap.get(k.substring(1))]));
        }
    }

    public boolean isRegister(String s) {
        return registerMap.containsKey(s);
    }

    public long getValueFromRegister(String s) {
        return registers[registerMap.get(s)];
    }

    public long getValueFromRegister(int i) {
        return registers[i];
    }

    public void setValueInRegister(String s, long a) {
        setValueInRegister(registerMap.get(s), a);
    }

    public void setValueInRegister(int i, long a) {
        registers[i] = a;
        regList.get(i).setHex(Strings.getHex(a, 4));
        regList.get(i).setDecimal(Strings.getDecimal(a, 5));
    }

    public void reset() {
        Arrays.fill(registers, 0);
        for (int i = 0; i < registers.length; i++) {
            regList.get(i).setHex(Strings.getHex(0, 4));
            regList.get(i).setDecimal(Strings.getDecimal(0, 5));
        }
    }

    public class Register {

        private final SimpleStringProperty name;
        private final SimpleStringProperty hex;
        private final SimpleStringProperty decimal;

        public Register(String na, long val) {
            name = new SimpleStringProperty(na);
            hex = new SimpleStringProperty(Strings.getHex(val, 4));
            decimal = new SimpleStringProperty(Strings.getDecimal(val, 5));
        }

        public String getName() {
            return name.get();
        }

        public void setName(String n) {
            name.set(n);
        }

        public String getHex() {
            return hex.get();
        }

        public void setHex(String h) {
            hex.set(h);
        }

        public String getDecimal() {
            return decimal.get();
        }

        public void setDecimal(String dec) {
            decimal.set(dec);
        }
    }

}
