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
 * A decorator that computes minimal pseudo boolean models.
 * 
 * @author daniel
 * 
 */
public class PseudoOptDecorator extends PBSolverDecorator implements
        IOptimizationProblem {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected BigInteger objectiveValue;

    private int[] prevmodel;
    private int[] prevmodelwithadditionalvars;

    private boolean[] prevfullmodel;

    private IVecInt prevModelBlockingClause;

    private IConstr previousPBConstr;

    private boolean isSolutionOptimal;

    private final boolean nonOptimalMeansSatisfiable;

    private final boolean useAnImplicantForEvaluation;

    private int solverTimeout = Integer.MAX_VALUE;

    private int optimizationTimeout = -1;

    /**
     * Create a PB decorator for which a non optimal solution means that the
     * problem is satisfiable.
     * 
     * @param solver
     *            a PB solver.
     */
    public PseudoOptDecorator(IPBSolver solver) {
        this(solver, true);
    }

    /**
     * Create a PB decorator with a specific semantic of non optimal solution.
     * 
     * @param solver
     *            a PB solver
     * @param nonOptimalMeansSatisfiable
     *            true if a suboptimal solution means that the problem is
     *            satisfiable (e.g. as in the PB competition), else false (e.g.
     *            as in the MAXSAT competition).
     */
    public PseudoOptDecorator(IPBSolver solver,
            boolean nonOptimalMeansSatisfiable) {
        this(solver, nonOptimalMeansSatisfiable, false);
    }

    /**
     * Create a PB decorator with a specific semantic of non optimal solution.
     * 
     * @param solver
     *            a PB solver
     * @param nonOptimalMeansSatisfiable
     *            true if a suboptimal solution means that the problem is
     *            satisfiable (e.g. as in the PB competition), else false (e.g.
     *            as in the MAXSAT competition).
     * @param useAnImplicantForEvaluation
     *            uses an implicant (a prime implicant computed using
     *            {@link #primeImplicant()}) instead of a plain model to
     *            evaluate the objective function.
     */
    public PseudoOptDecorator(IPBSolver solver,
            boolean nonOptimalMeansSatisfiable,
            boolean useAnImplicantForEvaluation) {
        super(solver);
        this.nonOptimalMeansSatisfiable = nonOptimalMeansSatisfiable;
        this.useAnImplicantForEvaluation = useAnImplicantForEvaluation;
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
        boolean result = super.isSatisfiable(assumps, global);
        if (result) {
            this.prevmodel = super.model();
            this.prevModelBlockingClause = super
                    .createBlockingClauseForCurrentModel();
            this.prevmodelwithadditionalvars = super
                    .modelWithInternalVariables();
            this.prevfullmodel = new boolean[nVars()];
            for (int i = 0; i < nVars(); i++) {
                this.prevfullmodel[i] = decorated().model(i + 1);
            }
            if (optimizationTimeout > 0) {
                super.expireTimeout();
                super.setTimeout(optimizationTimeout);
            }

        } else {
            if (this.previousPBConstr != null) {
                decorated().removeConstr(this.previousPBConstr);
                this.previousPBConstr = null;
            }
            super.setTimeout(solverTimeout);
        }
        return result;
    }

    @Override
    public boolean isSatisfiable(IVecInt assumps) throws TimeoutException {
        return isSatisfiable(assumps, false);
    }

    @Override
    public void setObjectiveFunction(ObjectiveFunction objf) {
        decorated().setObjectiveFunction(objf);
    }

    public boolean admitABetterSolution() throws TimeoutException {
        return admitABetterSolution(VecInt.EMPTY);
    }

    public boolean admitABetterSolution(IVecInt assumps)
            throws TimeoutException {
        try {
            this.isSolutionOptimal = false;
            boolean result = super.isSatisfiable(assumps, true);
            if (result) {
                if (this.useAnImplicantForEvaluation) {
                    this.prevmodel = modelWithAdaptedNonPrimeLiterals();

                } else {
                    this.prevmodel = super.model();
                }
                this.prevModelBlockingClause = super
                        .createBlockingClauseForCurrentModel();
                this.prevmodelwithadditionalvars = super
                        .modelWithInternalVariables();
                this.prevfullmodel = new boolean[nVars()];
                for (int i = 0; i < nVars(); i++) {
                    this.prevfullmodel[i] = decorated().model(i + 1);
                }
                if (decorated().getObjectiveFunction() != null) {
                    calculateObjective();
                }
                if (optimizationTimeout > 0) {
                    super.expireTimeout();
                    super.setTimeout(optimizationTimeout);
                }
            } else {
                this.isSolutionOptimal = true;
                if (this.previousPBConstr != null) {
                    decorated().removeConstr(this.previousPBConstr);
                    this.previousPBConstr = null;
                }
            }
            return result;
        } catch (TimeoutException te) {
            if (this.previousPBConstr != null) {
                decorated().removeConstr(this.previousPBConstr);
                this.previousPBConstr = null;
            }
            throw te;
        }
    }

    private int[] modelWithAdaptedNonPrimeLiterals() {
        // do not use model() because it might contain holes.
        int[] completeModel = new int[nVars()];
        int var;
        for (int i = 0; i < nVars(); i++) {
            var = i + 1;
            completeModel[i] = super.model(var) ? var : -var;
        }
        primeImplicant();
        ObjectiveFunction obj = getObjectiveFunction();
        for (int i = 0; i < obj.getVars().size(); i++) {
            int d = obj.getVars().get(i);
            BigInteger coeff = obj.getCoeffs().get(i);
            if (d <= nVars() && !primeImplicant(d) && !primeImplicant(-d)) {
                // the variable does not appear in the model: it can be assigned
                // either way
                assert Math.abs(completeModel[Math.abs(d) - 1]) == d;
                if (coeff.signum() * d < 0) {
                    completeModel[Math.abs(d) - 1] = Math.abs(d);
                } else {
                    completeModel[Math.abs(d) - 1] = -Math.abs(d);
                }
            }
        }
        return completeModel;
    }

    public boolean hasNoObjectiveFunction() {
        return decorated().getObjectiveFunction() == null;
    }

    public boolean nonOptimalMeansSatisfiable() {
        return nonOptimalMeansSatisfiable;
    }

    public Number calculateObjective() {
        if (decorated().getObjectiveFunction() == null) {
            throw new UnsupportedOperationException(
                    "The problem does not contain an objective function");
        }
        if (this.useAnImplicantForEvaluation) {
            this.objectiveValue = decorated().getObjectiveFunction()
                    .calculateDegreeImplicant(decorated());
        } else {
            this.objectiveValue = decorated().getObjectiveFunction()
                    .calculateDegree(decorated());
        }
        return getObjectiveValue();
    }

    public void discardCurrentSolution() throws ContradictionException {
        if (this.previousPBConstr != null) {
            super.removeSubsumedConstr(this.previousPBConstr);
        }
        if (decorated().getObjectiveFunction() != null
                && this.objectiveValue != null) {
            this.previousPBConstr = super.addPseudoBoolean(decorated()
                    .getObjectiveFunction().getVars(), decorated()
                    .getObjectiveFunction().getCoeffs(), false,
                    this.objectiveValue.subtract(BigInteger.ONE));
        }
    }

    @Override
    public void reset() {
        this.previousPBConstr = null;
        super.reset();
    }

    @Override
    public int[] model() {
        // DLB findbugs ok
        return this.prevmodel;
    }

    @Override
    public boolean model(int var) {
        return this.prevfullmodel[var - 1];
    }

    @Override
    public String toString(String prefix) {
        return prefix
                + "Pseudo Boolean Optimization by upper bound\n"
                + (useAnImplicantForEvaluation ? prefix
                        + "using prime implicants for evaluating the objective function\n"
                        : "") + super.toString(prefix);
    }

    public Number getObjectiveValue() {
        return this.objectiveValue.add(decorated().getObjectiveFunction()
                .getCorrection());
    }

    public void discard() throws ContradictionException {
        discardCurrentSolution();
    }

    public void forceObjectiveValueTo(Number forcedValue)
            throws ContradictionException {
        super.addPseudoBoolean(decorated().getObjectiveFunction().getVars(),
                decorated().getObjectiveFunction().getCoeffs(), false,
                (BigInteger) forcedValue);
    }

    public boolean isOptimal() {
        return this.isSolutionOptimal;
    }

    @Override
    public int[] modelWithInternalVariables() {
        return this.prevmodelwithadditionalvars;
    }

    public void setTimeoutForFindingBetterSolution(int seconds) {
        optimizationTimeout = seconds;
    }

    @Override
    public void setTimeout(int t) {
        solverTimeout = t;
        super.setTimeout(t);
    }

    public void removeSubsumedOptConstr() {
        if (this.previousPBConstr == null)
            return;
        super.removeSubsumedConstr(previousPBConstr);
        this.previousPBConstr = null;
    }

    @Override
    public IConstr discardCurrentModel() throws ContradictionException {
        return addBlockingClause(this.prevModelBlockingClause);
    }
}
