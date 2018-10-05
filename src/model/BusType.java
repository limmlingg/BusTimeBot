package model;

import java.util.BitSet;

public class BusType {
    public enum Type {
        PUBLIC(0b1),
        NUS(0b10),
        NTU(0b100);

        private final int value;
        Type(int value) {
            this.value = value;
        }

        protected int getValue() {
            return value;
        }
    };

    private BitSet type;

    public BusType() {
        type = new BitSet();
    }

    /**
     * Sets a flag to true
     * @param type of the bus stop (Public, NUS, NTU)
     * @return True if successfully set, false otherwise
     */
    public boolean setTrue(Type type) {
        if (type != null) {
            this.type.set(type.getValue(), true);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the flag has been set. Returns false if type passed in is null.
     * @param type of the bus stop (Public, NUS, NTU)
     */
    public boolean isType(Type type) {
        if (type != null) {
            return (this.type.get(type.getValue()));
        } else {
            return false;
        }
    }

    /**
     * Returns a string representation of the object
     */
    public String toString() {
        StringBuilder s = new StringBuilder("{");
        for (Type type : Type.values()) {
            if (s.length() > 0) {
                s.append(", ");
            }
            s.append(type.name() + "=" + isType(type));
        }
        s.append("}");
        return s.toString();
    }
}
