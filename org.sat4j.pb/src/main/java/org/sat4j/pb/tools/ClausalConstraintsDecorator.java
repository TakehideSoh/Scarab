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
package org.sat4j.pb.tools;

import java.math.BigInteger;

import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.tools.ClausalCardinalitiesDecorator;
import org.sat4j.tools.encoding.EncodingStrategyAdapter;
import org.sat4j.tools.encoding.Policy;

public class ClausalConstraintsDecorator extends
        ClausalCardinalitiesDecorator<IPBSolver> implements IPBSolver {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private final IPBSolver solver;

    public ClausalConstraintsDecorator(IPBSolver solver,
            EncodingStrategyAdapter encodingAd) {
        super(solver, encodingAd);
        this.solver = solver;
    }

    public ClausalConstraintsDecorator(IPBSolver solver) {
        super(solver, new Policy());
        this.solver = solver;
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger d) throws ContradictionException {
        if (isCardinality(coeffs)) {
            if (moreThan) {
                return super.addAtLeast(lits, d.intValue());
            } else {
                return super.addAtMost(lits, d.intValue());
            }
        } else {
            return solver.addPseudoBoolean(lits, coeffs, moreThan, d);
        }
    }

    public void setObjectiveFunction(ObjectiveFunction obj) {
        solver.setObjectiveFunction(obj);
    }

    public ObjectiveFunction getObjectiveFunction() {
        return solver.getObjectiveFunction();
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        if (isCardinality(coeffs)) {
            return super.addAtMost(literals, degree);
        } else {
            return solver.addAtMost(literals, coeffs, degree);
        }
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        if (isCardinality(coeffs)) {
            return super.addAtMost(literals, degree.intValue());
        } else {
            return solver.addAtMost(literals, coeffs, degree);
        }
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        if (isCardinality(coeffs)) {
            return super.addAtLeast(literals, degree);
        } else {
            return solver.addAtLeast(literals, coeffs, degree);
        }
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        if (isCardinality(coeffs)) {
            return super.addAtLeast(literals, degree.intValue());
        } else {
            return solver.addAtLeast(literals, coeffs, degree);
        }
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
            throws ContradictionException {
        if (isCardinality(coeffs)) {
            return super.addExactly(literals, weight);
        } else {
            return solver.addExactly(literals, coeffs, weight);
        }
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight) throws ContradictionException {
        if (isCardinality(coeffs)) {
            return super.addExactly(literals, weight.intValue());
        } else {
            return solver.addExactly(literals, coeffs, weight);
        }
    }

    public static boolean isCardinality(IVecInt coeffs) {
        boolean result = true;
        int i = 0;
        while (result && i < coeffs.size()) {
            result = (coeffs.get(i) == 1);
            i++;
        }
        return result;
    }

    public static boolean isCardinality(IVec<BigInteger> coeffs) {
        boolean result = true;
        int i = 0;
        while (result && i < coeffs.size()) {
            result = (coeffs.get(i).equals(BigInteger.ONE));
            i++;
        }
        return result;
    }
}
