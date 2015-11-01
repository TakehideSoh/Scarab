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

import org.sat4j.core.VecInt;
import org.sat4j.minisat.constraints.cnf.Lits;
import org.sat4j.minisat.core.ILits;
import org.sat4j.minisat.core.VarActivityListener;
import org.sat4j.specs.IteratorInt;

/**
 * @author parrain TODO To change the template for this generated type comment
 *         go to Window - Preferences - Java - Code Style - Code Templates
 */
public class ConflictMap extends MapPb implements IConflict {

    private final ILits voc;

    protected boolean hasBeenReduced = false;
    protected long numberOfReductions = 0;

    /**
     * to store the slack of the current resolvant
     */
    protected BigInteger currentSlack;

    protected int currentLevel;

    /**
     * allows to access directly to all variables belonging to a particular
     * level At index 0, unassigned literals are stored (usually level -1); so
     * there is always a step between index and levels.
     */
    protected VecInt[] byLevel;

    /**
     * constructs the data structure needed to perform cutting planes
     * 
     * @param cpb
     *            pseudo-boolean constraint which raised the conflict
     * @param level
     *            current decision level
     * @return a conflict on which cutting plane can be performed.
     */
    public static IConflict createConflict(PBConstr cpb, int level) {
        return new ConflictMap(cpb, level);
    }

    public static IConflict createConflict(PBConstr cpb, int level,
            boolean noRemove) {
        return new ConflictMap(cpb, level, noRemove);
    }

    ConflictMap(PBConstr cpb, int level) {
        this(cpb, level, false);
    }

    ConflictMap(PBConstr cpb, int level, boolean noRemove) {
        super(cpb, level, noRemove);
        this.voc = cpb.getVocabulary();
        this.currentLevel = level;
        initStructures();
        if (noRemove)
            this.rmSatLit = new NoRemoveSatisfied();
        else
            this.rmSatLit = new RemoveSatisfied();
    }

    private void initStructures() {
        this.currentSlack = BigInteger.ZERO;
        this.byLevel = new VecInt[levelToIndex(this.currentLevel) + 1];
        int ilit, litLevel, index;
        BigInteger tmp;
        for (int i = 0; i < size(); i++) {
            ilit = this.weightedLits.getLit(i);
            litLevel = this.voc.getLevel(ilit);
            // eventually add to slack
            tmp = this.weightedLits.getCoef(i);
            if (tmp.signum() > 0
                    && (!this.voc.isFalsified(ilit) || litLevel == this.currentLevel)) {
                this.currentSlack = this.currentSlack.add(tmp);
            }
            // add to byLevel structure
            index = levelToIndex(litLevel);
            if (this.byLevel[index] == null) {
                this.byLevel[index] = new VecInt();
            }
            this.byLevel[index].push(ilit);
        }
    }

    /**
     * convert level into an index in the byLevel structure
     * 
     * @param level
     * @return
     */
    private static int levelToIndex(int level) {
        return level + 1;
    }

    /**
     * convert index in the byLevel structure into a level
     * 
     * @param indLevel
     * @return
     */
    private static int indexToLevel(int indLevel) {
        return indLevel - 1;
    }

    /**
     * private interface to simplify or not conflicts by removing satisfied
     * literals at a higher level.
     */
    private final IRemoveSatisfiedLiterals rmSatLit;

    private static class NoRemoveSatisfied implements IRemoveSatisfiedLiterals {

        public BigInteger removeSatisfiedLiteralsFromHigherDecisionLevels(
                IWatchPb wpb, final BigInteger[] coefsBis,
                final int currentLevel, final BigInteger degreeBis) {
            return degreeBis;
        }
    }

    private class RemoveSatisfied implements IRemoveSatisfiedLiterals {

        public BigInteger removeSatisfiedLiteralsFromHigherDecisionLevels(
                IWatchPb wpb, final BigInteger[] coefsBis,
                final int currentLevel, final BigInteger degreeBis) {
            assert degreeBis.compareTo(BigInteger.ONE) > 0;
            // search for all satisfied literals above the current decision
            // level
            int size = wpb.size();
            BigInteger degUpdate = degreeBis;
            int p;
            ConflictMap.this.possReducedCoefs = possConstraint(wpb, coefsBis);
            for (int ind = 0; ind < size; ind++) {
                p = wpb.get(ind);
                if (coefsBis[ind].signum() != 0
                        && ConflictMap.this.voc.isSatisfied(p)
                        && ConflictMap.this.voc.getLevel(p) < currentLevel) {

                    // reduction can be done
                    degUpdate = degUpdate.subtract(coefsBis[ind]);
                    ConflictMap.this.possReducedCoefs = ConflictMap.this.possReducedCoefs
                            .subtract(coefsBis[ind]);
                    coefsBis[ind] = BigInteger.ZERO;
                    assert ConflictMap.this.possReducedCoefs
                            .equals(possConstraint(wpb, coefsBis));
                }
            }

            // saturation of the constraint
            degUpdate = saturation(coefsBis, degUpdate, wpb);

            assert degreeBis.compareTo(degUpdate) >= 0;
            assert ConflictMap.this.possReducedCoefs.equals(possConstraint(wpb,
                    coefsBis));
            return degUpdate;
        }

    };

    /*
     * coefficients to be computed.
     */
    protected BigInteger coefMult = BigInteger.ZERO;

    protected BigInteger coefMultCons = BigInteger.ZERO;

    /**
     * computes a cutting plane with a pseudo-boolean constraint. this method
     * updates the current instance (of ConflictMap).
     * 
     * @param cpb
     *            constraint to compute with the cutting plane
     * @param litImplied
     *            literal that must be resolved by the cutting plane
     * @return an update of the degree of the current instance
     */
    public BigInteger resolve(PBConstr cpb, int litImplied,
            VarActivityListener val) {
        assert litImplied > 1;
        int nLitImplied = litImplied ^ 1;
        if (cpb == null || !this.weightedLits.containsKey(nLitImplied)) {
            // no resolution
            // undo operation should be anticipated
            int litLevel = levelToIndex(this.voc.getLevel(litImplied));
            int lit = 0;
            if (this.byLevel[litLevel] != null) {
                if (this.byLevel[litLevel].contains(litImplied)) {
                    lit = litImplied;
                    assert this.weightedLits.containsKey(litImplied);
                } else if (this.byLevel[litLevel].contains(nLitImplied)) {
                    lit = nLitImplied;
                    assert this.weightedLits.containsKey(nLitImplied);
                }
            }

            if (lit > 0) {
                this.byLevel[litLevel].remove(lit);
                if (this.byLevel[0] == null) {
                    this.byLevel[0] = new VecInt();
                }
                this.byLevel[0].push(lit);
            }
            return this.degree;
        }

        assert slackConflict().signum() <= 0;
        assert this.degree.signum() >= 0;

        // coefficients of the constraint must be copied in an other structure
        // in order to make reduction operations.
        BigInteger[] coefsCons = null;
        BigInteger degreeCons = cpb.getDegree();

        // search of the index of the implied literal
        int ind = 0;
        while (cpb.get(ind) != litImplied) {
            ind++;
        }

        assert cpb.get(ind) == litImplied;
        assert cpb.getCoef(ind) != BigInteger.ZERO;

        if (cpb.getCoef(ind).equals(BigInteger.ONE)) {
            // then we know that the resolvant will still be a conflict (cf.
            // Dixon's property)
            this.coefMultCons = this.weightedLits.get(nLitImplied);
            this.coefMult = BigInteger.ONE;
            // updating of the degree of the conflict
            degreeCons = degreeCons.multiply(this.coefMultCons);
        } else {
            IWatchPb wpb = (IWatchPb) cpb;
            coefsCons = wpb.getCoefs();
            degreeCons = rmSatLit
                    .removeSatisfiedLiteralsFromHigherDecisionLevels(wpb,
                            coefsCons, currentLevel, degreeCons);
            if (this.weightedLits.get(nLitImplied).equals(BigInteger.ONE)) {
                // then we know that the resolvant will still be a conflict (cf.
                // Dixon's property)
                this.coefMult = coefsCons[ind];
                this.coefMultCons = BigInteger.ONE;
                // updating of the degree of the conflict
                this.degree = this.degree.multiply(this.coefMult);
            } else if (coefsCons[ind].equals(BigInteger.ONE)) {
                // it is now again possible -
                // then we know that the resolvant will still be a conflict (cf.
                // Dixon's property)
                this.coefMultCons = this.weightedLits.get(nLitImplied);
                this.coefMult = BigInteger.ONE;
                // updating of the degree of the conflict
                degreeCons = degreeCons.multiply(this.coefMultCons);
            } else {
                // pb-constraint has to be reduced
                // to obtain a conflictual result from the cutting plane
                // DLB Findbugs warning ok
                assert positiveCoefs(coefsCons);
                degreeCons = reduceUntilConflict(litImplied, ind, coefsCons,
                        degreeCons, wpb);
                // updating of the degree of the conflict
                degreeCons = degreeCons.multiply(this.coefMultCons);
                this.degree = this.degree.multiply(this.coefMult);
            }

            // coefficients of the conflict must be multiplied by coefMult
            if (!this.coefMult.equals(BigInteger.ONE)) {
                for (int i = 0; i < size(); i++) {
                    changeCoef(i,
                            this.weightedLits.getCoef(i)
                                    .multiply(this.coefMult));
                }
            }

        }

        assert slackConflict().signum() <= 0;

        // cutting plane
        this.degree = cuttingPlane(cpb, degreeCons, coefsCons,
                this.coefMultCons, val);
        // neither litImplied nor nLitImplied is present in coefs structure
        assert !this.weightedLits.containsKey(litImplied);
        assert !this.weightedLits.containsKey(nLitImplied);
        // neither litImplied nor nLitImplied is present in byLevel structure
        assert getLevelByLevel(litImplied) == -1;
        assert getLevelByLevel(nLitImplied) == -1;
        assert this.degree.signum() > 0;
        assert slackConflict().signum() <= 0;

        // saturation
        this.degree = saturation();
        assert slackConflict().signum() <= 0;

        return this.degree;
    }

    /**
     * possReducedCoefs is used to update on the fly the slack of the wpb
     * constraint with reduced coefficients. possReducedCoefs is needed in
     * reduceUntilConflictConstraint; possReducedCoefs is computed first time in
     * reduceUntilConflict by a call to possConstraint and is modified directly
     * in reduceInConstraint and in saturation methods.
     */
    private BigInteger possReducedCoefs = BigInteger.ZERO;

    protected BigInteger reduceUntilConflict(int litImplied, int ind,
            BigInteger[] reducedCoefs, BigInteger degreeReduced, IWatchPb wpb) {
        BigInteger slackResolve = BigInteger.ONE.negate();
        BigInteger slackThis = BigInteger.ZERO;
        BigInteger slackIndex;
        BigInteger slackConflict = slackConflict();
        BigInteger ppcm;
        BigInteger reducedDegree = degreeReduced;
        BigInteger previousCoefLitImplied = BigInteger.ZERO;
        BigInteger tmp;
        BigInteger coefLitImplied = this.weightedLits.get(litImplied ^ 1);
        this.possReducedCoefs = possConstraint(wpb, reducedCoefs);
        do {
            if (slackResolve.signum() >= 0) {
                assert slackThis.signum() > 0;
                tmp = reduceInConstraint(wpb, reducedCoefs, ind, reducedDegree);
                assert tmp.compareTo(reducedDegree) < 0
                        && tmp.compareTo(BigInteger.ONE) >= 0;
                reducedDegree = tmp;
            }
            // search of the multiplying coefficients
            assert this.weightedLits.get(litImplied ^ 1).signum() > 0;
            assert reducedCoefs[ind].signum() > 0;

            if (!reducedCoefs[ind].equals(previousCoefLitImplied)) {
                assert coefLitImplied.equals(this.weightedLits
                        .get(litImplied ^ 1));
                ppcm = ppcm(reducedCoefs[ind], coefLitImplied);
                assert ppcm.signum() > 0;
                this.coefMult = ppcm.divide(coefLitImplied);
                this.coefMultCons = ppcm.divide(reducedCoefs[ind]);

                assert this.coefMultCons.signum() > 0;
                assert this.coefMult.signum() > 0;
                assert this.coefMult.multiply(coefLitImplied).equals(
                        this.coefMultCons.multiply(reducedCoefs[ind]));
                previousCoefLitImplied = reducedCoefs[ind];
            }

            // slacks computed for each constraint
            slackThis = this.possReducedCoefs.subtract(reducedDegree).multiply(
                    this.coefMultCons);
            assert slackThis.equals(wpb.slackConstraint(reducedCoefs,
                    reducedDegree).multiply(this.coefMultCons));
            assert slackConflict.equals(slackConflict());
            slackIndex = slackConflict.multiply(this.coefMult);
            assert slackIndex.signum() <= 0;
            // estimate of the slack after the cutting plane
            slackResolve = slackThis.add(slackIndex);
        } while (slackResolve.signum() >= 0);
        assert this.coefMult.multiply(this.weightedLits.get(litImplied ^ 1))
                .equals(this.coefMultCons.multiply(reducedCoefs[ind]));
        return reducedDegree;

    }

    private BigInteger possConstraint(IWatchPb wpb, BigInteger[] theCoefs) {
        BigInteger poss = BigInteger.ZERO;
        // for each literal
        for (int i = 0; i < wpb.size(); i++) {
            if (!this.voc.isFalsified(wpb.get(i))) {
                assert theCoefs[i].signum() >= 0;
                poss = poss.add(theCoefs[i]);
            }
        }
        return poss;
    }

    /**
     * computes the slack of the current instance
     */
    public BigInteger slackConflict() {
        BigInteger poss = BigInteger.ZERO;
        BigInteger tmp;
        // for each literal
        for (int i = 0; i < size(); i++) {
            tmp = this.weightedLits.getCoef(i);
            if (tmp.signum() != 0
                    && !this.voc.isFalsified(this.weightedLits.getLit(i))) {
                assert tmp.signum() > 0;
                poss = poss.add(tmp);
            }
        }
        return poss.subtract(this.degree);
    }

    public boolean oldIsAssertive(int dl) {
        BigInteger tmp;
        int lit;
        BigInteger slack = computeSlack(dl).subtract(this.degree);
        if (slack.signum() < 0) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            tmp = this.weightedLits.getCoef(i);
            lit = this.weightedLits.getLit(i);
            if (tmp.signum() > 0
                    && (this.voc.isUnassigned(lit) || this.voc.getLevel(lit) >= dl)
                    && slack.compareTo(tmp) < 0) {
                return true;
            }
        }
        return false;
    }

    // computes a slack with respect to a particular decision level
    private BigInteger computeSlack(int dl) {
        BigInteger slack = BigInteger.ZERO;
        int lit;
        BigInteger tmp;
        for (int i = 0; i < size(); i++) {
            tmp = this.weightedLits.getCoef(i);
            lit = this.weightedLits.getLit(i);
            if (tmp.signum() > 0
                    && (!this.voc.isFalsified(lit) || this.voc.getLevel(lit) >= dl)) {
                slack = slack.add(tmp);
            }
        }
        return slack;
    }

    /**
     * tests if the conflict is assertive (allows to imply a literal) at a
     * particular decision level
     * 
     * @param dl
     *            the decision level
     * @return true if the conflict is assertive at the decision level
     */
    public boolean isAssertive(int dl) {
        assert dl <= this.currentLevel;
        assert dl <= this.currentLevel;

        this.currentLevel = dl;
        BigInteger slack = this.currentSlack.subtract(this.degree);
        if (slack.signum() < 0) {
            return false;
        }
        return isImplyingLiteral(slack);
    }

    // given the slack already computed, tests if a literal could be implied at
    // a particular level
    // uses the byLevel data structure to parse each literal by decision level
    private boolean isImplyingLiteral(BigInteger slack) {
        // unassigned literals are tried first
        int unassigned = levelToIndex(-1);
        int lit;
        if (this.byLevel[unassigned] != null) {
            for (IteratorInt iterator = this.byLevel[unassigned].iterator(); iterator
                    .hasNext();) {
                lit = iterator.next();
                if (slack.compareTo(this.weightedLits.get(lit)) < 0) {
                    this.assertiveLiteral = this.weightedLits
                            .getFromAllLits(lit);
                    return true;
                }
            }
        }
        // then we have to look at every literal at a decision level >=dl
        BigInteger tmp;
        int level = levelToIndex(this.currentLevel);
        if (this.byLevel[level] != null) {
            for (IteratorInt iterator = this.byLevel[level].iterator(); iterator
                    .hasNext();) {
                lit = iterator.next();
                tmp = this.weightedLits.get(lit);
                if (tmp != null && slack.compareTo(tmp) < 0) {
                    this.assertiveLiteral = this.weightedLits
                            .getFromAllLits(lit);
                    return true;
                }
            }
        }
        return false;
    }

    // given the slack already computed, tests if a literal could be implied at
    // a particular level
    // uses the coefs data structure (where coefficients are decreasing ordered)
    // to parse each literal
    private boolean isImplyingLiteralOrdered(int dl, BigInteger slack) {
        int ilit, litLevel;
        for (int i = 0; i < size(); i++) {
            ilit = this.weightedLits.getLit(i);
            litLevel = this.voc.getLevel(ilit);
            if ((litLevel >= dl || this.voc.isUnassigned(ilit))
                    && slack.compareTo(this.weightedLits.getCoef(i)) < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * computes the least common factor of two integers (Plus Petit Commun
     * Multiple in french)
     * 
     * @param a
     *            first integer
     * @param b
     *            second integer
     * @return the least common factor
     */
    protected static BigInteger ppcm(BigInteger a, BigInteger b) {
        return a.divide(a.gcd(b)).multiply(b);
    }

    /**
     * constraint reduction : removes a literal of the constraint. The literal
     * should be either unassigned or satisfied. The literal can not be the
     * literal that should be resolved.
     * 
     * @param wpb
     *            the initial constraint to reduce
     * @param coefsBis
     *            the coefficients of the constraint wrt which the reduction
     *            will be proposed
     * @param indLitImplied
     *            index in wpb of the literal that should be resolved
     * @param degreeBis
     *            the degree of the constraint wrt which the reduction will be
     *            proposed
     * @return new degree of the reduced constraint
     */
    public BigInteger reduceInConstraint(IWatchPb wpb,
            final BigInteger[] coefsBis, final int indLitImplied,
            final BigInteger degreeBis) {
        assert degreeBis.compareTo(BigInteger.ONE) > 0;
        // search of an unassigned literal
        int lit = -1;
        int size = wpb.size();
        for (int ind = 0; ind < size && lit == -1; ind++) {
            if (coefsBis[ind].signum() != 0
                    && this.voc.isUnassigned(wpb.get(ind))) {
                assert coefsBis[ind].compareTo(degreeBis) < 0;
                lit = ind;
            }
        }

        // else, search of a satisfied literal
        if (lit == -1) {
            for (int ind = 0; ind < size && lit == -1; ind++) {
                if (coefsBis[ind].signum() != 0
                        && this.voc.isSatisfied(wpb.get(ind))
                        && ind != indLitImplied) {
                    lit = ind;
                }
            }
        }

        // a literal has been found
        assert lit != -1;

        assert lit != indLitImplied;
        // reduction can be done
        BigInteger degUpdate = degreeBis.subtract(coefsBis[lit]);
        this.possReducedCoefs = this.possReducedCoefs.subtract(coefsBis[lit]);
        coefsBis[lit] = BigInteger.ZERO;
        assert this.possReducedCoefs.equals(possConstraint(wpb, coefsBis));

        // saturation of the constraint
        degUpdate = saturation(coefsBis, degUpdate, wpb);

        assert coefsBis[indLitImplied].signum() > 0;
        assert degreeBis.compareTo(degUpdate) > 0;
        assert this.possReducedCoefs.equals(possConstraint(wpb, coefsBis));
        return degUpdate;
    }

    private BigInteger saturation(BigInteger[] coefs, BigInteger degree,
            IWatchPb wpb) {
        assert degree.signum() > 0;
        BigInteger degreeResult = degree;
        boolean isMinimumEqualsToDegree = true;
        int comparison;
        for (int i = 0; i < coefs.length; i++) {
            comparison = coefs[i].compareTo(degree);
            if (comparison > 0) {
                if (!this.voc.isFalsified(wpb.get(i))) {
                    this.possReducedCoefs = this.possReducedCoefs
                            .subtract(coefs[i]);
                    this.possReducedCoefs = this.possReducedCoefs.add(degree);
                }
                coefs[i] = degree;
            } else if (comparison < 0 && coefs[i].signum() > 0) {
                isMinimumEqualsToDegree = false;
            }

        }
        if (isMinimumEqualsToDegree && !degree.equals(BigInteger.ONE)) {
            // the result is a clause
            // there is no more possible reduction
            this.possReducedCoefs = BigInteger.ZERO;
            degreeResult = BigInteger.ONE;
            for (int i = 0; i < coefs.length; i++) {
                if (coefs[i].signum() > 0) {
                    coefs[i] = BigInteger.ONE;
                    if (!this.voc.isFalsified(wpb.get(i))) {
                        this.possReducedCoefs = this.possReducedCoefs
                                .add(BigInteger.ONE);
                    }
                }
            }
        }
        return degreeResult;
    }

    private static boolean positiveCoefs(final BigInteger[] coefsCons) {
        for (BigInteger coefsCon : coefsCons) {
            if (coefsCon.signum() < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * computes the level for the backtrack : the highest decision level for
     * which the conflict is assertive.
     * 
     * @param maxLevel
     *            the lowest level for which the conflict is assertive
     * @return the highest level (smaller int) for which the constraint is
     *         assertive.
     */
    public int getBacktrackLevel(int maxLevel) {
        // we are looking for a level higher than maxLevel
        // where the constraint is still assertive
        VecInt lits;
        int level;
        int indStop = levelToIndex(maxLevel) - 1;
        int indStart = levelToIndex(0);
        BigInteger slack = computeSlack(0).subtract(this.degree);
        int previous = 0;
        for (int indLevel = indStart; indLevel <= indStop; indLevel++) {
            if (this.byLevel[indLevel] != null) {
                level = indexToLevel(indLevel);
                assert computeSlack(level).subtract(this.degree).equals(slack);
                if (isImplyingLiteralOrdered(level, slack)) {
                    break;
                }
                // updating the new slack
                lits = this.byLevel[indLevel];
                int lit;
                for (IteratorInt iterator = lits.iterator(); iterator.hasNext();) {
                    lit = iterator.next();
                    if (this.voc.isFalsified(lit)
                            && this.voc.getLevel(lit) == indexToLevel(indLevel)) {
                        slack = slack.subtract(this.weightedLits.get(lit));
                    }
                }
                if (!lits.isEmpty()) {
                    previous = level;
                }
            }
        }
        assert previous == oldGetBacktrackLevel(maxLevel);
        return previous;
    }

    public int oldGetBacktrackLevel(int maxLevel) {
        int litLevel;
        int borneMax = maxLevel;
        // assert isAssertive(borneMax);
        // assert oldIsAssertive(borneMax);
        int borneMin = -1;
        // borneMax is the highest level in the search tree where the constraint
        // is assertive
        for (int i = 0; i < size(); i++) {
            litLevel = this.voc.getLevel(this.weightedLits.getLit(i));
            if (litLevel < borneMax && litLevel > borneMin
                    && oldIsAssertive(litLevel)) {
                borneMax = litLevel;
            }
        }
        // the level returned is the first level below borneMax
        // where there is a literal belonging to the constraint
        int retour = 0;
        for (int i = 0; i < size(); i++) {
            litLevel = this.voc.getLevel(this.weightedLits.getLit(i));
            if (litLevel > retour && litLevel < borneMax) {
                retour = litLevel;
            }
        }
        return retour;
    }

    public void updateSlack(int level) {
        int dl = levelToIndex(level);
        if (this.byLevel[dl] != null) {
            int lit;
            for (IteratorInt iterator = this.byLevel[dl].iterator(); iterator
                    .hasNext();) {
                lit = iterator.next();
                if (this.voc.isFalsified(lit)) {
                    this.currentSlack = this.currentSlack.add(this.weightedLits
                            .get(lit));
                }
            }
        }
    }

    @Override
    void increaseCoef(int lit, BigInteger incCoef) {
        if (!this.voc.isFalsified(lit)
                || this.voc.getLevel(lit) == this.currentLevel) {
            this.currentSlack = this.currentSlack.add(incCoef);
        }
        assert this.byLevel[levelToIndex(this.voc.getLevel(lit))].contains(lit);
        super.increaseCoef(lit, incCoef);
    }

    @Override
    void decreaseCoef(int lit, BigInteger decCoef) {
        if (!this.voc.isFalsified(lit)
                || this.voc.getLevel(lit) == this.currentLevel) {
            this.currentSlack = this.currentSlack.subtract(decCoef);
        }
        assert this.byLevel[levelToIndex(this.voc.getLevel(lit))].contains(lit);
        super.decreaseCoef(lit, decCoef);
    }

    @Override
    void setCoef(int lit, BigInteger newValue) {
        int litLevel = this.voc.getLevel(lit);
        if (!this.voc.isFalsified(lit) || litLevel == this.currentLevel) {
            if (this.weightedLits.containsKey(lit)) {
                this.currentSlack = this.currentSlack
                        .subtract(this.weightedLits.get(lit));
            }
            this.currentSlack = this.currentSlack.add(newValue);
        }
        int indLitLevel = levelToIndex(litLevel);
        if (!this.weightedLits.containsKey(lit)) {
            if (this.byLevel[indLitLevel] == null) {
                this.byLevel[indLitLevel] = new VecInt();
            }
            this.byLevel[indLitLevel].push(lit);

        }
        assert this.byLevel[indLitLevel] != null;
        assert this.byLevel[indLitLevel].contains(lit);
        super.setCoef(lit, newValue);
    }

    @Override
    void changeCoef(int indLit, BigInteger newValue) {
        int lit = this.weightedLits.getLit(indLit);
        int litLevel = this.voc.getLevel(lit);
        if (!this.voc.isFalsified(lit) || litLevel == this.currentLevel) {
            if (this.weightedLits.containsKey(lit)) {
                this.currentSlack = this.currentSlack
                        .subtract(this.weightedLits.get(lit));
            }
            this.currentSlack = this.currentSlack.add(newValue);
        }
        int indLitLevel = levelToIndex(litLevel);
        assert this.weightedLits.containsKey(lit);
        assert this.byLevel[indLitLevel] != null;
        assert this.byLevel[indLitLevel].contains(lit);
        super.changeCoef(indLit, newValue);
    }

    @Override
    void removeCoef(int lit) {
        int litLevel = this.voc.getLevel(lit);
        if (!this.voc.isFalsified(lit) || litLevel == this.currentLevel) {
            this.currentSlack = this.currentSlack.subtract(this.weightedLits
                    .get(lit));
        }
        int indLitLevel = levelToIndex(litLevel);
        assert indLitLevel < this.byLevel.length;
        assert this.byLevel[indLitLevel] != null;
        assert this.byLevel[indLitLevel].contains(lit);
        this.byLevel[indLitLevel].remove(lit);
        super.removeCoef(lit);
    }

    private int getLevelByLevel(int lit) {
        for (int i = 0; i < this.byLevel.length; i++) {
            if (this.byLevel[i] != null && this.byLevel[i].contains(lit)) {
                return i;
            }
        }
        return -1;
    }

    public boolean slackIsCorrect(int dl) {
        return this.currentSlack.equals(computeSlack(dl));
    }

    @Override
    public String toString() {
        int lit;
        StringBuffer stb = new StringBuffer();
        for (int i = 0; i < size(); i++) {
            lit = this.weightedLits.getLit(i);
            stb.append(this.weightedLits.getCoef(i));
            stb.append(".");
            stb.append(Lits.toString(lit));
            stb.append(" ");
            stb.append("[");
            stb.append(this.voc.valueToString(lit));
            stb.append("@");
            stb.append(this.voc.getLevel(lit));
            stb.append("]");
        }
        return stb.toString() + " >= " + this.degree; //$NON-NLS-1$
    }

    public boolean hasBeenReduced() {
        return this.hasBeenReduced;
    }

    public long getNumberOfReductions() {
        return this.numberOfReductions;
    }

}
