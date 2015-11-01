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
package org.sat4j.pb;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IOptimizationProblem;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

/**
 * A PBSolverHandle is a PBSolverDecorator in which it is possible to change the
 * decorated solver, in contrast with classical decorators.
 * 
 * @author leberre
 * @since 2.3.6
 * 
 */
public class PBSolverHandle extends PBSolverDecorator implements
        IOptimizationProblem {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public PBSolverHandle(IPBSolver solver) {
        super(solver);
        if (!(solver instanceof IOptimizationProblem)) {
            throw new IllegalArgumentException(
                    "We need also optimization problem here!");
        }
    }

    public void changeDecorated(IPBSolver solver) {
        setDecorated(solver);
    }

    public boolean admitABetterSolution() throws TimeoutException {
        return ((IOptimizationProblem) decorated()).admitABetterSolution();
    }

    public boolean admitABetterSolution(IVecInt assumps)
            throws TimeoutException {
        return ((IOptimizationProblem) decorated())
                .admitABetterSolution(assumps);
    }

    public boolean hasNoObjectiveFunction() {
        return ((IOptimizationProblem) decorated()).hasNoObjectiveFunction();
    }

    public boolean nonOptimalMeansSatisfiable() {
        return ((IOptimizationProblem) decorated())
                .nonOptimalMeansSatisfiable();
    }

    public Number calculateObjective() {
        return ((IOptimizationProblem) decorated()).calculateObjective();
    }

    public Number getObjectiveValue() {
        return ((IOptimizationProblem) decorated()).getObjectiveValue();
    }

    public void forceObjectiveValueTo(Number forcedValue)
            throws ContradictionException {
        ((IOptimizationProblem) decorated()).forceObjectiveValueTo(forcedValue);
    }

    public void discard() throws ContradictionException {
        ((IOptimizationProblem) decorated()).discard();
    }

    public void discardCurrentSolution() throws ContradictionException {
        ((IOptimizationProblem) decorated()).discardCurrentSolution();
    }

    public boolean isOptimal() {
        return ((IOptimizationProblem) decorated()).isOptimal();
    }

    public void setTimeoutForFindingBetterSolution(int seconds) {
        ((IOptimizationProblem) decorated())
                .setTimeoutForFindingBetterSolution(seconds);
    }
}
