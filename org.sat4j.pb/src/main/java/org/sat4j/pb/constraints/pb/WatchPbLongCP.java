/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004, 2012 Artois University and CNRS
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU Lesser General Public License Version 2.1 or later (the
 * "LGPL"), in which case the provisions of the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting
 * the provisions above and replace them with the notice and other provisions
 * required by the LGPL. If you do not delete the provisions above, a recipient
 * may use your version of this file under the terms of the EPL or the LGPL.
 *
 * Based on the original MiniSat specification from:
 *
 * An extensible SAT solver. Niklas Een and Niklas Sorensson. Proceedings of the
 * Sixth International Conference on Theory and Applications of Satisfiability
 * Testing, LNCS 2919, pp 502-518, 2003.
 *
 * See www.minisat.se for the original solver in C++.
 *
 * Contributors:
 *   CRIL - initial API and implementation
 *******************************************************************************/
package org.sat4j.pb.constraints.pb;

import java.io.Serializable;
import java.math.BigInteger;

import org.sat4j.core.LiteralsUtils;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.constraints.cnf.Lits;
import org.sat4j.minisat.core.ILits;
import org.sat4j.minisat.core.Undoable;
import org.sat4j.specs.Constr;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.MandatoryLiteralListener;
import org.sat4j.specs.Propagatable;
import org.sat4j.specs.UnitPropagationListener;
import org.sat4j.specs.VarMapper;

public abstract class WatchPbLongCP implements IWatchPb, Propagatable,
        Undoable, Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static final int LIMIT_SELECTION_SORT = 15;

    /**
     * constraint activity
     */
    protected double activity;
    /** Constructor used for original constraints. */

    /**
     * coefficients of the literals of the constraint
     */
    protected BigInteger[] bigCoefs;

    /**
     * degree of the pseudo-boolean constraint
     */
    protected BigInteger bigDegree;

    /**
     * coefficients of the literals of the constraint
     */
    protected long[] coefs;

    protected long sumcoefs;

    /**
     * degree of the pseudo-boolean constraint
     */
    protected long degree;

    /**
     * literals of the constraint
     */
    protected int[] lits;

    /**
     * true if the constraint is a learned constraint
     */
    protected boolean learnt = false;

    /**
     * constraint's vocabulary
     */
    protected ILits voc;

    /**
     * This constructor is only available for the serialization.
     */
    WatchPbLongCP() {
    }

    /** Constructor used for learnt constraints. */
    WatchPbLongCP(IDataStructurePB mpb) {
        int size = mpb.size();
        this.lits = new int[size];
        this.bigCoefs = new BigInteger[size];
        mpb.buildConstraintFromMapPb(this.lits, this.bigCoefs);
        assert mpb.isLongSufficient();
        this.coefs = toLong(this.bigCoefs);
        this.sumcoefs = 0;
        for (long c : this.coefs) {
            this.sumcoefs += c;
        }
        this.bigDegree = mpb.getDegree();
        this.degree = this.bigDegree.longValue();
        this.learnt = true;
        // arrays are sorted by decreasing coefficients
        sort();
    }

    /** Constructor used for original constraints. */
    WatchPbLongCP(int[] lits, BigInteger[] coefs, BigInteger degree,
            BigInteger sumCoefs) { // NOPMD
        this.lits = lits;
        this.coefs = toLong(coefs);
        this.degree = degree.longValue();
        this.bigCoefs = coefs;
        this.bigDegree = degree;
        this.sumcoefs = sumCoefs.longValue();
        // arrays are sorted by decreasing coefficients
        sort();
    }

    public static long[] toLong(BigInteger[] bigValues) {
        long[] res = new long[bigValues.length];
        for (int i = 0; i < res.length; i++) {
            assert bigValues[i].bitLength() < Long.SIZE;
            res[i] = bigValues[i].longValue();
            assert res[i] >= 0;
        }
        return res;
    }

    /**
     * This predicate tests wether the constraint is assertive at decision level
     * dl
     * 
     * @param dl
     * @return true iff the constraint is assertive at decision level dl.
     */
    public boolean isAssertive(int dl) {
        long slack = 0;
        for (int i = 0; i < this.lits.length; i++) {
            if (this.coefs[i] > 0
                    && (!this.voc.isFalsified(this.lits[i]) || this.voc
                            .getLevel(this.lits[i]) >= dl)) {
                slack = slack + this.coefs[i];
            }
        }
        slack = slack - this.degree;
        if (slack < 0) {
            return false;
        }
        for (int i = 0; i < this.lits.length; i++) {
            if (this.coefs[i] > 0
                    && (this.voc.isUnassigned(this.lits[i]) || this.voc
                            .getLevel(this.lits[i]) >= dl)
                    && slack < this.coefs[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * compute the reason for the assignment of a literal
     * 
     * @param p
     *            a falsified literal (or Lit.UNDEFINED)
     * @param outReason
     *            list of falsified literals for which the negation is the
     *            reason of the assignment
     * @see org.sat4j.specs.Constr#calcReason(int, IVecInt)
     */
    public void calcReason(int p, IVecInt outReason) {
        long sumfalsified = 0;
        final int[] mlits = this.lits;
        for (int i = 0; i < mlits.length; i++) {
            int q = mlits[i];
            if (this.voc.isFalsified(q)) {
                outReason.push(q ^ 1);
                sumfalsified += this.coefs[i];
                if (this.sumcoefs - sumfalsified < this.degree) {
                    return;
                }
            }
        }
    }

    protected abstract void computeWatches() throws ContradictionException;

    protected abstract void computePropagation(UnitPropagationListener s)
            throws ContradictionException;

    /**
     * to obtain the i-th literal of the constraint
     * 
     * @param i
     *            index of the literal
     * @return the literal
     */
    public int get(int i) {
        return this.lits[i];
    }

    /**
     * to obtain the activity value of the constraint
     * 
     * @return activity value of the constraint
     * @see org.sat4j.specs.Constr#getActivity()
     */
    public double getActivity() {
        return this.activity;
    }

    /**
     * increase activity value of the constraint
     * 
     * @see org.sat4j.specs.Constr#incActivity(double)
     */
    public void incActivity(double claInc) {
        if (this.learnt) {
            this.activity += claInc;
        }
    }

    public void setActivity(double d) {
        if (this.learnt) {
            this.activity = d;
        }
    }

    /**
     * compute the slack of the current constraint slack = poss - degree of the
     * constraint
     * 
     * @return la marge
     */
    public long slackConstraint() {
        return computeLeftSide() - this.degree;
    }

    /**
     * compute the slack of a described constraint slack = poss - degree of the
     * constraint
     * 
     * @param theCoefs
     *            coefficients of the constraint
     * @param theDegree
     *            degree of the constraint
     * @return slack of the constraint
     */
    public long slackConstraint(long[] theCoefs, long theDegree) {
        return computeLeftSide(theCoefs) - theDegree;
    }

    /**
     * compute the sum of the coefficients of the satisfied or non-assigned
     * literals of a described constraint (usually called poss)
     * 
     * @param coefs
     *            coefficients of the constraint
     * @return poss
     */
    public long computeLeftSide(long[] theCoefs) {
        long poss = 0;
        // for each literal
        for (int i = 0; i < this.lits.length; i++) {
            if (!this.voc.isFalsified(this.lits[i])) {
                assert theCoefs[i] >= 0;
                poss = poss + theCoefs[i];
            }
        }
        return poss;
    }

    /**
     * compute the sum of the coefficients of the satisfied or non-assigned
     * literals of a described constraint (usually called poss)
     * 
     * @param coefs
     *            coefficients of the constraint
     * @return poss
     */
    public BigInteger computeLeftSide(BigInteger[] theCoefs) {
        BigInteger poss = BigInteger.ZERO;
        // for each literal
        for (int i = 0; i < this.lits.length; i++) {
            if (!this.voc.isFalsified(this.lits[i])) {
                assert theCoefs[i].signum() >= 0;
                poss = poss.add(theCoefs[i]);
            }
        }
        return poss;
    }

    /**
     * compute the sum of the coefficients of the satisfied or non-assigned
     * literals of the current constraint (usually called poss)
     * 
     * @return poss
     */
    public long computeLeftSide() {
        return computeLeftSide(this.coefs);
    }

    /**
     * tests if the constraint is still satisfiable.
     * 
     * this method is only called in assertions.
     * 
     * @return the constraint is satisfiable
     */
    protected boolean isSatisfiable() {
        return computeLeftSide() >= this.degree;
    }

    /**
     * is the constraint a learnt constraint ?
     * 
     * @return true if the constraint is learnt, else false
     * @see org.sat4j.specs.IConstr#learnt()
     */
    public boolean learnt() {
        return this.learnt;
    }

    /**
     * The constraint is the reason of a unit propagation.
     * 
     * @return true
     */
    public boolean locked() {
        for (int p : this.lits) {
            if (this.voc.getReason(p) == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * ppcm : least common multiple for two integers (plus petit commun
     * multiple)
     * 
     * @param a
     *            one integer
     * @param b
     *            the other integer
     * @return the least common multiple of a and b
     */
    protected static BigInteger ppcm(BigInteger a, BigInteger b) {
        return a.divide(a.gcd(b)).multiply(b);
    }

    /**
     * to re-scale the activity of the constraint
     * 
     * @param d
     *            adjusting factor
     */
    public void rescaleBy(double d) {
        this.activity *= d;
    }

    void selectionSort(int from, int to) {
        int i, j, bestIndex;
        long tmp;
        BigInteger bigTmp;
        int tmp2;

        for (i = from; i < to - 1; i++) {
            bestIndex = i;
            for (j = i + 1; j < to; j++) {
                if (this.coefs[j] > this.coefs[bestIndex]
                        || this.coefs[j] == this.coefs[bestIndex]
                        && this.lits[j] > this.lits[bestIndex]) {
                    bestIndex = j;
                }
            }
            tmp = this.coefs[i];
            this.coefs[i] = this.coefs[bestIndex];
            this.coefs[bestIndex] = tmp;
            bigTmp = this.bigCoefs[i];
            this.bigCoefs[i] = this.bigCoefs[bestIndex];
            this.bigCoefs[bestIndex] = bigTmp;
            tmp2 = this.lits[i];
            this.lits[i] = this.lits[bestIndex];
            this.lits[bestIndex] = tmp2;
            assert this.coefs[i] >= 0;
            assert this.coefs[bestIndex] >= 0;
        }
    }

    /**
     * the constraint is learnt
     */
    public void setLearnt() {
        this.learnt = true;
    }

    /**
     * simplify the constraint (if it is satisfied)
     * 
     * @return true if the constraint is satisfied, else false
     */
    public boolean simplify() {
        long cumul = 0;

        int i = 0;
        while (i < this.lits.length && cumul < this.degree) {
            if (this.voc.isSatisfied(this.lits[i])) {
                // strong measure
                cumul = cumul + this.coefs[i];
            }
            i++;
        }

        return cumul >= this.degree;
    }

    public final int size() {
        return this.lits.length;
    }

    /**
     * sort coefficient and literal arrays
     */
    protected final void sort() {
        assert this.lits != null;
        if (this.coefs.length > 0) {
            this.sort(0, size());
        }
    }

    /**
     * sort partially coefficient and literal arrays
     * 
     * @param from
     *            index for the beginning of the sort
     * @param to
     *            index for the end of the sort
     */
    protected final void sort(int from, int to) {
        int width = to - from;
        if (width <= LIMIT_SELECTION_SORT) {
            selectionSort(from, to);
        } else {
            assert this.coefs.length == this.bigCoefs.length;
            int indPivot = width / 2 + from;
            long pivot = this.coefs[indPivot];
            int litPivot = this.lits[indPivot];
            long tmp;
            BigInteger bigTmp;
            int i = from - 1;
            int j = to;
            int tmp2;

            for (;;) {
                do {
                    i++;
                } while (this.coefs[i] > pivot || this.coefs[i] == pivot
                        && this.lits[i] > litPivot);
                do {
                    j--;
                } while (pivot > this.coefs[j] || this.coefs[j] == pivot
                        && this.lits[j] < litPivot);

                if (i >= j) {
                    break;
                }

                tmp = this.coefs[i];
                this.coefs[i] = this.coefs[j];
                this.coefs[j] = tmp;
                bigTmp = this.bigCoefs[i];
                this.bigCoefs[i] = this.bigCoefs[j];
                this.bigCoefs[j] = bigTmp;
                tmp2 = this.lits[i];
                this.lits[i] = this.lits[j];
                this.lits[j] = tmp2;
                assert this.coefs[i] >= 0;
                assert this.coefs[j] >= 0;
            }

            sort(from, i);
            sort(i, to);
        }

    }

    @Override
    public String toString() {
        StringBuffer stb = new StringBuffer();

        if (this.lits.length > 0) {
            for (int i = 0; i < this.lits.length; i++) {
                stb.append(" + ");
                stb.append(this.coefs[i]);
                stb.append(".");
                stb.append(Lits.toString(this.lits[i]));
                stb.append("[");
                stb.append(this.voc.valueToString(this.lits[i]));
                stb.append("@");
                stb.append(this.voc.getLevel(this.lits[i]));
                stb.append("]");
                stb.append(" ");
            }
            stb.append(">= ");
            stb.append(this.degree);
        }
        return stb.toString();
    }

    public void assertConstraint(UnitPropagationListener s) {
        long tmp = slackConstraint();
        for (int i = 0; i < this.lits.length; i++) {
            if (this.voc.isUnassigned(this.lits[i]) && tmp < this.coefs[i]) {
                boolean ret = s.enqueue(this.lits[i], this);
                assert ret;
            }
        }
    }

    public void assertConstraintIfNeeded(UnitPropagationListener s) {
        assertConstraint(s);
    }

    public void register() {
        assert this.learnt;
        try {
            computeWatches();
        } catch (ContradictionException e) {
            System.out.println(this);
            assert false;
        }
    }

    /**
     * to obtain the literals of the constraint.
     * 
     * @return a copy of the array of the literals
     */
    public int[] getLits() {
        int[] litsBis = new int[this.lits.length];
        System.arraycopy(this.lits, 0, litsBis, 0, this.lits.length);
        return litsBis;
    }

    public ILits getVocabulary() {
        return this.voc;
    }

    /**
     * compute an implied clause on the literals with the greater coefficients.
     * 
     * @return a vector containing the literals for this clause.
     */
    public IVecInt computeAnImpliedClause() {
        long cptCoefs = 0;
        int index = this.coefs.length;
        while (cptCoefs > this.degree && index > 0) {
            cptCoefs = cptCoefs + this.coefs[--index];
        }
        if (index > 0 && index < size() / 2) {
            IVecInt literals = new VecInt(index);
            for (int j = 0; j <= index; j++) {
                literals.push(this.lits[j]);
            }
            return literals;
        }
        return null;
    }

    public boolean coefficientsEqualToOne() {
        return false;
    }

    @Override
    public boolean equals(Object pb) {
        if (pb == null) {
            return false;
        }
        // this method should be simplified since now two constraints should
        // have
        // always
        // their literals in the same order
        try {

            WatchPbLongCP wpb = (WatchPbLongCP) pb;
            if (this.degree != wpb.degree
                    || this.coefs.length != wpb.coefs.length
                    || this.lits.length != wpb.lits.length) {
                return false;
            }
            int lit;
            boolean ok;
            for (int ilit = 0; ilit < this.coefs.length; ilit++) {
                lit = this.lits[ilit];
                ok = false;
                for (int ilit2 = 0; ilit2 < this.coefs.length; ilit2++) {
                    if (wpb.lits[ilit2] == lit) {
                        if (wpb.coefs[ilit2] != this.coefs[ilit]) {
                            return false;
                        }

                        ok = true;
                        break;

                    }
                }
                if (!ok) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        long sum = 0;
        for (int p : this.lits) {
            sum += p;
        }
        return (int) sum / this.lits.length;
    }

    public void forwardActivity(double claInc) {
        if (!this.learnt) {
            this.activity += claInc;
        }
    }

    public long[] getLongCoefs() {
        long[] coefsBis = new long[this.coefs.length];
        System.arraycopy(this.coefs, 0, coefsBis, 0, this.coefs.length);
        return coefsBis;
    }

    public BigInteger slackConstraint(BigInteger[] theCoefs,
            BigInteger theDegree) {
        return computeLeftSide(theCoefs).subtract(theDegree);
    }

    /**
     * to obtain the coefficient of the i-th literal of the constraint
     * 
     * @param i
     *            index of the literal
     * @return coefficient of the literal
     */
    public BigInteger getCoef(int i) {
        return this.bigCoefs[i];
    }

    /**
     * to obtain the coefficients of the constraint.
     * 
     * @return a copy of the array of the coefficients
     */
    public BigInteger[] getCoefs() {
        BigInteger[] coefsBis = new BigInteger[this.coefs.length];
        System.arraycopy(this.bigCoefs, 0, coefsBis, 0, this.coefs.length);
        return coefsBis;
    }

    /**
     * @return Returns the degree.
     */
    public BigInteger getDegree() {
        return this.bigDegree;
    }

    public boolean canBePropagatedMultipleTimes() {
        return true;
    }

    public Constr toConstraint() {
        return this;
    }

    public void calcReasonOnTheFly(int p, IVecInt trail, IVecInt outReason) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    public boolean propagatePI(MandatoryLiteralListener l, int p) {
        throw new UnsupportedOperationException("Not implemented yet!");

    }

    public boolean canBeSatisfiedByCountingLiterals() {
        return false;
    }

    public int requiredNumberOfSatisfiedLiterals() {
        throw new UnsupportedOperationException(
                "Not applicable for PB constraints");
    }

    public boolean isSatisfied() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public String toString(VarMapper mapper) {
        StringBuffer stb = new StringBuffer();

        if (this.lits.length > 0) {
            for (int i = 0; i < this.lits.length; i++) {
                stb.append(" + ");
                stb.append(this.coefs[i]);
                stb.append(".");
                stb.append(mapper.map(LiteralsUtils.toDimacs(this.lits[i])));
                stb.append("[");
                stb.append(this.voc.valueToString(this.lits[i]));
                stb.append("@");
                stb.append(this.voc.getLevel(this.lits[i]));
                stb.append("]");
                stb.append(" ");
            }
            stb.append(">= ");
            stb.append(this.degree);
        }
        return stb.toString();
    }
}
