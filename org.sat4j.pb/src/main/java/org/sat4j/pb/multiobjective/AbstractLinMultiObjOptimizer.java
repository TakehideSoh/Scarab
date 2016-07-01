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
package org.sat4j.pb.multiobjective;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.pb.IIntegerPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

/**
 * A partial implementation of the IMUltiObjectivePBSolver interface designed to
 * handle optimization functions which are a linear aggregation of other
 * objective functions.
 * 
 * The two abstract methods, setInitConstraints() and setGlobalObj(), are called
 * before the solver is called. setInitConstraints() is designed to add
 * linearization constraints, while setGlobalObj() is intended to set the global
 * linear function to the decorated PB solver.
 * 
 * @author lonca
 */
public abstract class AbstractLinMultiObjOptimizer extends PseudoOptDecorator
        implements IMultiObjOptimizationProblem {

    private static final long serialVersionUID = 1L;

    protected final List<ObjectiveFunction> objs = new ArrayList<ObjectiveFunction>();

    protected boolean initConstraintsSet = false;

    protected final IIntegerPBSolver integerSolver;

    public AbstractLinMultiObjOptimizer(IIntegerPBSolver solver) {
        super(solver);
        this.integerSolver = solver;
    }

    public void addObjectiveFunction(ObjectiveFunction obj) {
        this.objs.add(obj);
    }

    @Override
    public void setObjectiveFunction(ObjectiveFunction objf) {
        addObjectiveFunction(objf);
    }

    /**
     * Return the objective functions values through the last model found. The
     * values are ordered from the lowest to the highest.
     */
    public BigInteger[] getObjectiveValues() {
        BigInteger[] objValues = new BigInteger[this.objs.size()];
        for (int i = 0; i < this.objs.size(); ++i) {
            objValues[i] = this.objs.get(i).calculateDegree(this);
        }
        Arrays.sort(objValues);
        return objValues;
    }

    @Override
    public boolean admitABetterSolution() throws TimeoutException {
        return admitABetterSolution(VecInt.EMPTY);
    }

    @Override
    public boolean admitABetterSolution(IVecInt assumps)
            throws TimeoutException {
        if (!this.initConstraintsSet) {
            setInitConstraints();
            setGlobalObj();
            this.initConstraintsSet = true;
        }
        boolean res = super.admitABetterSolution(assumps);
        if (res && isVerbose()) {
            System.out.println(getLogPrefix()
                    + "Current objective functions values: "
                    + Arrays.toString(getObjectiveValues()));
        }
        return res;
    }

    @Override
    public boolean hasNoObjectiveFunction() {
        return false;
    }

    /**
     * Implement this method to add some constraints before the first call to
     * the solver.
     */
    protected abstract void setInitConstraints();

    /**
     * Implement this method to set a global linear objective function, which
     * depends on the added objective functions and the initialization-added
     * constraints.
     */
    protected abstract void setGlobalObj();

}
