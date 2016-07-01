package org.sat4j.pb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Map;

public class ObjectiveFunctionComparator implements Comparator<Integer>,
        Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final Map<Integer, BigInteger> obj;

    public ObjectiveFunctionComparator(ObjectiveFunction objf) {
        this.obj = objf.toMap();
    }

    public int compare(Integer o1, Integer o2) {
        BigInteger b1 = obj.get(o1);
        BigInteger b2 = obj.get(o2);
        if (b2 == null) {
            if (b1 == null) {
                return 0;
            }
            return -b1.intValue();
        }
        return b2.compareTo(b1);
    }
}
