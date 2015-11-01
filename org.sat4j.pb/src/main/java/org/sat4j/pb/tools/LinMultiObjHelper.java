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
import java.util.Collection;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.OptToPBSATAdapter;
import org.sat4j.pb.multiobjective.IMultiObjOptimizationProblem;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.SolutionFoundListener;

public class LinMultiObjHelper<T, C> extends DependencyHelper<T, C> implements
        SolutionFoundListener {

    private IMultiObjOptimizationProblem solver;
    private boolean hasASolution;
    private boolean isOptimal = false;

    public LinMultiObjHelper(IMultiObjOptimizationProblem solver) {
        this(solver, false);
    }

    public LinMultiObjHelper(IMultiObjOptimizationProblem solver,
            boolean explanationEnabled) {
        this(solver, false, false);
    }

    public LinMultiObjHelper(IMultiObjOptimizationProblem solver,
            boolean explanationEnabled, boolean canonicalOptFunctionEnabled) {
        super(new OptToPBSATAdapter(solver), explanationEnabled,
                canonicalOptFunctionEnabled);
        this.solver = solver;
        ((OptToPBSATAdapter) getSolver()).setSolutionFoundListener(this);
    }

    public void addCriterion(Collection<T> things) {
        IVecInt literals = new VecInt(things.size());
        for (T thing : things) {
            literals.push(getIntValue(thing));
        }
        solver.addObjectiveFunction(new ObjectiveFunction(literals,
                new Vec<BigInteger>(literals.size(), BigInteger.ONE)));
    }

    public void addWeightedCriterion(Collection<WeightedObject<T>> things) {
        IVecInt literals = new VecInt(things.size());
        IVec<BigInteger> coefs = new Vec<BigInteger>(things.size());
        for (WeightedObject<T> wo : things) {
            literals.push(getIntValue(wo.thing));
            coefs.push(wo.getWeight());
        }
        solver.addObjectiveFunction(new ObjectiveFunction(literals, coefs));
    }

    /**
     * 
     * @return true if the set of constraints entered inside the solver can be
     *         satisfied.
     * @throws TimeoutException
     */
    @Override
    public boolean hasASolution() throws TimeoutException {
        try {
            return super.hasASolution();
        } catch (TimeoutException e) {
            if (this.hasASolution) {
                return true;
            } else {
                throw e;
            }
        }
    }

    /**
     * 
     * @return true if the set of constraints entered inside the solver can be
     *         satisfied.
     * @throws TimeoutException
     */
    @Override
    public boolean hasASolution(IVec<T> assumps) throws TimeoutException {
        try {
            return super.hasASolution(assumps);
        } catch (TimeoutException e) {
            if (this.hasASolution) {
                return true;
            } else {
                throw e;
            }
        }
    }

    /**
     * 
     * @return true if the set of constraints entered inside the solver can be
     *         satisfied.
     * @throws TimeoutException
     */
    @Override
    public boolean hasASolution(Collection<T> assumps) throws TimeoutException {
        try {
            return super.hasASolution(assumps);
        } catch (TimeoutException e) {
            if (this.hasASolution) {
                return true;
            } else {
                throw e;
            }
        }
    }

    public boolean isOptimal() {
        return this.isOptimal || solver.isOptimal();
    }

    public void onSolutionFound(int[] solution) {
        this.hasASolution = true;
    }

    public void onSolutionFound(IVecInt solution) {
        this.hasASolution = true;

    }

    public void onUnsatTermination() {
        if (this.hasASolution)
            this.isOptimal = true;
    }

}
