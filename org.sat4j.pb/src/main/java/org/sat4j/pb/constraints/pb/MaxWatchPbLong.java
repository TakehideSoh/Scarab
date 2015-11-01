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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sat4j.minisat.core.ILits;
import org.sat4j.specs.Constr;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.MandatoryLiteralListener;
import org.sat4j.specs.UnitPropagationListener;
import org.sat4j.specs.VarMapper;

/**
 * Data structure for pseudo-boolean constraint with watched literals.
 * 
 * All literals are watched. The sum of the literals satisfied or unvalued is
 * always memorized, to detect conflict.
 * 
 * 
 */
public final class MaxWatchPbLong extends WatchPbLong {

    private static final long serialVersionUID = 1L;

    /**
     * sum of the coefficients of the literals satisfied or unvalued
     */
    private long watchCumul = 0;

    private final Map<Integer, Long> litToCoeffs;

    /**
     * Builds a PB constraint for a0.x0 + a1.x1 + ... + an.xn >= k
     * 
     * This constructor is called for learnt pseudo boolean constraints.
     * 
     * @param voc
     *            all the possible variables (vocabulary)
     * @param mpb
     *            data structure which contains literals of the constraint,
     *            coefficients (a0, a1, ... an), and the degree of the
     *            constraint (k). The constraint is a "more than" constraint.
     */
    private MaxWatchPbLong(ILits voc, IDataStructurePB mpb) {

        super(mpb);
        this.voc = voc;

        this.activity = 0;
        this.watchCumul = 0;
        if (this.coefs.length > MaxWatchPb.LIMIT_FOR_MAP) {
            this.litToCoeffs = new HashMap<Integer, Long>(this.coefs.length);
            for (int i = 0; i < this.coefs.length; i++) {
                this.litToCoeffs.put(this.lits[i], this.coefs[i]);
            }
        } else {
            this.litToCoeffs = null;
        }
    }

    /**
     * Builds a PB constraint for a0.x0 + a1.x1 + ... + an.xn >= k
     * 
     * @param voc
     *            all the possible variables (vocabulary)
     * @param lits
     *            literals of the constraint (x0,x1, ... xn)
     * @param coefs
     *            coefficients of the left side of the constraint (a0, a1, ...
     *            an)
     * @param degree
     *            degree of the constraint (k)
     */
    private MaxWatchPbLong(ILits voc, int[] lits, BigInteger[] coefs,
            BigInteger degree, BigInteger sumCoefs) {

        super(lits, coefs, degree, sumCoefs);
        this.voc = voc;

        this.activity = 0;
        this.watchCumul = 0;
        if (coefs.length > MaxWatchPb.LIMIT_FOR_MAP) {
            this.litToCoeffs = new HashMap<Integer, Long>(this.coefs.length);
            for (int i = 0; i < this.coefs.length; i++) {
                this.litToCoeffs.put(this.lits[i], this.coefs[i]);
            }
        } else {
            this.litToCoeffs = null;
        }
    }

    /**
     * All the literals are watched.
     * 
     * @see org.sat4j.pb.constraints.pb.WatchPbLong#computeWatches()
     */
    @Override
    protected void computeWatches() throws ContradictionException {
        assert this.watchCumul == 0;
        for (int i = 0; i < this.lits.length; i++) {
            if (this.voc.isFalsified(this.lits[i])) {
                if (this.learnt) {
                    this.voc.undos(this.lits[i] ^ 1).push(this);
                    this.voc.watch(this.lits[i] ^ 1, this);
                }
            } else {
                // updating of the initial value for the counter
                this.voc.watch(this.lits[i] ^ 1, this);
                this.watchCumul = this.watchCumul + this.coefs[i];
            }
        }

        assert this.watchCumul >= computeLeftSide();
        if (!this.learnt && this.watchCumul < this.degree) {
            throw new ContradictionException("non satisfiable constraint");
        }
    }

    /*
     * This method propagates any possible value.
     * 
     * This method is only called in the factory methods.
     * 
     * @see org.sat4j.minisat.constraints.WatchPb#computePropagation()
     */
    @Override
    protected void computePropagation(UnitPropagationListener s)
            throws ContradictionException {
        // propagate any possible value
        int ind = 0;
        while (ind < this.coefs.length
                && this.watchCumul - this.coefs[ind] < this.degree) {
            if (this.voc.isUnassigned(this.lits[ind])
                    && !s.enqueue(this.lits[ind], this)) {
                // because this happens during the building of a constraint.
                throw new ContradictionException("non satisfiable constraint");
            }
            ind++;
        }
        assert this.watchCumul >= computeLeftSide();
    }

    /**
     * Propagation of a falsified literal
     * 
     * @param s
     *            the solver
     * @param p
     *            the propagated literal (it must be falsified)
     * @return false iff there is a conflict
     */
    @Override
    public boolean propagate(UnitPropagationListener s, int p) {
        this.voc.watch(p, this);

        assert this.watchCumul >= computeLeftSide() : "" + this.watchCumul
                + "/" + computeLeftSide() + ":" + this.learnt;

        // compute the new value for watchCumul
        long coefP;
        if (this.litToCoeffs == null) {
            // finding the index for p in the array of literals
            int indiceP = 0;
            while ((this.lits[indiceP] ^ 1) != p) {
                indiceP++;
            }

            // compute the new value for watchCumul
            coefP = this.coefs[indiceP];
        } else {
            coefP = this.litToCoeffs.get(p ^ 1);
        }
        long newcumul = this.watchCumul - coefP;

        if (newcumul < this.degree) {
            // there is a conflict
            assert !isSatisfiable();
            return false;
        }

        // if no conflict, not(p) can be propagated
        // allow a later un-assignation
        this.voc.undos(p).push(this);
        // really update watchCumul
        this.watchCumul = newcumul;

        // propagation
        int ind = 0;
        // limit is the margin between the sum of the coefficients of the
        // satisfied+unassigned literals
        // and the degree of the constraint
        long limit = this.watchCumul - this.degree;
        // for each coefficient greater than limit
        while (ind < this.coefs.length && limit < this.coefs[ind]) {
            // its corresponding literal is implied
            if (this.voc.isUnassigned(this.lits[ind])
                    && !s.enqueue(this.lits[ind], this)) {
                // if it is not possible then there is a conflict
                assert !isSatisfiable();
                return false;
            }
            ind++;
        }

        assert this.learnt || this.watchCumul >= computeLeftSide();
        assert this.watchCumul >= computeLeftSide();
        return true;
    }

    /**
     * Remove a constraint from the solver
     */
    @Override
    public void remove(UnitPropagationListener upl) {
        for (int i = 0; i < this.lits.length; i++) {
            if (!this.voc.isFalsified(this.lits[i])) {
                this.voc.watches(this.lits[i] ^ 1).remove(this);
            }
        }
        // Unset root propagated literals, see SAT-110
        int ind = 0;
        while (ind < this.coefs.length
                && this.watchCumul - this.coefs[ind] < this.degree) {
            if (!this.voc.isUnassigned(this.lits[ind])) {
                upl.unset(this.lits[ind]);
            }
            ind++;
        }
    }

    /**
     * this method is called during backtrack
     * 
     * @param p
     *            an unassigned literal
     */
    @Override
    public void undo(int p) {
        long coefP;
        if (this.litToCoeffs == null) {
            // finding the index for p in the array of literals
            int indiceP = 0;
            while (indiceP < this.lits.length && (this.lits[indiceP] ^ 1) != p) {
                indiceP++;
            }
            // compute the new value for watchCumul
            coefP = indiceP == this.lits.length ? 0L : this.coefs[indiceP];
        } else {
            Long coefL = this.litToCoeffs.get(p ^ 1);
            if (coefL != null) {
                coefP = coefL.longValue();
            } else {
                coefP = 0L;
            }
        }
        this.watchCumul = this.watchCumul + coefP;
    }

    /**
     * build a pseudo boolean constraint. Coefficients are positive integers
     * less than or equal to the degree (this is called a normalized
     * constraint).
     * 
     * @param s
     *            a unit propagation listener (usually the solver)
     * @param voc
     *            the vocabulary
     * @param lits
     *            the literals of the constraint
     * @param coefs
     *            the coefficients of the constraint
     * @param degree
     *            the degree of the constraint
     * @return a new PB constraint or null if a trivial inconsistency is
     *         detected.
     */
    public static MaxWatchPbLong normalizedMaxWatchPbNew(
            UnitPropagationListener s, ILits voc, int[] lits,
            BigInteger[] coefs, BigInteger degree, BigInteger sumCoefs)
            throws ContradictionException {
        // Parameters must not be modified
        MaxWatchPbLong outclause = new MaxWatchPbLong(voc, lits, coefs, degree,
                sumCoefs);

        if (outclause.degree <= 0) {
            return null;
        }

        outclause.computeWatches();

        outclause.computePropagation(s);

        return outclause;

    }

    /**
     * build a pseudo boolean constraint from a specific data structure. For
     * learnt constraints.
     * 
     * @param s
     *            a unit propagation listener (usually the solver)
     * @param mpb
     *            data structure which contains literals of the constraint,
     *            coefficients (a0, a1, ... an), and the degree of the
     *            constraint (k). The constraint is a "more than" constraint.
     * @return a new PB constraint or null if a trivial inconsistency is
     *         detected.
     */
    public static WatchPbLong normalizedWatchPbNew(ILits voc,
            IDataStructurePB mpb) {
        return new MaxWatchPbLong(voc, mpb);
    }

    public boolean propagatePI(MandatoryLiteralListener l, int p) {
        this.voc.watch(p, this);

        // compute the new value for watchCumul
        long coefP;
        if (this.litToCoeffs == null) {
            // finding the index for p in the array of literals
            int indiceP = 0;
            while ((this.lits[indiceP] ^ 1) != p) {
                indiceP++;
            }

            // compute the new value for watchCumul
            coefP = this.coefs[indiceP];
        } else {
            coefP = this.litToCoeffs.get(p ^ 1);
        }

        long newcumul = this.watchCumul - coefP;

        // if no conflict, not(p) can be propagated
        // allow a later un-assignation
        this.voc.undos(p).push(this);
        // really update watchCumul
        this.watchCumul = newcumul;

        // propagation
        int ind = 0;
        // limit is the margin between the sum of the coefficients of the
        // satisfied+unassigned literals
        // and the degree of the constraint
        long limit = this.watchCumul - this.degree;
        // for each coefficient greater than limit
        while (ind < this.coefs.length && limit < this.coefs[ind]) {
            // its corresponding literal is implied
            if (!this.voc.isFalsified(this.lits[ind])) {
                l.isMandatory(this.lits[ind]);
            }
            ind++;
        }
        return true;

    }

    public int getAssertionLevel(IVecInt trail, int decisionLevel) {
        WatchPbLongPbConstrDecorator decorator = new WatchPbLongPbConstrDecorator(
                this);
        IConflict cm = ConflictMap.createConflict(decorator, decisionLevel,
                false);
        int backtrackDL = cm.getBacktrackLevel(decisionLevel);
        System.out.println("btdl=" + backtrackDL);
        Set<Integer> litsSet = new HashSet<Integer>();
        for (Integer i : this.lits)
            litsSet.add(i);
        for (int i = 0; i < trail.size(); ++i) {
            if (litsSet.contains(trail.get(i) ^ 1)) {
                return i;
            }
        }
        return -1;
    }

    private static class WatchPbLongPbConstrDecorator implements PBConstr {

        private final WatchPbLong cstr;

        public WatchPbLongPbConstrDecorator(WatchPbLong cstr) {
            this.cstr = cstr;
        }

        public boolean propagatePI(MandatoryLiteralListener l, int p) {
            return cstr.propagatePI(l, p);
        }

        public boolean isAssertive(int dl) {
            return cstr.isAssertive(dl);
        }

        public void calcReason(int p, IVecInt outReason) {
            cstr.calcReason(p, outReason);
        }

        public int get(int i) {
            return cstr.get(i);
        }

        public double getActivity() {
            return cstr.getActivity();
        }

        public int getAssertionLevel(IVecInt trail, int decisionLevel) {
            return cstr.getAssertionLevel(trail, decisionLevel);
        }

        public void incActivity(double claInc) {
            cstr.incActivity(claInc);
        }

        public void setActivity(double d) {
            cstr.setActivity(d);
        }

        public long slackConstraint() {
            return cstr.slackConstraint();
        }

        public long slackConstraint(long[] theCoefs, long theDegree) {
            return cstr.slackConstraint(theCoefs, theDegree);
        }

        public long computeLeftSide(long[] theCoefs) {
            return cstr.computeLeftSide(theCoefs);
        }

        public long computeLeftSide() {
            return cstr.computeLeftSide();
        }

        public boolean learnt() {
            return cstr.learnt();
        }

        public boolean locked() {
            return cstr.locked();
        }

        public void rescaleBy(double d) {
            cstr.rescaleBy(d);
        }

        public void setLearnt() {
            cstr.setLearnt();
        }

        public boolean simplify() {
            return cstr.simplify();
        }

        public final int size() {
            return cstr.size();
        }

        @Override
        public String toString() {
            return cstr.toString();
        }

        public void assertConstraint(UnitPropagationListener s) {
            cstr.assertConstraint(s);
        }

        public void assertConstraintIfNeeded(UnitPropagationListener s) {
            cstr.assertConstraintIfNeeded(s);
        }

        public void register() {
            cstr.register();
        }

        public int[] getLits() {
            return cstr.getLits();
        }

        public ILits getVocabulary() {
            return cstr.getVocabulary();
        }

        public IVecInt computeAnImpliedClause() {
            return cstr.computeAnImpliedClause();
        }

        public boolean coefficientsEqualToOne() {
            return cstr.coefficientsEqualToOne();
        }

        @Override
        public boolean equals(Object pb) {
            return cstr.equals(pb);
        }

        @Override
        public int hashCode() {
            return cstr.hashCode();
        }

        public void forwardActivity(double claInc) {
            cstr.forwardActivity(claInc);
        }

        public void remove(UnitPropagationListener upl) {
            cstr.remove(upl);
        }

        public boolean propagate(UnitPropagationListener s, int p) {
            return cstr.propagate(s, p);
        }

        public void undo(int p) {
            cstr.undo(p);
        }

        public boolean canBePropagatedMultipleTimes() {
            return cstr.canBePropagatedMultipleTimes();
        }

        public Constr toConstraint() {
            return cstr.toConstraint();
        }

        public void calcReasonOnTheFly(int p, IVecInt trail, IVecInt outReason) {
            cstr.calcReasonOnTheFly(p, trail, outReason);
        }

        public boolean canBeSatisfiedByCountingLiterals() {
            return cstr.canBeSatisfiedByCountingLiterals();
        }

        public int requiredNumberOfSatisfiedLiterals() {
            return cstr.requiredNumberOfSatisfiedLiterals();
        }

        public boolean isSatisfied() {
            return cstr.isSatisfied();
        }

        public String toString(VarMapper mapper) {
            return cstr.toString(mapper);
        }

        public BigInteger getCoef(int literal) {
            return cstr.getCoef(literal);
        }

        public BigInteger getDegree() {
            return cstr.getDegree();
        }

        public BigInteger[] getCoefs() {
            return cstr.getCoefs();
        }

    }
}
