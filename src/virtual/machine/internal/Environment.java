package virtual.machine.internal;

import java.util.Set;
import virtual.machine.execution.Interpreter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Callback;

/**
 *
 * @author aniket
 */
public class Environment {
    
    public class Condition {
        
        private final BooleanProperty state;
        private final StringProperty name;
        private final StringProperty stateValue;
        
        public Condition(boolean a, String name) {
            state = new SimpleBooleanProperty(a);
            this.name = new SimpleStringProperty(name);
            stateValue = new SimpleStringProperty("0");
            state.addListener((observable, oldValue, newValue) -> {
                stateValue.set(newValue ? "1" : "0");
            });
        }
        
        public String getName() {
            return name.get();
        }
        
        public void setName(String n) {
            name.set(n);
        }
        
        public String getStateValue() {
            return stateValue.get();
        }
        
        public void setStateValue(String n) {
            stateValue.set(n);
        }
        
        public boolean getState() {
            return state.get();
        }
        
        public void setState(boolean h) {
            state.set(h);
        }
        
    }
    
    private final IntegerProperty counter;
    private final IntegerProperty status;
    private final ObservableList<Condition> conditions = FXCollections.observableArrayList(
            new Condition(false, "Zero Flag"),
            new Condition(false, "Overflow Flag"),
            new Condition(false, "Sign Flag"),
            new Condition(false, "Carry Flag"));
    private boolean overrideBreakpoint = false;
    
    private final Registers register;
    private final Memory memory;
    
    private Callback<Void, Void> breakCall;
    
    public Environment() {
        register = new Registers();
        memory = new Memory(this, 4096);
        status = new SimpleIntegerProperty(0);
        counter = new SimpleIntegerProperty(0);
    }
    
    public ObservableList<Condition> getCodes() {
        return conditions;
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
    
    private Condition get(int a) {
        return conditions.get(a);
    }
    
    public BooleanProperty signProperty() {
        return get(2).state;
    }
    
    public BooleanProperty overflowProperty() {
        return get(1).state;
    }
    
    public BooleanProperty zeroProperty() {
        return get(0).state;
    }
    
    public BooleanProperty carryProperty() {
        return get(3).state;
    }
    
    public int isCarry() {
        return get(3).getState() ? 1 : 0;
    }
    
    public void setCarry(boolean v) {
        get(3).setState(v);
    }
    
    public int isSign() {
        return get(2).getState() ? 1 : 0;
    }
    
    public void setSign(boolean s) {
        get(2).setState(s);
    }
    
    public int isOverflow() {
        return get(1).getState() ? 1 : 0;
    }
    
    public void setOverflow(boolean o) {
        get(1).setState(o);
    }
    
    public int isZero() {
        return get(0).getState() ? 1 : 0;
    }
    
    public void setZero(boolean z) {
        get(0).setState(z);
    }
    
    public boolean overflow() {
        return get(1).getState();
    }
    
    public boolean zero() {
        return get(0).getState();
    }
    
    public boolean sign() {
        return get(2).getState();
    }
    
    public boolean carry() {
        return get(3).getState();
    }
}
