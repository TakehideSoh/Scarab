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

import org.sat4j.core.ConstrGroup;
import org.sat4j.core.LiteralsUtils;
import org.sat4j.core.Vec;
import org.sat4j.minisat.core.ConflictTimer;
import org.sat4j.minisat.core.ConflictTimerAdapter;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.LearnedConstraintsDeletionStrategy;
import org.sat4j.minisat.core.LearningStrategy;
import org.sat4j.minisat.core.PrimeImplicantStrategy;
import org.sat4j.minisat.core.RestartStrategy;
import org.sat4j.minisat.core.SearchParams;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.core.WatcherBasedPrimeImplicantStrategy;
import org.sat4j.pb.IPBSolverService;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.ObjectiveFunctionComparator;
import org.sat4j.pb.orders.IOrderObjective;
import org.sat4j.specs.Constr;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;

public abstract class PBSolver extends Solver<PBDataStructureFactory> implements
        IPBCDCLSolver<PBDataStructureFactory>, IPBSolverService {

    private ObjectiveFunction objf;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected PBSolverStats stats;

    public PBSolver(LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, IOrder order, RestartStrategy restarter) {
        super(learner, dsf, order, restarter);
        this.stats = new PBSolverStats();
        initStats(this.stats);
    }

    public PBSolver(LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, SearchParams params, IOrder order,
            RestartStrategy restarter) {
        super(learner, dsf, params, order, restarter);
        this.stats = new PBSolverStats();
        initStats(this.stats);
    }

    public IConstr addPseudoBoolean(IVecInt literals, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger degree) throws ContradictionException {
        IVecInt vlits = dimacs2internal(literals);
        assert vlits.size() == literals.size();
        assert literals.size() == coeffs.size();
        return addConstr(this.dsfactory.createPseudoBooleanConstraint(vlits,
                coeffs, moreThan, degree));
    }

    public void setObjectiveFunction(ObjectiveFunction obj) {
        this.objf = obj;
        IOrder order = getOrder();
        if (order instanceof IOrderObjective) {
            ((IOrderObjective) order).setObjectiveFunction(obj);
        }
        // register literals only visible in the objective function (see
        // SAT-130)
        if (obj != null) {
            dimacs2internal(obj.getVars());
        }
    }

    public ObjectiveFunction getObjectiveFunction() {
        return this.objf;
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        // TODO use direct encoding to int/long
        IVec<BigInteger> bcoeffs = new Vec<BigInteger>(coeffs.size());
        for (int i = 0; i < coeffs.size(); i++) {
            bcoeffs.push(BigInteger.valueOf(coeffs.get(i)));
        }
        return addAtMost(literals, bcoeffs, BigInteger.valueOf(degree));
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        IVecInt vlits = dimacs2internal(literals);
        assert vlits.size() == literals.size();
        assert literals.size() == coeffs.size();
        return addConstr(this.dsfactory.createPseudoBooleanConstraint(vlits,
                coeffs, false, degree));
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        // TODO use direct encoding to int/long
        IVec<BigInteger> bcoeffs = new Vec<BigInteger>(coeffs.size());
        for (int i = 0; i < coeffs.size(); i++) {
            bcoeffs.push(BigInteger.valueOf(coeffs.get(i)));
        }
        return addAtLeast(literals, bcoeffs, BigInteger.valueOf(degree));
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        IVecInt vlits = dimacs2internal(literals);
        assert vlits.size() == literals.size();
        assert literals.size() == coeffs.size();
        return addConstr(this.dsfactory.createPseudoBooleanConstraint(vlits,
                coeffs, true, degree));
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
            throws ContradictionException {
        // TODO use direct encoding to int/long
        IVec<BigInteger> bcoeffs = new Vec<BigInteger>(coeffs.size());
        for (int i = 0; i < coeffs.size(); i++) {
            bcoeffs.push(BigInteger.valueOf(coeffs.get(i)));
        }
        return addExactly(literals, bcoeffs, BigInteger.valueOf(weight));
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight) throws ContradictionException {
        IVecInt vlits = dimacs2internal(literals);
        assert vlits.size() == literals.size();
        assert literals.size() == coeffs.size();
        ConstrGroup group = new ConstrGroup(false);
        group.add(addConstr(this.dsfactory.createPseudoBooleanConstraint(vlits,
                coeffs, false, weight)));
        group.add(addConstr(this.dsfactory.createPseudoBooleanConstraint(vlits,
                coeffs, true, weight)));
        return group;
    }

    public IConstr addAtMostOnTheFly(IVecInt literals, IVec<BigInteger> coefs,
            BigInteger degree) {
        IVecInt vlits = dimacs2internal(literals);
        this.sharedConflict = this.dsfactory
                .createUnregisteredAtMostConstraint(vlits, coefs, degree);
        this.sharedConflict.setLearnt();
        this.sharedConflict.register();
        addConstr(this.sharedConflict);
        // // backtrack to the first decision level with a reason
        // // for falsifying that constraint
        // IVecInt outReason = new VecInt();
        // this.sharedConflict.calcReasonOnTheFly(ILits.UNDEFINED, trail,
        // outReason);
        // Set<Integer> subset = new HashSet<Integer>();
        // for (IteratorInt it = outReason.iterator(); it.hasNext();) {
        // subset.add(it.next());
        // }
        // while (!trail.isEmpty() && !subset.contains(trail.last())) {
        // undoOne();
        // if (!trailLim.isEmpty() && trailLim.last() == trail.size()) {
        // trailLim.pop();
        // }
        // }
        return this.sharedConflict;
    }

    public IConstr addAtMostOnTheFly(IVecInt literals, IVecInt coefs, int degree) {
        IVec<BigInteger> coeffsCpy = new Vec<BigInteger>(coefs.size());
        for (IteratorInt iterator = coefs.iterator(); iterator.hasNext();) {
            coeffsCpy.push(BigInteger.valueOf(iterator.next()));
        }
        return addAtMostOnTheFly(literals, coeffsCpy,
                BigInteger.valueOf(degree));
    }

    @Override
    public int[] primeImplicant() {
        String primeApproach = System.getProperty("prime");
        PrimeImplicantStrategy strategy;
        if ("OBJECTIVE".equals(primeApproach)) {
            strategy = new WatcherBasedPrimeImplicantStrategy(
                    new ObjectiveFunctionComparator(this.objf));
        } else {
            strategy = new WatcherBasedPrimeImplicantStrategy();
        }
        int[] implicant = strategy.compute(this);
        prime = strategy.getPrimeImplicantAsArrayWithHoles();
        return implicant;
    }

    /**
     * @since 2.1
     */
    public final LearnedConstraintsDeletionStrategy objectiveFunctionBased = new LearnedConstraintsDeletionStrategy() {

        private static final long serialVersionUID = 1L;
        private boolean[] inObjectiveFunction;

        private final ConflictTimer clauseManagement = new ConflictTimerAdapter(
                PBSolver.this, 1000) {
            private static final long serialVersionUID = 1L;
            private int nbconflict = 0;
            private static final int MAX_CLAUSE = 5000;
            private static final int INC_CLAUSE = 1000;
            private int nextbound = MAX_CLAUSE;

            @Override
            public void run() {
                this.nbconflict += bound();
                if (this.nbconflict >= this.nextbound) {
                    this.nextbound += INC_CLAUSE;
                    this.nbconflict = 0;
                    setNeedToReduceDB(true);
                }
            }

            @Override
            public void reset() {
                super.reset();
                this.nextbound = MAX_CLAUSE;
                if (this.nbconflict >= this.nextbound) {
                    this.nbconflict = 0;
                    setNeedToReduceDB(true);
                }
            }
        };

        public void reduce(IVec<Constr> learnedConstrs) {
            int i, j;
            for (i = j = 0; i < learnedConstrs.size(); i++) {
                Constr c = learnedConstrs.get(i);
                if (c.locked() || c.getActivity() <= 2.0) {
                    learnedConstrs.set(j++, PBSolver.this.learnts.get(i));
                } else {
                    c.remove(PBSolver.this);
                }
            }
            if (isVerbose()) {
                System.out
                        .println(getLogPrefix()
                                + "cleaning " + (learnedConstrs.size() - j) //$NON-NLS-1$
                                + " clauses out of " + learnedConstrs.size() + "/" + PBSolver.this.stats.conflicts); //$NON-NLS-1$ //$NON-NLS-2$
                System.out.flush();
            }
            PBSolver.this.learnts.shrinkTo(j);

        }

        public ConflictTimer getTimer() {
            return this.clauseManagement;
        }

        @Override
        public String toString() {
            return "Objective function driven learned constraints deletion strategy";
        }

        public void init() {
            this.inObjectiveFunction = new boolean[nVars() + 1];
            if (PBSolver.this.objf == null) {
                throw new IllegalStateException(
                        "The strategy does not make sense if there is no objective function");
            }
            for (IteratorInt it = PBSolver.this.objf.getVars().iterator(); it
                    .hasNext();) {
                this.inObjectiveFunction[Math.abs(it.next())] = true;
            }
            this.clauseManagement.reset();
        }

        public void onClauseLearning(Constr constr) {
            boolean fullObj = true;

            for (int i = 0; i < constr.size(); i++) {
                fullObj = fullObj
                        && this.inObjectiveFunction[LiteralsUtils.var(constr
                                .get(i))];
            }
            if (fullObj) {
                constr.incActivity(1.0);
            } else {
                constr.incActivity(constr.size());
            }
        }

        public void onConflictAnalysis(Constr reason) {
            // do nothing
        }

        public void onPropagation(Constr from) {
            // do nothing
        }
    };
}
