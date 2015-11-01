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
import org.sat4j.specs.UnitPropagationListener;

public final class PuebloMinWatchPb extends MinWatchPb {

    private static final long serialVersionUID = 1L;

    /**
     * Constructeur de base des contraintes
     * 
     * @param voc
     *            Informations sur le vocabulaire employ???
     * @param ps
     *            Liste des litt???raux
     * @param weightedLits
     *            Liste des coefficients
     * @param moreThan
     *            Indication sur le comparateur
     * @param degree
     *            Stockage du degr??? de la contrainte
     */
    private PuebloMinWatchPb(ILits voc, int[] lits, BigInteger[] coefs,
            BigInteger degree, BigInteger sumCoefs) {
        super(voc, lits, coefs, degree, sumCoefs);
    }

    private PuebloMinWatchPb(ILits voc, IDataStructurePB mpb) {

        super(voc, mpb);
    }

    /**
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
    public static PuebloMinWatchPb normalizedMinWatchPbNew(
            UnitPropagationListener s, ILits voc, int[] lits,
            BigInteger[] coefs, BigInteger degree)
            throws ContradictionException {
        // Il ne faut pas modifier les param?tres
        BigInteger sumCoefs = BigInteger.ZERO;
        for (BigInteger c : coefs) {
            sumCoefs = sumCoefs.add(c);
        }
        PuebloMinWatchPb outclause = new PuebloMinWatchPb(voc, lits, coefs,
                degree, sumCoefs);

        if (outclause.degree.signum() <= 0) {
            return null;
        }

        outclause.computeWatches();

        outclause.computePropagation(s);

        return outclause;

    }

    public static WatchPb normalizedWatchPbNew(ILits voc, IDataStructurePB mpb) {
        return new PuebloMinWatchPb(voc, mpb);
    }

    @Override
    protected BigInteger maximalCoefficient(int pIndice) {
        return this.coefs[0];
    }

    @Override
    protected BigInteger updateWatched(BigInteger mc, int pIndice) {
        BigInteger maxCoef = mc;
        if (this.watchingCount < size()) {
            BigInteger upWatchCumul = this.watchCumul
                    .subtract(this.coefs[pIndice]);
            BigInteger borneSup = this.degree.add(maxCoef);
            for (int ind = 0; ind < this.lits.length
                    && upWatchCumul.compareTo(borneSup) < 0; ind++) {
                if (!this.voc.isFalsified(this.lits[ind]) && !this.watched[ind]) {
                    upWatchCumul = upWatchCumul.add(this.coefs[ind]);
                    this.watched[ind] = true;
                    assert this.watchingCount < size();
                    this.watching[this.watchingCount++] = ind;
                    this.voc.watch(this.lits[ind] ^ 1, this);
                }
            }
            this.watchCumul = upWatchCumul.add(this.coefs[pIndice]);
        }
        return maxCoef;
    }

}
