package org.sat4j.pb.tools;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CombinationIterator implements Iterable<Set<Integer>>,
        Iterator<Set<Integer>> {

    /* contains a combination of indexes ; ascending order */
    private int indexes[] = null;

    /* contains items ; ascending order */
    private int items[] = null;

    private final int combSize;

    private final int itemsSize;

    private boolean hasNext = true;

    public CombinationIterator(int combSize, int itemsSize) {
        this.combSize = combSize;
        this.itemsSize = itemsSize;
        this.items = new int[this.itemsSize];
        for (int i = 0; i < this.itemsSize; ++i) {
            this.items[i] = i;
        }
        computeNext();
    }

    public CombinationIterator(int combSize, Set<Integer> items) {
        this.combSize = combSize;
        this.itemsSize = items.size();
        this.items = new int[this.itemsSize];
        int i = 0;
        for (Integer item : items) {
            this.items[i++] = item;
        }
        computeNext();
    }

    public CombinationIterator(int combSize, BitSet items) {
        this(combSize, items, 0);
    }

    public CombinationIterator(int combSize, BitSet items, int offset) {
        this.combSize = combSize;
        this.itemsSize = items.cardinality();
        this.items = new int[this.itemsSize];
        int index = 0;
        for (int i = items.nextSetBit(0); i >= 0; i = items.nextSetBit(i + 1)) {
            this.items[index++] = i + offset;
        }
        computeNext();
    }

    private void computeNext() {
        if (this.indexes == null) {
            this.indexes = new int[this.combSize];
            for (int i = 0; i < this.combSize; ++i) {
                this.indexes[i] = i;
            }
            return;
        }
        int j;
        for (j = this.combSize - 1; j >= 0; --j) {
            ++this.indexes[j];
            if (this.indexes[j] == this.itemsSize - this.combSize + j + 1) {
                if (j == 0) {
                    this.hasNext = false;
                    return;
                }
            } else {
                break;
            }
        }
        for (int k = j + 1; k < this.combSize; ++k) {
            this.indexes[k] = this.indexes[k - 1] + 1;
        }
    }

    public Iterator<Set<Integer>> iterator() {
        return this;
    }

    public boolean hasNext() {
        return this.hasNext;
    }

    public Set<Integer> next() {
        Set<Integer> nextSet = new HashSet<Integer>();
        for (Integer i : this.indexes) {
            nextSet.add(this.items[i]);
        }
        computeNext();
        return nextSet;
    }

    public BitSet nextBitSet() {
        BitSet nextSet = new BitSet();
        for (Integer i : this.indexes) {
            nextSet.set(this.items[i]);
        }
        computeNext();
        return nextSet;
    }

    public void remove() {
        // TODO Auto-generated method stub

    }
}