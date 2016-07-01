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

import java.math.BigInteger;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IOptimizationProblem;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

/**
 * 
 * @author lonca
 * 
 */

public class ConstraintRelaxingPseudoOptDecorator extends PBSolverDecorator
        implements IOptimizationProblem {

    private static final long serialVersionUID = 1L;
    private IConstr previousPBConstr;
    private IConstr addedConstr = null;
    private Number objectiveValue;
    private boolean optimumFound = false;

    public ConstraintRelaxingPseudoOptDecorator(IPBSolver solver) {
        super(solver);
    }

    @Override
    public boolean isSatisfiable() throws TimeoutException {
        return isSatisfiable(VecInt.EMPTY);
    }

    @Override
    public boolean isSatisfiable(boolean global) throws TimeoutException {
        return isSatisfiable(VecInt.EMPTY, global);
    }

    @Override
    public boolean isSatisfiable(IVecInt assumps, boolean global)
            throws TimeoutException {
        boolean result = super.isSatisfiable(assumps, true);
        if (result) {
            calculateObjective();
        } else {
            if (this.previousPBConstr != null) {
                decorated().removeConstr(this.previousPBConstr);
                this.previousPBConstr = null;
            }
        }
        return result;
    }

    @Override
    public boolean isSatisfiable(IVecInt assumps) throws TimeoutException {
        return isSatisfiable(assumps, true);
    }

    public boolean admitABetterSolution() throws TimeoutException {
        return admitABetterSolution(VecInt.EMPTY);
    }

    public boolean admitABetterSolution(IVecInt assumps)
            throws TimeoutException {
        boolean isSatisfiable;

        if (this.optimumFound) {
            return false;
        }
        int maxValue = getObjectiveFunction().minValue().intValue();
        while (true) {
            if (this.addedConstr != null) {
                this.decorated().removeConstr(this.addedConstr);
            }
            try {
                forceObjectiveValueTo(maxValue++);
            } catch (ContradictionException e) {
                if (isVerbose()) {
                    System.out.println(decorated().getLogPrefix()
                            + "no solution for objective value "
                            + (maxValue - 1));
                }
                continue;
            }
            isSatisfiable = super.isSatisfiable(assumps, true);
            if (isSatisfiable) {
                this.optimumFound = true;
                if (getObjectiveFunction() != null) {
                    calculateObjective();
                }
                this.decorated().removeConstr(this.addedConstr);
                return true;
            }
            if (isVerbose()) {
                System.out.println(decorated().getLogPrefix()
                        + "no solution for objective value " + (maxValue - 1));
            }
        }
    }

    public boolean hasNoObjectiveFunction() {
        return getObjectiveFunction() == null;
    }

    public boolean nonOptimalMeansSatisfiable() {
        return false;
    }

    @Deprecated
    public Number calculateObjective() {
        if (getObjectiveFunction() == null) {
            throw new UnsupportedOperationException(
                    "The problem does not contain an objective function");
        }
        this.objectiveValue = getObjectiveFunction().calculateDegree(this);
        return this.objectiveValue;
    }

    public Number getObjectiveValue() {
        return this.objectiveValue;
    }

    public void forceObjectiveValueTo(Number forcedValue)
            throws ContradictionException {
        this.addedConstr = super.addPseudoBoolean(getObjectiveFunction()
                .getVars(), getObjectiveFunction().getCoeffs(), false,
                BigInteger.valueOf(forcedValue.longValue()));
    }

    @Deprecated
    public void discard() {
        discardCurrentSolution();
    }

    public void discardCurrentSolution() {
        // nothing to do here
    }

    public boolean isOptimal() {
        return this.optimumFound;
    }

    @Override
    public String toString(String prefix) {
        return prefix + "Pseudo Boolean Optimization by lower bound\n"
                + super.toString(prefix);
    }

    public void setTimeoutForFindingBetterSolution(int seconds) {
        throw new UnsupportedOperationException("Does not make sense here");
    }

}