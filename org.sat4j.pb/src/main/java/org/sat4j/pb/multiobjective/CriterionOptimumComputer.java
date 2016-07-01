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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.OptToPBSATAdapter;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.pb.tools.ManyCorePB;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.SolutionFoundListener;

/**
 * A class used to compute optimal values for each criteria for a given problem.
 * Computation is parallelized.
 * 
 * @author lonca
 * 
 * @param <S>
 *            Subsolvers type
 */
public class CriterionOptimumComputer<S extends IPBSolver> {

    private final ManyCorePB<S> solvers;

    private final List<ObjectiveFunction> objs = new ArrayList<ObjectiveFunction>();

    private BigInteger opts[];

    protected Semaphore lock;

    protected boolean timeoutOccured = false;

    public CriterionOptimumComputer(ManyCorePB<S> solvers) {
        this.solvers = solvers;
    }

    public void addObjectiveFunction(ObjectiveFunction obj) {
        this.objs.add(obj);
    }

    public void addObjectiveFunctions(Collection<ObjectiveFunction> objs) {
        this.objs.addAll(objs);
    }

    /**
     * Launch optimization processes. User must call waitForSolvers() in order
     * to wait all the computations to be finished.
     */
    public void compute() {
        opts = new BigInteger[this.objs.size()];
        this.lock = new Semaphore(this.objs.size());
        this.timeoutOccured = false;
        for (int i = 0; i < this.objs.size(); ++i) {
            OptToPBSATAdapter optimizer = new OptToPBSATAdapter(
                    new PseudoOptDecorator(this.solvers.getSolvers().get(i)));
            optimizer.setObjectiveFunction(objs.get(i));
            optimizer.setSolutionFoundListener(new OptimumValueWriter(this, i));
            new SolverLauncher(optimizer).start();
            try {
                this.lock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    public void expireAll() {
        for (int i = 0; i < objs.size(); ++i) {
            this.solvers.getSolvers().get(i).expireTimeout();
        }
        opts = null;
    }

    public boolean timeoutOccured() {
        return this.timeoutOccured;
    }

    /**
     * Allow user to get optimum values. In case timeout occurred, suboptimal
     * values should take place in the results : call timeoutOccured() before
     * this method.
     * 
     * @return
     */
    public BigInteger[] getOptimums() {
        return this.opts;
    }

    public void waitForSolvers() {
        try {
            this.lock.acquire(this.objs.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    class OptimumValueWriter implements SolutionFoundListener {

        private final CriterionOptimumComputer<S> coc;
        private final int solverIndex;

        OptimumValueWriter(CriterionOptimumComputer<S> coc, int solverIndex) {
            this.coc = coc;
            this.solverIndex = solverIndex;
        }

        public synchronized void onSolutionFound(int[] solution) {
            coc.opts[solverIndex] = coc.objs.get(solverIndex).calculateDegree(
                    coc.solvers.getSolvers().get(solverIndex));
        }

        public synchronized void onSolutionFound(IVecInt solution) {
            coc.opts[solverIndex] = coc.objs.get(solverIndex).calculateDegree(
                    coc.solvers.getSolvers().get(solverIndex));
        }

        public synchronized void onUnsatTermination() {
            coc.lock.release();
        }
    }

    class SolverLauncher extends Thread {

        private final IPBSolver solver;

        SolverLauncher(IPBSolver solver) {
            this.solver = solver;
        }

        @Override
        public void run() {
            try {
                this.solver.isSatisfiable();
            } catch (TimeoutException e) {
                timeoutOccured = true;
                lock.release();
            }
        }
    }
}
