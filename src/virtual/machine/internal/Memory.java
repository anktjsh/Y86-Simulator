package virtual.machine.internal;

import virtual.machine.core.Strings;
import java.util.HashMap;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 *
 * @author aniket
 */
public class Memory {

    private final ObservableMap<Integer, Integer> memory;
    private final ObservableList<Data> data;
    private final String emptyHex = "0x00000000";
    private final int capacity;
    private final Environment environ;

    public Memory(Environment e, int initialSize) {
        environ = e;
        capacity = initialSize;
        memory = FXCollections.observableMap(new HashMap<>());
        data = FXCollections.observableArrayList();
        memory.addListener((MapChangeListener.Change<? extends Integer, ? extends Integer> change) -> {
            if (change.wasRemoved()) {
                data.get(change.getKey() >> 2).setValue(emptyHex);
            }
            if (change.wasAdded()) {
                if (change.getKey() >> 2 <= data.size()) {
                    for (int x = data.size() << 2; x <= change.getKey(); x += 4) {
                        data.add(new Data(x, 0));
                    }
                }
                data.get(change.getKey() >> 2).setValue(Strings.getHex(change.getValueAdded(), 8));
            }
        });
        for (int x = 0; x < capacity; x += 4) {
            memory.put(x, 0);
        }
    }

    public long occupiedSpace() {
        return memory.size() << 2;
    }

    public final void reset() {
        memory.clear();
    }

    public byte getByte(int address) {
        int rem = address & 3;
        address = address - rem;
        rem = 24 - (rem << 3);
        Integer b = memory.get(address);
        return (byte) (b == null ? 0 : ((b >>> rem) & 0xFF));
    }

    public void putByte(int address, byte value) {
        if (address < capacity) {
            int rem = address & 3;
            address = address - rem;
            rem = 24 - (rem << 3);
            Integer b = memory.get(address);
            if (b == null) {
                b = 0;
            }
            int mask = (b ^ (value << rem)) & (0xFF << rem);
            memory.put(address, mask ^ b);
        } else {
            environ.setStatus(2);
        }
    }

    public ObservableList<Data> getData() {
        return data;
    }

    public class Data {

        private final SimpleStringProperty address;
        private final SimpleStringProperty value;

        private Data(int addr, int val) {
            address = new SimpleStringProperty(Strings.getHex(addr, 4));
            value = new SimpleStringProperty(Strings.getHex(val, 8));
        }

        public String getAddress() {
            return address.get();
        }

        public void setAddress(String adds) {
            address.set(adds);
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String adds) {
            value.set(adds);
        }

    }
}
