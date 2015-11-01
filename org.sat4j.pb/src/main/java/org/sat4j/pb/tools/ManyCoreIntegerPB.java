/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004, 2013 Artois University and CNRS
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

import org.sat4j.core.ASolverFactory;
import org.sat4j.core.ConstrGroup;
import org.sat4j.pb.IIntegerPBSolver;
import org.sat4j.pb.core.IntegerVariable;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public class ManyCoreIntegerPB<S extends IIntegerPBSolver> extends
        ManyCorePB<S> implements IIntegerPBSolver {

    private static final long serialVersionUID = 1L;

    public ManyCoreIntegerPB(ASolverFactory<S> factory, String... solverNames) {
        super(factory, solverNames);
    }

    public ManyCoreIntegerPB(S... iSolver) {
        super(iSolver);
    }

    public IntegerVariable newIntegerVar(BigInteger maxValue) {
        IntegerVariable res = this.solvers.get(0).newIntegerVar(maxValue);
        for (int i = 1; i < this.numberOfSolvers; ++i) {
            this.solvers.get(i).newIntegerVar(maxValue);
        }
        return res;
    }

    public IConstr addAtLeast(IntegerVariable var, int degree)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtLeast(var, degree));
        }
        return group;
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger degree)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtLeast(literals, coeffs,
                    integerVars, integerVarsCoeffs, degree));
        }
        return group;
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int degree)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtLeast(literals, coeffs,
                    integerVars, integerVarsCoeffs, degree));
        }
        return group;
    }

    public IConstr addAtMost(IntegerVariable var, int degree)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtMost(var, degree));
        }
        return group;
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger degree)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtMost(literals, coeffs,
                    integerVars, integerVarsCoeffs, degree));
        }
        return group;
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int degree)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtMost(literals, coeffs,
                    integerVars, integerVarsCoeffs, degree));
        }
        return group;
    }

    public IConstr addExactly(IntegerVariable var, int degree)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addExactly(var, degree));
        }
        return group;
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger weight)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addExactly(literals, coeffs,
                    integerVars, integerVarsCoeffs, weight));
        }
        return group;
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int weight)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addExactly(literals, coeffs,
                    integerVars, integerVarsCoeffs, weight));
        }
        return group;
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, boolean moreThan, BigInteger d)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addPseudoBoolean(lits, coeffs,
                    integerVars, integerVarsCoeffs, moreThan, d));
        }
        return group;
    }

    public void addIntegerVariableToObjectiveFunction(IntegerVariable var,
            BigInteger weight) {
        for (int i = 0; i < this.numberOfSolvers; i++) {
            this.solvers.get(i).addIntegerVariableToObjectiveFunction(var,
                    weight);
        }
    }

    public BigInteger getIntegerVarValue(IntegerVariable var) {
        return this.solvers.get(this.winnerId).getIntegerVarValue(var);
    }

}
