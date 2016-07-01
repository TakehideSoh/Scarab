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

import org.sat4j.minisat.core.ILits;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.UnitPropagationListener;

/**
 * Data structure for pseudo-boolean constraint with watched literals.
 * 
 * All literals are watched. The sum of the literals satisfied or unvalued is
 * always memorized, to detect conflict.
 * 
 * 
 */
public class MinWatchPbLongCP extends WatchPbLongCP {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
     * sum of the coefficients of the literals satisfied or unvalued
     */
    protected long watchCumul = 0;

    /**
     * is the literal of index i watching the constraint ?
     */
    protected boolean[] watched;

    /**
     * indexes of literals watching the constraint
     */
    protected int[] watching;

    /**
     * number of literals watching the constraint.
     * 
     * This is the real size of the array watching
     */
    protected int watchingCount = 0;

    /**
     * Basic constructor for pb constraint a0.x0 + a1.x1 + ... + an.xn >= k
     * 
     * This constructor is called for learnt pseudo boolean constraints.
     * 
     * @param voc
     *            all the possible variables (vocabulary)
     * @param mpb
     *            a mutable PB constraint
     */
    protected MinWatchPbLongCP(ILits voc, IDataStructurePB mpb) {

        super(mpb);
        this.voc = voc;

        this.watching = new int[this.coefs.length];
        this.watched = new boolean[this.coefs.length];
        this.activity = 0;
        this.watchCumul = 0;
        this.watchingCount = 0;

    }

    /**
     * Basic constructor for PB constraint a0.x0 + a1.x1 + ... + an.xn >= k
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
    protected MinWatchPbLongCP(ILits voc, int[] lits, BigInteger[] coefs, // NOPMD
            BigInteger degree, BigInteger sumCoefs) {

        super(lits, coefs, degree, sumCoefs);
        this.voc = voc;

        this.watching = new int[this.coefs.length];
        this.watched = new boolean[this.coefs.length];
        this.activity = 0;
        this.watchCumul = 0;
        this.watchingCount = 0;

    }

    /*
     * This method initialize the watched literals.
     * 
     * This method is only called in the factory methods.
     * 
     * @see org.sat4j.minisat.constraints.WatchPb#computeWatches()
     */
    @Override
    protected void computeWatches() throws ContradictionException {
        assert this.watchCumul == 0;
        assert this.watchingCount == 0;
        for (int i = 0; i < this.lits.length
                && this.watchCumul - this.coefs[0] < this.degree; i++) {
            if (!this.voc.isFalsified(this.lits[i])) {
                this.voc.watch(this.lits[i] ^ 1, this);
                this.watching[this.watchingCount++] = i;
                this.watched[i] = true;
                // update the initial value for watchCumul (poss)
                this.watchCumul = this.watchCumul + this.coefs[i];
            }
        }

        if (this.learnt) {
            watchMoreForLearntConstraint();
        }

        if (this.watchCumul < this.degree) {
            throw new ContradictionException("non satisfiable constraint");
        }
        assert nbOfWatched() == this.watchingCount;
    }

    private void watchMoreForLearntConstraint() {
        // looking for literals to be watched,
        // ordered by decreasing level
        int free = 1;
        int maxlevel, maxi, level;

        while (this.watchCumul - this.coefs[0] < this.degree && free > 0) {
            free = 0;
            // looking for the literal falsified
            // at the least (lowest ?) level
            maxlevel = -1;
            maxi = -1;
            for (int i = 0; i < this.lits.length; i++) {
                if (this.voc.isFalsified(this.lits[i]) && !this.watched[i]) {
                    free++;
                    level = this.voc.getLevel(this.lits[i]);
                    if (level > maxlevel) {
                        maxi = i;
                        maxlevel = level;
                    }
                }
            }

            if (free > 0) {
                assert maxi >= 0;
                this.voc.watch(this.lits[maxi] ^ 1, this);
                this.watching[this.watchingCount++] = maxi;
                this.watched[maxi] = true;
                // update of the watchCumul value
                this.watchCumul = this.watchCumul + this.coefs[maxi];
                free--;
                assert free >= 0;
            }
        }
        assert this.lits.length == 1 || this.watchingCount > 1;
    }

    /*
     * This method propagates any possible value.
     * 
     * This method is only called in the factory methods.
     * 
     * @see
     * org.sat4j.minisat.constraints.WatchPb#computePropagation(org.sat4j.minisat
     * .UnitPropagationListener)
     */
    @Override
    protected void computePropagation(UnitPropagationListener s)
            throws ContradictionException {
        // propagate any possible value
        int ind = 0;
        while (ind < this.lits.length
                && this.watchCumul - this.coefs[this.watching[ind]] < this.degree) {
            if (this.voc.isUnassigned(this.lits[ind])
                    && !s.enqueue(this.lits[ind], this)) {
                throw new ContradictionException("non satisfiable constraint");
            }
            ind++;
        }
    }

    /**
     * build a pseudo boolean constraint. Coefficients are positive integers
     * less than or equal to the degree (this is called a normalized
     * constraint).
     * 
     * @param s
     *            a unit propagation listener
     * @param voc
     *            the vocabulary
     * @param lits
     *            the literals
     * @param coefs
     *            the coefficients
     * @param degree
     *            the degree of the constraint to normalize.
     * @return a new PB constraint or null if a trivial inconsistency is
     *         detected.
     */
    public static MinWatchPbLongCP normalizedMinWatchPbNew(
            UnitPropagationListener s, ILits voc, int[] lits,
            BigInteger[] coefs, BigInteger degree, BigInteger sumCoefs)
            throws ContradictionException {
        // Parameters must not be modified
        MinWatchPbLongCP outclause = new MinWatchPbLongCP(voc, lits, coefs,
                degree, sumCoefs);

        if (outclause.degree <= 0) {
            return null;
        }

        outclause.computeWatches();

        outclause.computePropagation(s);

        return outclause;

    }

    /**
     * Number of really watched literals. It should return the same value as
     * watchingCount.
     * 
     * This method must only be called for assertions.
     * 
     * @return number of watched literals.
     */
    protected int nbOfWatched() {
        int retour = 0;
        for (int ind = 0; ind < this.watched.length; ind++) {
            for (int i = 0; i < this.watchingCount; i++) {
                if (this.watching[i] == ind) {
                    assert this.watched[ind];
                }
            }
            retour += this.watched[ind] ? 1 : 0;
        }
        return retour;
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
        assert nbOfWatched() == this.watchingCount;
        assert this.watchingCount > 1;

        // finding the index for p in the array of literals (pIndice)
        // and in the array of watching (pIndiceWatching)
        int pIndiceWatching = 0;
        while (pIndiceWatching < this.watchingCount
                && (this.lits[this.watching[pIndiceWatching]] ^ 1) != p) {
            pIndiceWatching++;
        }
        int pIndice = this.watching[pIndiceWatching];

        assert p == (this.lits[pIndice] ^ 1);
        assert this.watched[pIndice];

        // the greatest coefficient of the watched literals is necessary
        // (pIndice excluded)
        long maxCoef = maximalCoefficient(pIndice);

        // update watching and watched w.r.t. to the propagation of p
        // new literals will be watched, maxCoef could be changed
        maxCoef = updateWatched(maxCoef, pIndice);

        long upWatchCumul = this.watchCumul - this.coefs[pIndice];
        assert nbOfWatched() == this.watchingCount;

        // if a conflict has been detected, return false
        if (upWatchCumul < this.degree) {
            // conflit
            this.voc.watch(p, this);
            assert this.watched[pIndice];
            assert !isSatisfiable();
            return false;
        } else if (upWatchCumul < this.degree + maxCoef) {
            // some literals must be assigned to true and then propagated
            assert this.watchingCount != 0;
            long limit = upWatchCumul - this.degree;
            for (int i = 0; i < this.watchingCount; i++) {
                if (limit < this.coefs[this.watching[i]]
                        && i != pIndiceWatching
                        && !this.voc.isSatisfied(this.lits[this.watching[i]])
                        && !s.enqueue(this.lits[this.watching[i]], this)) {
                    this.voc.watch(p, this);
                    assert !isSatisfiable();
                    return false;
                }
            }
            // if the constraint is added to the undos of p (by propagation),
            // then p should be preserved.
            this.voc.undos(p).push(this);
        }

        // else p is no more watched
        this.watched[pIndice] = false;
        this.watchCumul = upWatchCumul;
        this.watching[pIndiceWatching] = this.watching[--this.watchingCount];

        assert this.watchingCount != 0;
        assert nbOfWatched() == this.watchingCount;

        return true;
    }

    /**
     * Remove the constraint from the solver
     */
    public void remove(UnitPropagationListener upl) {
        for (int i = 0; i < this.watchingCount; i++) {
            this.voc.watches(this.lits[this.watching[i]] ^ 1).remove(this);
            this.watched[this.watching[i]] = false;
        }
        this.watchingCount = 0;
        assert nbOfWatched() == this.watchingCount;
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
     *            un unassigned literal
     */
    public void undo(int p) {
        this.voc.watch(p, this);
        int pIndice = 0;
        while ((this.lits[pIndice] ^ 1) != p) {
            pIndice++;
        }

        assert pIndice < this.lits.length;

        this.watchCumul = this.watchCumul + this.coefs[pIndice];

        assert this.watchingCount == nbOfWatched();

        this.watched[pIndice] = true;
        this.watching[this.watchingCount++] = pIndice;

        assert this.watchingCount == nbOfWatched();
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
    public static WatchPbLongCP normalizedWatchPbNew(ILits voc,
            IDataStructurePB mpb) {
        return new MinWatchPbLongCP(voc, mpb);
    }

    /**
     * the maximal coefficient for the watched literals
     * 
     * @param pIndice
     *            propagated literal : its coefficient is excluded from the
     *            search of the maximal coefficient
     * @return the maximal coefficient for the watched literals
     */
    protected long maximalCoefficient(int pIndice) {
        long maxCoef = 0;
        for (int i = 0; i < this.watchingCount; i++) {
            if (this.coefs[this.watching[i]] > maxCoef
                    && this.watching[i] != pIndice) {
                maxCoef = this.coefs[this.watching[i]];
            }
        }

        assert this.learnt || maxCoef != 0;
        return maxCoef;
    }

    /**
     * update arrays watched and watching w.r.t. the propagation of a literal.
     * 
     * return the maximal coefficient of the watched literals (could have been
     * changed).
     * 
     * @param mc
     *            the current maximal coefficient of the watched literals
     * @param pIndice
     *            the literal propagated (falsified)
     * @return the new maximal coefficient of the watched literals
     */
    protected long updateWatched(long mc, int pIndice) {
        long maxCoef = mc;
        // if not all the literals are watched
        if (this.watchingCount < size()) {
            // the watchCumul sum will have to be updated
            long upWatchCumul = this.watchCumul - this.coefs[pIndice];

            // we must obtain upWatchCumul such that
            // upWatchCumul = degree + maxCoef
            long degreePlusMaxCoef = this.degree + maxCoef;
            for (int ind = 0; ind < this.lits.length; ind++) {
                if (upWatchCumul >= degreePlusMaxCoef) {
                    // nothing more to watch
                    // note: logic negated to old version // dvh
                    break;
                }
                // while upWatchCumul does not contain enough
                if (!this.voc.isFalsified(this.lits[ind]) && !this.watched[ind]) {
                    // watch one more
                    upWatchCumul = upWatchCumul + this.coefs[ind];
                    // update arrays watched and watching
                    this.watched[ind] = true;
                    assert this.watchingCount < size();
                    this.watching[this.watchingCount++] = ind;
                    this.voc.watch(this.lits[ind] ^ 1, this);
                    // this new watched literal could change the maximal
                    // coefficient
                    if (this.coefs[ind] > maxCoef) {
                        maxCoef = this.coefs[ind];
                        degreePlusMaxCoef = this.degree + maxCoef;
                    }
                }
            }
            // update watchCumul
            this.watchCumul = upWatchCumul + this.coefs[pIndice];
        }
        return maxCoef;
    }

    public int getAssertionLevel(IVecInt trail, int decisionLevel) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("To be done");
    }
}
