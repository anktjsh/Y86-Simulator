package virtual.machine.execution;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import virtual.machine.internal.Environment;
import virtual.machine.internal.Memory;

/**
 *
 * @author aniket
 */
public class Interpreter {

    private Memory memory;
    private Environment environ;

    public Interpreter(Environment en, Memory m) {
        environ = en;
        memory = m;
    }

    private static Interpreter interpret;

    public static Interpreter getInstance(Environment en, Memory m) {
        if (interpret == null) {
            interpret = new Interpreter(en, m);
        }
        interpret.environ = en;
        interpret.memory = m;
        return interpret;
    }

    public int interpret(byte a, int programCount) {
        int result = 0;
        byte regs, reg2, reg1;
        long temp;
        ByteBuffer buff;
        switch (a) {
            case 0x00:
                environ.setStatus(1);
                break;
            case 0x10:
                break;
            case 0x21:
                if (environ.zero() || (environ.overflow() != environ.sign())) {
                    move(programCount);
                }
                result = 1;
                break;
            case 0x22:
                if (environ.overflow() != environ.sign()) {
                    move(programCount);
                }
                result = 1;
                break;
            case 0x23:
                if (environ.zero()) {
                    move(programCount);
                }
                result = 1;
                break;
            case 0x24:
                if (!environ.zero()) {
                    move(programCount);
                }
                result = 1;
                break;
            case 0x25:
                if (environ.sign() == environ.overflow()) {
                    move(programCount);
                }
                result = 1;
                break;
            case 0x26:
                if (environ.sign() == environ.overflow() && environ.zero()) {
                    move(programCount);
                }
                result = 1;
                break;
            case 0x20:
                move(programCount);
                result = 1;
                break;
            case 0x30:
                reg1 = environ.getMemory().getByte(programCount);
                ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
                for (temp = programCount + 1; temp < programCount + 9; temp++) {
                    buf.put(environ.getMemory().getByte((int) temp));
                }
                buf.flip();
                environ.getRegister().setValueInRegister(reg1 & 0xF, reverse(buf).getLong());
                result = 9;
                break;
            case 0x40:
                regs = environ.getMemory().getByte(programCount);
                long val = environ.getRegister().getValueFromRegister(regs >>> 4);
                buff = ByteBuffer.allocate(Long.BYTES);
                for (temp = programCount + 1; temp < programCount + 9; temp++) {
                    buff.put(memory.getByte((int) temp));
                }
                buff.flip();
                placeInMemory(val, environ.getRegister().getValueFromRegister(regs & 0xF) + reverse(buff).getLong());
                result = 9;
                break;
            case 0x50:
                regs = environ.getMemory().getByte(programCount);
                buff = ByteBuffer.allocate(Long.BYTES);
                for (temp = programCount + 1; temp < programCount + 9; temp++) {
                    buff.put(memory.getByte((int) temp));
                }
                buff.flip();
                long r = environ.getRegister().getValueFromRegister(regs >>> 4) + reverse(buff).getLong();
                getFromMemory(regs & 0xF, (int) r);
                result = 9;
                break;
            case 0x60:
            case 0x61:
            case 0x62:
            case 0x63:
            case 0x64:
            case 0x65:
            case 0x66:
            case 0x67:
            case 0x68:
            case 0x69:
            case 0x6A:
                regs = memory.getByte(programCount);
                reg2 = (byte) (regs & 0xF);
                reg1 = (byte) ((regs >>> 4) & 0xF);
                operation(a, reg2, environ.getRegister().getValueFromRegister(reg1),
                        environ.getRegister().getValueFromRegister(reg2));
                result = 1;
                break;
            case 0x70:
                jump(programCount);
                result = 8;
                break;
            case 0x71:
                if (environ.zero() || (environ.overflow() != environ.sign())) {
                    jump(programCount);
                }
                result = 8;
                break;
            case 0x72:
                if (environ.overflow() != environ.sign()) {
                    jump(programCount);
                }
                result = 8;
                break;
            case 0x73:
                if (environ.zero()) {
                    jump(programCount);
                }
                result = 8;
                break;
            case 0x74:
                if (!environ.zero()) {
                    jump(programCount);
                }
                result = 8;
                break;
            case 0x75:
                if (environ.sign() == environ.overflow()) {
                    jump(programCount);
                }
                result = 8;
                break;
            case 0x76:
                if (environ.sign() == environ.overflow() && environ.zero()) {
                    jump(programCount);
                }
                result = 8;
                break;
            case (byte) 0x80:
                result = 8;
                if (pushToStack(environ.programCounter() + 8)) {
                    jump(programCount);
                } else {
                    environ.setStatus(3);
                }
                break;
            case (byte) 0x90:
                long rsp = environ.getRegister().getValueFromRegister("rsp");
                buff = ByteBuffer.allocate(Long.BYTES);
                for (temp = rsp; temp < rsp + 8; temp++) {
                    buff.put(memory.getByte((int) temp));
                }
                buff.flip();
                environ.getRegister().setValueInRegister("rsp", environ.getRegister().getValueFromRegister("rsp") + 8);
                environ.setJumped(true);
                environ.setJumpLocation(reverse(buff).getLong());
                break;
            case (byte) 0xA0:
                if (!pushToStack(environ.getRegister().getValueFromRegister(memory.getByte(programCount) >>> 4))) {
                    environ.setStatus(3);
                }
                result = 1;
                break;
            case (byte) 0xB0:
                popFromStack(memory.getByte(programCount) >>> 4);
                result = 1;
                break;
            case (byte) 0xC0:
            case (byte) 0xC1:
            case (byte) 0xC2:
            case (byte) 0xC3:
            case (byte) 0xC4:
                singleOp(a, (byte) (memory.getByte(programCount) >>> 4));
                break;
            default:
                environ.setStatus(3);
                break;
        }
        return result;
    }

    private boolean pushToStack(long value) {
        if (environ.getRegister().getValueFromRegister("rsp") < 8) {
            return false;
        }
        environ.getRegister().setValueInRegister("rsp", environ.getRegister().getValueFromRegister("rsp") - 8);
        long rsp = environ.getRegister().getValueFromRegister("rsp");
        placeInMemory(value, rsp);
        return true;
    }

    private void popFromStack(int register) {
        long rsp = environ.getRegister().getValueFromRegister("rsp");
        getFromMemory(register, (int) rsp);
        environ.getRegister().setValueInRegister("rsp", environ.getRegister().getValueFromRegister("rsp") + 8);
    }

    private void placeInMemory(long value, long rsp) {
        ByteBuffer buff = ByteBuffer.allocate(Long.BYTES);
        buff.putLong(value);
        byte[] arr = buff.array();
        Compiler.reverse(arr);
        for (byte b : arr) {
            memory.putByte((int) rsp, b);
            rsp++;
        }
    }

    private void getFromMemory(int register, int rsp) {
        ByteBuffer buff = ByteBuffer.allocate(Long.BYTES);
        for (long temp = rsp; temp < rsp + 8; temp++) {
            buff.put(memory.getByte((int) temp));
        }
        buff.flip();
        environ.getRegister().setValueInRegister(register, reverse(buff).getLong());
    }

    private void jump(int programCount) {
        ByteBuffer buff = ByteBuffer.allocate(Long.BYTES);
        for (int temp = programCount; temp < programCount + 8; temp++) {
            buff.put(memory.getByte(temp));
        }
        environ.setJumped(true);
        buff.flip();
        environ.setJumpLocation(reverse(buff).getLong());
    }

    private ByteBuffer reverse(ByteBuffer b) {
        byte[] arr = b.array();
        ByteBuffer bu = ByteBuffer.allocate(Long.BYTES);
        Compiler.reverse(arr);
        bu.put(arr);
        bu.flip();
        return bu;
    }

    private void move(int address) {
        byte regs = memory.getByte(address);
        byte reg2 = (byte) (regs & 0xF);
        byte reg1 = (byte) ((regs >>> 4) & 0xF);
        environ.getRegister().setValueInRegister(reg2, environ.getRegister().getValueFromRegister(reg1));
    }

    private void singleOp(byte op, byte reg) {
        long val = environ.getRegister().getValueFromRegister(reg);
        boolean overflo = false;
        switch (op) {
            case (byte) 0xC0:
                val = ~val;
                break;
            case (byte) 0xC1:
                val = -val;
                break;
            case (byte) 0xC2:
                val++;
                if (val == Long.MIN_VALUE) {
                    overflo = true;
                }
                break;
            case (byte) 0xC3:
                val--;
                if (val == Long.MAX_VALUE) {
                    overflo = true;
                }
                break;
            case (byte) 0xC4:
                val = bang(val);
                break;
        }
        environ.getRegister().setValueInRegister(reg, val);
        if (val == 0) {
            environ.setZero(true);
        } else {
            environ.setZero(false);
        }
        if (val < 0) {
            environ.setSign(true);
        } else {
            environ.setSign(false);
        }
        environ.setOverflow(overflo);
    }

    private static long bang(long x) {
        return ((x >> 63) | ((~x + 1) >> 63)) + 1;
    }

    private void operation(byte op, byte reg2, long a, long b) {
        long result;
        long asign = a >>> 63;
        long bsign = b >>> 63;
        long lsign;
        boolean overflo = false;
        switch (op) {
            case 0x60:
                result = b + a;
                lsign = result >>> 63;
                if (asign == bsign && asign != lsign) {
                    overflo = true;
                }
                break;
            case 0x61:
                result = b - a;
                lsign = result >>> 63;
                if (asign == lsign && asign != bsign) {
                    overflo = true;
                }
                break;
            case 0x62:
                result = b & a;
                break;
            case 0x63:
                result = b ^ a;
                break;
            case 0x64:
                result = b * a;
                BigInteger mu = BigInteger.valueOf(b).multiply(BigInteger.valueOf(a));
                if ((mu.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) < 0) || (mu.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0)) {
                    overflo = true;
                }
                break;
            case 0x65:
                if (a == 0) {
                    environ.setStatus(3);
                    result = 0;
                    break;
                }
                result = b / a;
                if (b == 0x80000000 && a == -1) {
                    overflo = true;
                }
                break;
            case 0x66:
                result = b % a;
                break;
            case 0x67:
                result = b >> a;
                break;
            case 0x68:
                result = b >>> a;
                break;
            case 0x69:
                result = b << a;
                break;
            case 0x6A:
                result = b | a;
                break;
            default:
                result = 0;
        }
        environ.getRegister().setValueInRegister(reg2, result);
        if (result == 0) {
            environ.setZero(true);
        } else {
            environ.setZero(false);
        }
        if (result < 0) {
            environ.setSign(true);
        } else {
            environ.setSign(false);
        }
        environ.setOverflow(overflo);
    }
}
