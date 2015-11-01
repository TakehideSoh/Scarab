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

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.ILits;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

/**
 * @author anne
 * 
 */
public class InternalMapPBStructure {

    private final IVecInt lits;
    private final IVec<BigInteger> coefs;
    private IVecInt allLits;
    protected BigInteger degree;

    // temporarily : just for the case where an InternalMapPBStructure
    // is used to embed in one object literals and coefficients
    InternalMapPBStructure(IVecInt lits, IVec<BigInteger> coefs) {
        this.lits = lits;
        this.coefs = coefs;
    }

    InternalMapPBStructure(int size) {
        assert size > 0;
        this.allLits = new VecInt(size, -1);
        this.coefs = new Vec<BigInteger>();
        this.lits = new VecInt();
    }

    InternalMapPBStructure(PBConstr cpb, int level, boolean noRemove) {
        ILits voc = cpb.getVocabulary();
        this.allLits = new VecInt(cpb.getVocabulary().nVars() * 2 + 2, -1);
        this.coefs = new Vec<BigInteger>(cpb.size());
        this.lits = new VecInt(cpb.size());
        int lit;
        int ind = 0;
        BigInteger degree = cpb.getDegree();
        BigInteger coef;
        boolean clause = degree.equals(BigInteger.ONE);
        for (int i = 0; i < cpb.size(); i++) {
            assert cpb.get(i) != 0;
            assert cpb.getCoef(i).signum() > 0;
            lit = cpb.get(i);
            if (noRemove || clause
                    || !(voc.isSatisfied(lit) && voc.getLevel(lit) < level)) {
                // the literal is kept
                this.lits.push(lit);
                assert ind + 1 == this.lits.size();
                this.allLits.set(lit, ind);
                this.coefs.push(cpb.getCoef(i));
                ind = ind + 1;
            } else {
                // the literal is forgotten
                coef = cpb.getCoef(i);
                degree = degree.subtract(coef);
            }
        }
        this.degree = degree;
    }

    public BigInteger getComputedDegree() {
        return this.degree;
    }

    BigInteger get(int lit) {
        assert this.allLits.get(lit) != -1;
        return this.coefs.get(this.allLits.get(lit));
    }

    int getFromAllLits(int lit) {
        return this.allLits.get(lit);
    }

    int getLit(int indLit) {
        assert indLit < this.lits.size();
        return this.lits.get(indLit);
    }

    BigInteger getCoef(int indLit) {
        assert indLit < this.coefs.size();
        return this.coefs.get(indLit);
    }

    boolean containsKey(int lit) {
        return this.allLits.get(lit) != -1;
    }

    int size() {
        return this.lits.size();
    }

    void put(int lit, BigInteger newValue) {
        int indLit = this.allLits.get(lit);
        if (indLit != -1) {
            this.coefs.set(indLit, newValue);
        } else {
            this.lits.push(lit);
            this.coefs.push(newValue);
            this.allLits.set(lit, this.lits.size() - 1);
        }
    }

    void changeCoef(int indLit, BigInteger newValue) {
        assert indLit <= this.coefs.size();
        this.coefs.set(indLit, newValue);
    }

    void remove(int lit) {
        int indLit = this.allLits.get(lit);
        if (indLit != -1) {
            int tmp = this.lits.last();
            this.coefs.delete(indLit);
            this.lits.delete(indLit);
            this.allLits.set(tmp, indLit);
            this.allLits.set(lit, -1);
        }
    }

    void copyCoefs(IVec<BigInteger> dest) {
        this.coefs.copyTo(dest);
    }

    void copyCoefs(BigInteger[] dest) {
        this.coefs.copyTo(dest);
    }

    void copyLits(IVecInt dest) {
        this.lits.copyTo(dest);
    }

    void copyLits(int[] dest) {
        this.lits.copyTo(dest);
    }
}
