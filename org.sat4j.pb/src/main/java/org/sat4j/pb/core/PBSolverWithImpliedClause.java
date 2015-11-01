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
package org.sat4j.pb.core;

import java.math.BigInteger;

import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.LearningStrategy;
import org.sat4j.pb.constraints.pb.PBConstr;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public class PBSolverWithImpliedClause extends PBSolverCP {

    public PBSolverWithImpliedClause(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, IOrder order) {
        super(learner, dsf, order);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public IConstr addPseudoBoolean(IVecInt literals, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger degree) throws ContradictionException {
        IVecInt vlits = dimacs2internal(literals);
        assert vlits.size() == literals.size();
        assert literals.size() == coeffs.size();
        PBConstr result = (PBConstr) this.dsfactory
                .createPseudoBooleanConstraint(vlits, coeffs, moreThan, degree);
        if (result != null) {
            IVecInt clits = result.computeAnImpliedClause();
            if (clits != null) {
                addConstr(this.dsfactory.createClause(clits));
            }
        }
        return addConstr(result);
    }

    @Override
    public String toString(String prefix) {
        return super.toString(prefix) + "\n" + prefix
                + "Add implied clauses in preprocessing";
    }
}
