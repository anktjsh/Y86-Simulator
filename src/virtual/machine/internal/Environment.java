
package virtual.machine.internal;

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

    private final Registers register;
    private final Memory memory;

    public Environment() {
        register = new Registers();
        memory = new Memory(this, 4096);
        status = new SimpleIntegerProperty(0);
        counter = new SimpleIntegerProperty(0);
        sign = new SimpleBooleanProperty(false);
        overflow = new SimpleBooleanProperty(false);
        zero = new SimpleBooleanProperty(false);
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
                return "INS";
            default:
                return "ADR";
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

    public void run(Callback<Void, Void> call) {
        while (isRunning()) {
            nextInstruction(call);
        }
    }

    public void reset() {
        counter.set(0);
        status.set(0);
    }

    public void nextInstruction(Callback<Void, Void> call) {
        Interpreter ip = Interpreter.getInstance(this, memory);
        if (isRunning()) {
            if (hasJumped()) {
                jumped = false;
                counter.set((int) getJumpLocation());
            }
            byte a = memory.getByte(counter.get());
            counter.set(counter.get() + 1);
            counter.set(counter.get() + ip.interpret(a, counter.get()));
        } else {
            status.set(1);
        }
        call.call(null);
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
