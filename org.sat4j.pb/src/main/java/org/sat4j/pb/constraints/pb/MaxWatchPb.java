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
import java.util.Map;

import org.sat4j.minisat.core.ILits;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.MandatoryLiteralListener;
import org.sat4j.specs.UnitPropagationListener;

/**
 * Data structure for pseudo-boolean constraint with watched literals.
 * 
 * All literals are watched. The sum of the literals satisfied or unvalued is
 * always memorized, to detect conflict.
 * 
 * @author anne
 * 
 */
public final class MaxWatchPb extends WatchPb {

    private static final long serialVersionUID = 1L;

    public static final int LIMIT_FOR_MAP = 100;

    /**
     * sum of the coefficients of the literals satisfied or unvalued
     */
    private BigInteger watchCumul = BigInteger.ZERO;

    private final Map<Integer, BigInteger> litToCoeffs;

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
    private MaxWatchPb(ILits voc, IDataStructurePB mpb) {

        super(mpb);
        this.voc = voc;

        this.activity = 0;
        this.watchCumul = BigInteger.ZERO;
        if (this.coefs.length > LIMIT_FOR_MAP) {
            this.litToCoeffs = new HashMap<Integer, BigInteger>(
                    this.coefs.length);
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
    private MaxWatchPb(ILits voc, int[] lits, BigInteger[] coefs,
            BigInteger degree, BigInteger sumCoefs) {

        super(lits, coefs, degree, sumCoefs);
        this.voc = voc;

        this.activity = 0;
        this.watchCumul = BigInteger.ZERO;
        if (coefs.length > LIMIT_FOR_MAP) {
            this.litToCoeffs = new HashMap<Integer, BigInteger>(
                    this.coefs.length);
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
     * @see org.sat4j.pb.constraints.pb.WatchPb#computeWatches()
     */
    @Override
    protected void computeWatches() throws ContradictionException {
        assert this.watchCumul.equals(BigInteger.ZERO);
        for (int i = 0; i < this.lits.length; i++) {
            if (this.voc.isFalsified(this.lits[i])) {
                if (this.learnt) {
                    this.voc.undos(this.lits[i] ^ 1).push(this);
                    this.voc.watch(this.lits[i] ^ 1, this);
                }
            } else {
                // updating of the initial value for the counter
                this.voc.watch(this.lits[i] ^ 1, this);
                this.watchCumul = this.watchCumul.add(this.coefs[i]);
            }
        }

        assert this.watchCumul.compareTo(computeLeftSide()) >= 0;
        if (!this.learnt && this.watchCumul.compareTo(this.degree) < 0) {
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
                && this.watchCumul.subtract(this.coefs[ind]).compareTo(
                        this.degree) < 0) {
            if (this.voc.isUnassigned(this.lits[ind])
                    && !s.enqueue(this.lits[ind], this)) {
                // because this happens during the building of a constraint.
                throw new ContradictionException("non satisfiable constraint");
            }
            ind++;
        }
        assert this.watchCumul.compareTo(computeLeftSide()) >= 0;
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
    public boolean propagate(UnitPropagationListener s, int p) {
        this.voc.watch(p, this);

        assert this.watchCumul.compareTo(computeLeftSide()) >= 0 : ""
                + this.watchCumul + "/" + computeLeftSide() + ":" + this.learnt;

        // compute the new value for watchCumul
        BigInteger coefP;
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

        BigInteger newcumul = this.watchCumul.subtract(coefP);

        if (newcumul.compareTo(this.degree) < 0) {
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
        BigInteger limit = this.watchCumul.subtract(this.degree);
        // for each coefficient greater than limit
        while (ind < this.coefs.length && limit.compareTo(this.coefs[ind]) < 0) {
            // its corresponding literal is implied
            if (this.voc.isUnassigned(this.lits[ind])
                    && !s.enqueue(this.lits[ind], this)) {
                // if it is not possible then there is a conflict
                assert !isSatisfiable();
                return false;
            }
            ind++;
        }

        assert this.learnt || this.watchCumul.compareTo(computeLeftSide()) >= 0;
        assert this.watchCumul.compareTo(computeLeftSide()) >= 0;
        return true;
    }

    /**
     * Remove a constraint from the solver
     */
    public void remove(UnitPropagationListener upl) {
        for (int i = 0; i < this.lits.length; i++) {
            if (!this.voc.isFalsified(this.lits[i])) {
                this.voc.watches(this.lits[i] ^ 1).remove(this);
            }
        }
        // Unset root propagated literals, see SAT-110
        int ind = 0;
        while (ind < this.coefs.length
                && this.watchCumul.subtract(this.coefs[ind]).compareTo(
                        this.degree) < 0) {
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
    public void undo(int p) {
        BigInteger coefP;
        if (this.litToCoeffs == null) {
            // finding the index for p in the array of literals
            int indiceP = 0;
            while (indiceP < this.lits.length && (this.lits[indiceP] ^ 1) != p) {
                indiceP++;
            }
            // compute the new value for watchCumul
            coefP = indiceP == this.lits.length ? BigInteger.ZERO
                    : this.coefs[indiceP];

        } else {
            coefP = this.litToCoeffs.get(p ^ 1);
        }

        this.watchCumul = this.watchCumul.add(coefP);
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
    public static MaxWatchPb normalizedMaxWatchPbNew(UnitPropagationListener s,
            ILits voc, int[] lits, BigInteger[] coefs, BigInteger degree,
            BigInteger sumCoefs) throws ContradictionException {
        // Parameters must not be modified
        MaxWatchPb outclause = new MaxWatchPb(voc, lits, coefs, degree,
                sumCoefs);

        if (outclause.degree.signum() <= 0) {
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
    public static WatchPb normalizedWatchPbNew(ILits voc, IDataStructurePB mpb) {
        return new MaxWatchPb(voc, mpb);
    }

    public boolean propagatePI(MandatoryLiteralListener l, int p) {
        this.voc.watch(p, this);

        // compute the new value for watchCumul
        BigInteger coefP;
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

        BigInteger newcumul = this.watchCumul.subtract(coefP);

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
        BigInteger limit = this.watchCumul.subtract(this.degree);
        // for each coefficient greater than limit
        while (ind < this.coefs.length && limit.compareTo(this.coefs[ind]) < 0) {
            // its corresponding literal is implied
            if (this.voc.isSatisfied(this.lits[ind])) {
                l.isMandatory(this.lits[ind]);
            }
            ind++;
        }
        return true;
    }

    public int getAssertionLevel(IVecInt trail, int decisionLevel) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("To be done");
    }
}
