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

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.ILits;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public abstract class Pseudos {

    public static IDataStructurePB niceCheckedParameters(IVecInt ps,
            IVec<BigInteger> bigCoefs, boolean moreThan, BigInteger bigDeg,
            ILits voc) {
        assert ps.size() != 0 && ps.size() == bigCoefs.size();
        int[] lits = new int[ps.size()];
        ps.copyTo(lits);
        BigInteger[] bc = new BigInteger[bigCoefs.size()];
        bigCoefs.copyTo(bc);
        BigInteger bigDegree = Pseudos.niceCheckedParametersForCompetition(
                lits, bc, moreThan, bigDeg);

        IDataStructurePB mpb = new MapPb(voc.nVars() * 2 + 2);
        if (bigDegree.signum() > 0) {
            bigDegree = mpb.cuttingPlane(lits, bc, bigDegree);
        }
        if (bigDegree.signum() > 0) {
            bigDegree = mpb.saturation();
        }
        if (bigDegree.signum() <= 0) {
            return null;
        }
        return mpb;
    }

    public static BigInteger niceCheckedParametersForCompetition(int[] lits,
            BigInteger[] bc, boolean moreThan, BigInteger bigDeg) {
        BigInteger bigDegree = bigDeg;
        if (!moreThan) {
            for (int i = 0; i < lits.length; i++) {
                bc[i] = bc[i].negate();
            }
            bigDegree = bigDegree.negate();
        }

        for (int i = 0; i < bc.length; i++) {
            if (bc[i].signum() < 0) {
                lits[i] = lits[i] ^ 1;
                bc[i] = bc[i].negate();
                bigDegree = bigDegree.add(bc[i]);
            }
        }

        for (int i = 0; i < bc.length; i++) {
            if (bc[i].compareTo(bigDegree) > 0) {
                bc[i] = bigDegree;
            }
        }

        return bigDegree;

    }

    public static IDataStructurePB niceParameters(IVecInt ps,
            IVec<BigInteger> bigCoefs, boolean moreThan, BigInteger bigDeg,
            ILits voc) throws ContradictionException {
        // Ajouter les simplifications quand la structure sera d?finitive
        if (ps.size() == 0) {
            if (moreThan && bigDeg.signum() > 0 || !moreThan
                    && bigDeg.signum() < 0) {
                throw new ContradictionException("Creating Empty clause ?");
            }
            // ignoring tautological constraint
            return null;
        }
        if (ps.size() != bigCoefs.size()) {
            throw new IllegalArgumentException(
                    "Contradiction dans la taille des tableaux ps=" + ps.size()
                            + " coefs=" + bigCoefs.size() + ".");
        }
        return niceCheckedParameters(ps, bigCoefs, moreThan, bigDeg, voc);
    }

    // BEWARE: here the contract is to return bigDeg if no simplification occurs
    public static BigInteger niceParametersForCompetition(int[] ps,
            BigInteger[] bigCoefs, boolean moreThan, BigInteger bigDeg)
            throws ContradictionException {
        // Ajouter les simplifications quand la structure sera d?finitive
        if (ps.length == 0) {
            if (moreThan && bigDeg.signum() > 0 || !moreThan
                    && bigDeg.signum() < 0) {
                throw new ContradictionException("Creating Empty clause ?");
            }
            // ignoring tautological constraint
            return bigDeg;
        }
        if (ps.length != bigCoefs.length) {
            throw new IllegalArgumentException(
                    "Contradiction dans la taille des tableaux ps=" + ps.length
                            + " coefs=" + bigCoefs.length + ".");
        }
        return niceCheckedParametersForCompetition(ps, bigCoefs, moreThan,
                bigDeg);
    }

    public static IVec<BigInteger> toVecBigInt(IVecInt vec) {
        IVec<BigInteger> bigVec = new Vec<BigInteger>(vec.size());
        for (int i = 0; i < vec.size(); ++i) {
            bigVec.push(BigInteger.valueOf(vec.get(i)));
        }
        return bigVec;
    }

    public static BigInteger toBigInt(int i) {
        return BigInteger.valueOf(i);
    }

    public static ObjectiveFunction normalizeObjective(ObjectiveFunction initial) {
        IVec<BigInteger> initCoeffs = initial.getCoeffs();
        IVecInt initLits = initial.getVars();
        assert initCoeffs.size() == initLits.size();
        Map<Integer, BigInteger> reduced = new HashMap<Integer, BigInteger>();
        int lit;
        for (int i = 0; i < initLits.size(); i++) {
            lit = initLits.get(i);
            BigInteger oldCoef = reduced.get(lit);
            if (oldCoef == null) {
                reduced.put(lit, initCoeffs.get(i));
            } else {
                reduced.put(lit, oldCoef.add(initCoeffs.get(i)));
            }
        }
        assert reduced.size() <= initLits.size();
        if (reduced.size() < initLits.size()) {
            IVecInt newLits = new VecInt(reduced.size());
            IVec<BigInteger> newCoefs = new Vec<BigInteger>(reduced.size());
            for (Map.Entry<Integer, BigInteger> entry : reduced.entrySet()) {
                newLits.push(entry.getKey());
                newCoefs.push(entry.getValue());
            }
            return new ObjectiveFunction(newLits, newCoefs);
        }
        return initial;
    }

}
