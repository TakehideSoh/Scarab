package org.sat4j.pb.tools;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.sat4j.core.VecInt;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;

public class AtLeastCard {

    private final IVecInt lits;
    private final int degree;

    public AtLeastCard(IVecInt atLeastLits, int degree) {
        this.lits = new VecInt(atLeastLits.size());
        atLeastLits.copyTo(lits);
        this.degree = degree;
    }

    public AtLeastCard(BitSet atLeastLits, int degree, int offset) {
        this.lits = new VecInt(atLeastLits.cardinality());
        int from = 0;
        int cur;
        while ((cur = atLeastLits.nextSetBit(from)) != -1) {
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

    public AtMostCard toAtMost() {
        IVecInt atMostLits = new VecInt(this.lits.size());
        for (IteratorInt it = this.lits.iterator(); it.hasNext();)
            atMostLits.push(-it.next());
        int atMostDegree = this.lits.size() - this.degree;
        return new AtMostCard(atMostLits, atMostDegree);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (IteratorInt it = this.lits.iterator(); it.hasNext();) {
            sb.append(it.next());
            sb.append(" + ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(">= ");
        sb.append(this.degree);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + degree;
        Set<Integer> litsSet = new HashSet<Integer>();
        for (IteratorInt it = lits.iterator(); it.hasNext();)
            litsSet.add(it.next());
        result = prime * result + litsSet.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AtLeastCard other = (AtLeastCard) obj;
        if (degree != other.degree)
            return false;
        if (lits == null) {
            if (other.lits != null)
                return false;
        }
        Set<Integer> litsSet1 = new HashSet<Integer>();
        for (IteratorInt it = lits.iterator(); it.hasNext();)
            litsSet1.add(it.next());
        Set<Integer> litsSet2 = new HashSet<Integer>();
        for (IteratorInt it = other.lits.iterator(); it.hasNext();)
            litsSet2.add(it.next());
        if (!litsSet1.equals(litsSet2))
            return false;
        return true;
    }

}
