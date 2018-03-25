package virtual.machine.internal;

import java.util.Set;
import virtual.machine.execution.Interpreter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Callback;

/**
 *
 * @author aniket
 */
public class Environment {

    private final IntegerProperty counter;
    private final IntegerProperty status;
    private final BooleanProperty sign, overflow, zero;
    private boolean overrideBreakpoint = false;

    private final Registers register;
    private final Memory memory;

    private Callback<Void, Void> breakCall;

    public Environment() {
        register = new Registers();
        memory = new Memory(this, 4096);
        status = new SimpleIntegerProperty(0);
        counter = new SimpleIntegerProperty(0);
        sign = new SimpleBooleanProperty(false);
        overflow = new SimpleBooleanProperty(false);
        zero = new SimpleBooleanProperty(false);
    }

    public void setBreakCall(Callback<Void, Void> v) {
        breakCall = v;
    }

    public void override() {
        overrideBreakpoint = true;
        setStatus(0);
    }

    public int programCounter() {
        return counter.get();
    }

    public IntegerProperty counter() {
        return counter;
    }

    public boolean isRunning() {
        return status.get() == 0;
    }

    public boolean hasError() {
        return !isRunning();
    }

    public IntegerProperty status() {
        return status;
    }

    public String getStatus() {
        switch (status.get()) {
            case 0:
                return "AOK";
            case 1:
                return "HLT";
            case 2:
                return "ADR";
            default:
                return "INS";
        }
    }

    public void setStatus(int i) {
        status.set(i);
    }

    public Registers getRegister() {
        return register;
    }

    public Memory getMemory() {
        return memory;
    }

    public void run(Callback<Void, Void> call, Set<Integer> breakpoints) {
        while (isRunning()) {
            nextInstruction(call, breakpoints);
        }
    }

    public void reset() {
        counter.set(0);
        status.set(0);
    }

    public void nextInstruction(Callback<Void, Void> call, Set<Integer> breakpoints) {
        Interpreter ip = Interpreter.getInstance(this, memory);
        if (isRunning()) {
            if (!breakpoint(counter.get(), breakpoints)) {
                byte a = memory.getByte(counter.get());
                counter.set(counter.get() + 1);
                int end = ip.interpret(a, counter.get());
                if (hasJumped()) {
                    jumped = false;
                    counter.set((int) getJumpLocation());
                } else {
                    counter.set(counter.get() + end);
                }
            }
        } else {
            status.set(1);
        }
        call.call(null);
    }

    private boolean breakpoint(int loc, Set<Integer> breakpoints) {
        if (breakpoints.contains(loc)) {
            if (overrideBreakpoint) {
                overrideBreakpoint = false;
            } else {
                setStatus(1);
                breakCall.call(null);
                return true;
            }
        }
        return false;
    }

    private boolean jumped = false;
    private long jumpLoc = 0L;

    private boolean hasJumped() {
        return jumped;
    }

    public void setJumped(boolean b) {
        jumped = b;
    }

    private long getJumpLocation() {
        return jumpLoc;
    }

    public void setJumpLocation(long a) {
        jumpLoc = a;
    }

    public BooleanProperty signProperty() {
        return sign;
    }

    public BooleanProperty overflowProperty() {
        return overflow;
    }

    public BooleanProperty zeroProperty() {
        return zero;
    }

    public int isSign() {
        return sign.get() ? 1 : 0;
    }

    public void setSign(boolean s) {
        sign.set(s);
    }

    public int isOverflow() {
        return overflow.get() ? 1 : 0;
    }

    public void setOverflow(boolean o) {
        overflow.set(o);
    }

    public int isZero() {
        return zero.get() ? 1 : 0;
    }

    public void setZero(boolean z) {
        zero.set(z);
    }

    public boolean overflow() {
        return overflow.get();
    }

    public boolean zero() {
        return zero.get();
    }

    public boolean sign() {
        return sign.get();
    }
}
