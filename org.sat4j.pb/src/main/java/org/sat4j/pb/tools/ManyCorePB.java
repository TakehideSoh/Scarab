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
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.tools.ManyCore;

public class ManyCorePB<S extends IPBSolver> extends ManyCore<S> implements
        IPBSolver {

    private static final long serialVersionUID = 1L;

    public ManyCorePB(ASolverFactory<S> factory, String... solverNames) {
        super(factory, solverNames);
    }

    public ManyCorePB(S... iSolver) {
        super(iSolver);
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger d) throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addPseudoBoolean(lits, coeffs,
                    moreThan, d));
        }
        return group;
    }

    public void setObjectiveFunction(ObjectiveFunction obj) {
        for (int i = 0; i < this.numberOfSolvers; i++) {
            this.solvers.get(i).setObjectiveFunction(obj);
        }
    }

    public ObjectiveFunction getObjectiveFunction() {
        return this.solvers.get(0).getObjectiveFunction();
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtMost(literals, coeffs, degree));
        }
        return group;
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtMost(literals, coeffs, degree));
        }
        return group;
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtLeast(literals, coeffs, degree));
        }
        return group;
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addAtLeast(literals, coeffs, degree));
        }
        return group;
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
            throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addExactly(literals, coeffs, weight));
        }
        return group;
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight) throws ContradictionException {
        ConstrGroup group = new ConstrGroup(false);
        for (int i = 0; i < this.numberOfSolvers; i++) {
            group.add(this.solvers.get(i).addExactly(literals, coeffs, weight));
        }
        return group;
    }

}
