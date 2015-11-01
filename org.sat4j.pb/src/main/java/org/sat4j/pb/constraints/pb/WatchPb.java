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
import org.sat4j.specs.IteratorInt;
import org.sat4j.specs.Propagatable;
import org.sat4j.specs.UnitPropagationListener;
import org.sat4j.specs.VarMapper;

/**
 * Abstract data structure for pseudo-boolean constraint with watched literals.
 * 
 * @author anne
 * 
 */
public abstract class WatchPb implements IWatchPb, Propagatable, Undoable,
        Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static final int LIMIT_SELECTION_SORT = 15;
    /**
     * constraint activity
     */
    protected double activity;

    /**
     * coefficients of the literals of the constraint
     */
    protected BigInteger[] coefs;

    protected BigInteger sumcoefs;

    /**
     * degree of the pseudo-boolean constraint
     */
    protected BigInteger degree;

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
    WatchPb() {
    }

    /** Constructor used for learnt constraints. */
    WatchPb(IDataStructurePB mpb) {
        int size = mpb.size();
        this.lits = new int[size];
        this.coefs = new BigInteger[size];
        mpb.buildConstraintFromMapPb(this.lits, this.coefs);

        this.degree = mpb.getDegree();
        this.sumcoefs = BigInteger.ZERO;
        for (BigInteger c : this.coefs) {
            this.sumcoefs = this.sumcoefs.add(c);
        }
        this.learnt = true;
        // arrays are sorted by decreasing coefficients
        sort();
    }

    /** Constructor used for original constraints. */
    WatchPb(int[] lits, BigInteger[] coefs, BigInteger degree,
            BigInteger sumCoefs) {
        this.lits = lits;
        this.coefs = coefs;
        this.degree = degree;
        this.sumcoefs = sumCoefs;
        // arrays are sorted by decreasing coefficients
        sort();
    }

    /**
     * This predicate tests wether the constraint is assertive at decision level
     * dl
     * 
     * @param dl
     * @return true iff the constraint is assertive at decision level dl.
     */
    public boolean isAssertive(int dl) {
        BigInteger slack = BigInteger.ZERO;
        for (int i = 0; i < this.lits.length; i++) {
            if (this.coefs[i].signum() > 0
                    && (!this.voc.isFalsified(this.lits[i]) || this.voc
                            .getLevel(this.lits[i]) >= dl)) {
                slack = slack.add(this.coefs[i]);
            }
        }
        slack = slack.subtract(this.degree);
        if (slack.signum() < 0) {
            return false;
        }
        for (int i = 0; i < this.lits.length; i++) {
            if (this.coefs[i].signum() > 0
                    && (this.voc.isUnassigned(this.lits[i]) || this.voc
                            .getLevel(this.lits[i]) >= dl)
                    && slack.compareTo(this.coefs[i]) < 0) {
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
        BigInteger sumfalsified = BigInteger.ZERO;
        final int[] mlits = this.lits;
        for (int i = 0; i < mlits.length; i++) {
            int q = mlits[i];
            if (this.voc.isFalsified(q)) {
                outReason.push(q ^ 1);
                sumfalsified = sumfalsified.add(this.coefs[i]);
                if (this.sumcoefs.subtract(sumfalsified).compareTo(this.degree) < 0) {
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
     * to obtain the coefficient of the i-th literal of the constraint
     * 
     * @param i
     *            index of the literal
     * @return coefficient of the literal
     */
    public BigInteger getCoef(int i) {
        return this.coefs[i];
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
    public BigInteger slackConstraint() {
        return computeLeftSide().subtract(this.degree);
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
    public BigInteger slackConstraint(BigInteger[] theCoefs,
            BigInteger theDegree) {
        return computeLeftSide(theCoefs).subtract(theDegree);
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
    public BigInteger computeLeftSide() {
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
        return computeLeftSide().compareTo(this.degree) >= 0;
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
        BigInteger tmp;
        int tmp2;

        for (i = from; i < to - 1; i++) {
            bestIndex = i;
            for (j = i + 1; j < to; j++) {
                if (this.coefs[j].compareTo(this.coefs[bestIndex]) > 0
                        || this.coefs[j].equals(this.coefs[bestIndex])
                        && this.lits[j] > this.lits[bestIndex]) {
                    bestIndex = j;
                }
            }
            tmp = this.coefs[i];
            this.coefs[i] = this.coefs[bestIndex];
            this.coefs[bestIndex] = tmp;
            tmp2 = this.lits[i];
            this.lits[i] = this.lits[bestIndex];
            this.lits[bestIndex] = tmp2;
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
        BigInteger cumul = BigInteger.ZERO;

        int i = 0;
        while (i < this.lits.length && cumul.compareTo(this.degree) < 0) {
            if (this.voc.isSatisfied(this.lits[i])) {
                // strong measure
                cumul = cumul.add(this.coefs[i]);
            }
            i++;
        }

        return cumul.compareTo(this.degree) >= 0;
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
            BigInteger buffInt = this.coefs[0];
            for (int i = 1; i < this.coefs.length; i++) {
                assert buffInt.compareTo(this.coefs[i]) >= 0;
                buffInt = this.coefs[i];
            }

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
            int indPivot = width / 2 + from;
            BigInteger pivot = this.coefs[indPivot];
            int litPivot = this.lits[indPivot];
            BigInteger tmp;
            int i = from - 1;
            int j = to;
            int tmp2;

            for (;;) {
                do {
                    i++;
                } while (this.coefs[i].compareTo(pivot) > 0
                        || this.coefs[i].equals(pivot)
                        && this.lits[i] > litPivot);
                do {
                    j--;
                } while (pivot.compareTo(this.coefs[j]) > 0
                        || this.coefs[j].equals(pivot)
                        && this.lits[j] < litPivot);

                if (i >= j) {
                    break;
                }

                tmp = this.coefs[i];
                this.coefs[i] = this.coefs[j];
                this.coefs[j] = tmp;
                tmp2 = this.lits[i];
                this.lits[i] = this.lits[j];
                this.lits[j] = tmp2;
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
        BigInteger tmp = slackConstraint();
        for (int i = 0; i < this.lits.length; i++) {
            if (this.voc.isUnassigned(this.lits[i])
                    && tmp.compareTo(this.coefs[i]) < 0) {
                boolean ret = s.enqueue(this.lits[i], this);
                assert ret;
            }
        }
    }

    public void assertConstraintIfNeeded(UnitPropagationListener s) {
        assertConstraint(s);
    }

    /**
     * @return Returns the degree.
     */
    public BigInteger getDegree() {
        return this.degree;
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
     * to obtain the coefficients of the constraint.
     * 
     * @return a copy of the array of the coefficients
     */
    public BigInteger[] getCoefs() {
        BigInteger[] coefsBis = new BigInteger[this.coefs.length];
        System.arraycopy(this.coefs, 0, coefsBis, 0, this.coefs.length);
        return coefsBis;
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
        BigInteger cptCoefs = BigInteger.ZERO;
        int index = this.coefs.length;
        while (cptCoefs.compareTo(this.degree) > 0 && index > 0) {
            cptCoefs = cptCoefs.add(this.coefs[--index]);
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

            WatchPb wpb = (WatchPb) pb;
            if (!this.degree.equals(wpb.degree)
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
                        if (!wpb.coefs[ilit2].equals(this.coefs[ilit])) {
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

    public boolean canBePropagatedMultipleTimes() {
        return true;
    }

    public Constr toConstraint() {
        return this;
    }

    public void calcReasonOnTheFly(int p, IVecInt trail, IVecInt outReason) {
        BigInteger sumfalsified = BigInteger.ZERO;
        IVecInt vlits = new VecInt(this.lits);
        int index;
        for (IteratorInt it = trail.iterator(); it.hasNext();) {
            int q = it.next();
            if (vlits.contains(q ^ 1)) {
                assert voc.isFalsified(q ^ 1);
                outReason.push(q);
                index = vlits.indexOf(q ^ 1);
                sumfalsified = sumfalsified.add(this.coefs[index]);
                if (this.sumcoefs.subtract(sumfalsified).compareTo(this.degree) < 0) {
                    return;
                }
            }
        }
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
                stb.append("]");
                stb.append(" ");
            }
            stb.append(">= ");
            stb.append(this.degree);
        }
        return stb.toString();
    }

}
