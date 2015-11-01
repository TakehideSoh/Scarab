package org.sat4j.pb.tools;

import java.util.BitSet;

import org.sat4j.core.VecInt;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;

public class AtMostCard {

    private final IVecInt lits;
    private final int degree;

    public AtMostCard(IVecInt atMostLits, int degree) {
        this.lits = atMostLits;
        this.degree = degree;
    }

    public AtMostCard(BitSet atMostLits, int degree, int offset) {
        this.lits = new VecInt(atMostLits.cardinality());
        int from = 0;
        int cur;
        while ((cur = atMostLits.nextSetBit(from)) != -1) {
            this.lits.push(cur + offset);
            from = cur + 1;
        }
        this.degree = degree;
    }

    public IVecInt getLits() {
        return lits;
    }

    public int getDegree() {
        return degree;
    }

    public AtLeastCard toAtLeast() {
        IVecInt atLeastLits = new VecInt(this.lits.size());
        for (IteratorInt it = this.lits.iterator(); it.hasNext();)
            atLeastLits.push(-it.next());
        int atLeastDegree = this.lits.size() - this.degree;
        return new AtLeastCard(atLeastLits, atLeastDegree);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (IteratorInt it = this.lits.iterator(); it.hasNext();) {
            sb.append(it.next());
            sb.append(" + ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("<= ");
        sb.append(this.degree);
        return sb.toString();
    }

}
