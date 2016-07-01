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

import org.sat4j.minisat.constraints.cnf.LearntHTClause;
import org.sat4j.minisat.core.ILits;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.UnitPropagationListener;

public final class LearntHTClausePB extends LearntHTClause implements PBConstr {

    private static final long serialVersionUID = 1L;

    public LearntHTClausePB(IVecInt ps, ILits voc) {
        super(ps, voc);
    }

    @Override
    public void assertConstraint(UnitPropagationListener s) {
        if (getVocabulary().isUnassigned(this.head)) {
            s.enqueue(this.head, this);
        } else if (getVocabulary().isUnassigned(this.tail)) {
            s.enqueue(this.tail, this);
        } else {
            for (int i = 0; i < this.middleLits.length; i++) {
                if (getVocabulary().isUnassigned(this.middleLits[i])) {
                    int temp = this.middleLits[i];
                    this.middleLits[i] = this.head;
                    this.head = temp;
                    s.enqueue(temp, this);
                    break;
                }
            }
        }
    }

    public IVecInt computeAnImpliedClause() {
        return null;
    }

    public BigInteger getCoef(int literal) {
        return BigInteger.ONE;
    }

    public BigInteger[] getCoefs() {
        BigInteger[] tmp = new BigInteger[size()];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = BigInteger.ONE;
        }
        return tmp;
    }

    public BigInteger getDegree() {
        return BigInteger.ONE;
    }

}
