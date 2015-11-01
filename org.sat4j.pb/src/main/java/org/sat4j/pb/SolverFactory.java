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

import org.sat4j.core.ASolverFactory;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.IPhaseSelectionStrategy;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.learning.ClauseOnlyLearning;
import org.sat4j.minisat.learning.MiniSATLearning;
import org.sat4j.minisat.learning.NoLearningButHeuristics;
import org.sat4j.minisat.orders.PhaseInLastLearnedClauseSelectionStrategy;
import org.sat4j.minisat.orders.RSATPhaseSelectionStrategy;
import org.sat4j.minisat.orders.UserFixedPhaseSelectionStrategy;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.minisat.restarts.ArminRestarts;
import org.sat4j.minisat.restarts.Glucose21Restarts;
import org.sat4j.minisat.restarts.LubyRestarts;
import org.sat4j.minisat.restarts.MiniSATRestarts;
import org.sat4j.minisat.restarts.NoRestarts;
import org.sat4j.pb.constraints.AbstractPBDataStructureFactory;
import org.sat4j.pb.constraints.CompetMinHTmixedClauseCardConstrDataStructureFactory;
import org.sat4j.pb.constraints.CompetResolutionMinPBLongMixedWLClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.CompetResolutionPBLongMixedHTClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.CompetResolutionPBLongMixedWLClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.CompetResolutionPBMixedHTClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.CompetResolutionPBMixedWLClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.PBLongMaxClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.PBLongMinClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.PBMaxClauseAtLeastConstrDataStructure;
import org.sat4j.pb.constraints.PBMaxClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.PBMaxDataStructure;
import org.sat4j.pb.constraints.PBMinClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.PBMinDataStructure;
import org.sat4j.pb.constraints.PuebloPBMinClauseAtLeastConstrDataStructure;
import org.sat4j.pb.constraints.PuebloPBMinClauseCardConstrDataStructure;
import org.sat4j.pb.constraints.PuebloPBMinDataStructure;
import org.sat4j.pb.core.PBDataStructureFactory;
import org.sat4j.pb.core.PBSolver;
import org.sat4j.pb.core.PBSolverCP;
import org.sat4j.pb.core.PBSolverCPLong;
import org.sat4j.pb.core.PBSolverCPLongReduceToCard;
import org.sat4j.pb.core.PBSolverCautious;
import org.sat4j.pb.core.PBSolverClause;
import org.sat4j.pb.core.PBSolverResCP;
import org.sat4j.pb.core.PBSolverResolution;
import org.sat4j.pb.core.PBSolverWithImpliedClause;
import org.sat4j.pb.orders.VarOrderHeapObjective;
import org.sat4j.pb.tools.InprocCardConstrLearningSolver;
import org.sat4j.pb.tools.ManyCorePB;
import org.sat4j.pb.tools.PreprocCardConstrLearningSolver;

/**
 * User friendly access to pre-constructed solvers.
 * 
 * @author leberre
 * @since 2.0
 */
public final class SolverFactory extends ASolverFactory<IPBSolver> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // thread safe implementation of the singleton design pattern
    private static SolverFactory instance;

    /**
     * Private contructor. Use singleton method instance() instead.
     * 
     * @see #instance()
     */
    private SolverFactory() {

    }

    private static synchronized void createInstance() {
        if (instance == null) {
            instance = new SolverFactory();
        }
    }

    /**
     * Access to the single instance of the factory.
     * 
     * @return the singleton of that class.
     */
    public static SolverFactory instance() {
        if (instance == null) {
            createInstance();
        }
        return instance;
    }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints and clause
     *         learning.
     */
    public static PBSolverResolution newPBResAllPB() {
        return newPBRes(new PBMaxDataStructure());
    }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints and
     *         constraint learning.
     */
    public static PBSolverCP newPBCPAllPB() {
        return newPBCP(new PBMaxDataStructure());
    }

    /**
     * @return Solver used to display in a string the pb-instance in OPB format.
     */
    public static IPBSolver newOPBStringSolver() {
        return new OPBStringSolver();
    }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt).
     */
    public static PBSolverCP newPBCPMixedConstraints() {
        return newPBCP(new PBMaxClauseCardConstrDataStructure());
    }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). A specific heuristics
     *         taking into account the objective value is used.
     */
    public static PBSolverCP newPBCPMixedConstraintsObjective() {
        return newPBCP(new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective());
    }

    public static PBSolverCP newCompetPBCPMixedConstraintsObjective() {
        return newPBCP(new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective());
    }

    public static PBSolverCP newCompetPBCPMixedConstraintsMinObjective() {
        return newPBCP(new PBMinClauseCardConstrDataStructure(),
                new VarOrderHeapObjective());
    }

    public static PBSolverCP newCompetPBCPMixedConstraintsLongMaxObjective() {
        PBSolverCP s = newPBCP(new PBLongMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective());
        return s;
    }

    public static PBSolverCP newCompetPBCPRemoveSatisfiedMixedConstraintsLongMaxObjective() {
        PBSolverCP s = newPBCP(new PBLongMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective(), false);
        return s;
    }

    public static PBSolverCP newCompetPBCPMixedConstraintsLongMinObjective() {
        return newPBCP(new PBLongMinClauseCardConstrDataStructure(),
                new VarOrderHeapObjective());
    }

    /**
     * @return MiniLearning with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). A specific heuristics
     *         taking into account the objective value is used. Conflict
     *         analysis with full cutting plane inference. Only clauses are
     *         recorded.
     */
    public static PBSolverCP newPBCPMixedConstraintsObjectiveLearnJustClauses() {
        ClauseOnlyLearning<PBDataStructureFactory> learning = new ClauseOnlyLearning<PBDataStructureFactory>();
        PBSolverCP solver = new PBSolverCP(learning,
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective());
        return solver;
    }

    public static PBSolverCP newCompetPBCPMixedConstraintsObjectiveLearnJustClauses() {
        ClauseOnlyLearning<PBDataStructureFactory> learning = new ClauseOnlyLearning<PBDataStructureFactory>();
        PBSolverCP solver = new PBSolverCP(learning,
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective());
        return solver;
    }

    private static PBSolverCP newPBKiller(IPhaseSelectionStrategy phase) {
        ClauseOnlyLearning<PBDataStructureFactory> learning = new ClauseOnlyLearning<PBDataStructureFactory>();
        PBSolverCP solver = new PBSolverCP(learning,
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective(phase));
        return solver;
    }

    public static PBSolverCP newPBKillerRSAT() {
        return newPBKiller(new RSATPhaseSelectionStrategy());
    }

    public static PBSolverCP newPBKillerClassic() {
        return newPBKiller(new PhaseInLastLearnedClauseSelectionStrategy());
    }

    public static PBSolverCP newPBKillerFixed() {
        return newPBKiller(new UserFixedPhaseSelectionStrategy());
    }

    private static PBSolverCP newCompetPBKiller(IPhaseSelectionStrategy phase) {
        ClauseOnlyLearning<PBDataStructureFactory> learning = new ClauseOnlyLearning<PBDataStructureFactory>();
        PBSolverCP solver = new PBSolverCP(learning,
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective(phase));
        return solver;
    }

    public static PBSolverCP newCompetPBKillerRSAT() {
        return newCompetPBKiller(new RSATPhaseSelectionStrategy());
    }

    public static PBSolverCP newCompetPBKillerClassic() {
        return newCompetPBKiller(new PhaseInLastLearnedClauseSelectionStrategy());
    }

    public static PBSolverCP newCompetPBKillerFixed() {
        return newCompetPBKiller(new UserFixedPhaseSelectionStrategy());
    }

    /**
     * @return MiniLearning with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). A specific heuristics
     *         taking into account the objective value is used. Conflict
     *         analysis reduces to cardinalities to avoid computations
     */
    // public static PBSolverCP
    // newCardLearningOPBLongClauseCardConstrMaxSpecificOrderIncrementalReductionToCardinality()
    // {
    // // LimitedLearning learning = new LimitedLearning(10);
    // MiniSATLearning<PBDataStructureFactory> learning = new
    // MiniSATLearning<PBDataStructureFactory>();
    // // LearningStrategy learning = new NoLearningButHeuristics();
    // PBSolverCP solver = new PBSolverCard(learning,
    // new PBLongMaxClauseCardConstrDataStructure(),
    // new VarOrderHeapObjective());
    // learning.setDataStructureFactory(solver.getDSFactory());
    // learning.setVarActivityListener(solver);
    // return solver;
    // }

    /**
     * @return MiniLearning with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). A specific heuristics
     *         taking into account the objective value is used. Conflict
     *         analysis reduces to clauses to avoid computations
     */
    public static PBSolverCP newMiniLearningOPBClauseCardConstrMaxSpecificOrderIncrementalReductionToClause() {
        // LimitedLearning learning = new LimitedLearning(10);
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        // LearningStrategy learning = new NoLearningButHeuristics();
        PBSolverCP solver = new PBSolverClause(learning,
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        return solver;
    }

    /**
     * @return MiniLearning with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). A specific heuristics
     *         taking into account the objective value is used. The PB
     *         constraints are not learnt (watched), just used for backjumping.
     */
    public static PBSolverCP newPBCPMixedConstraintsObjectiveNoLearning() {
        NoLearningButHeuristics<PBDataStructureFactory> learning = new NoLearningButHeuristics<PBDataStructureFactory>();
        // SearchParams params = new SearchParams(1.1,100);
        PBSolverCP solver = new PBSolverCP(learning,
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective());
        learning.setVarActivityListener(solver);
        return solver;
    }

    public static PBSolverResolution newPBResMixedConstraintsObjective() {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverResolution solver = new PBSolverResolution(learning,
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective(), new MiniSATRestarts());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        return solver;
    }

    public static PBSolverResolution newCompetPBResWLMixedConstraintsObjectiveExpSimp() {
        return newCompetPBResMixedConstraintsObjectiveExpSimp(new CompetResolutionPBMixedWLClauseCardConstrDataStructure());
    }

    public static PBSolverResolution newCompetPBResHTMixedConstraintsObjectiveExpSimp() {
        return newCompetPBResMixedConstraintsObjectiveExpSimp(new CompetResolutionPBMixedHTClauseCardConstrDataStructure());
    }

    public static PBSolverResolution newCompetPBResLongHTMixedConstraintsObjectiveExpSimp() {
        return newCompetPBResMixedConstraintsObjectiveExpSimp(new CompetResolutionPBLongMixedHTClauseCardConstrDataStructure());
    }

    public static PBSolverResolution newCompetPBResLongWLMixedConstraintsObjectiveExpSimp() {
        return newCompetPBResMixedConstraintsObjectiveExpSimp(new CompetResolutionPBLongMixedWLClauseCardConstrDataStructure());
    }

    public static PBSolverResolution newCompetMinPBResLongWLMixedConstraintsObjectiveExpSimp() {
        return newCompetPBResMixedConstraintsObjectiveExpSimp(new CompetResolutionMinPBLongMixedWLClauseCardConstrDataStructure());
    }

    public static PBSolverResolution newCompetPBResMixedConstraintsObjectiveExpSimp(
            PBDataStructureFactory dsf) {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverResolution solver = new PBSolverResolution(learning, dsf,
                new VarOrderHeapObjective(new RSATPhaseSelectionStrategy()),
                new ArminRestarts());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        solver.setSimplifier(solver.EXPENSIVE_SIMPLIFICATION);
        return solver;
    }

    public static PBSolverResolution newPBResHTMixedConstraintsObjective() {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        AbstractPBDataStructureFactory ds = new CompetResolutionPBMixedHTClauseCardConstrDataStructure();
        ds.setNormalizer(AbstractPBDataStructureFactory.NO_COMPETITION);
        PBSolverResolution solver = new PBSolverResolution(learning, ds,
                new VarOrderHeapObjective(), new MiniSATRestarts());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        return solver;
    }

    public static PBSolverResolution newCompetPBResMinHTMixedConstraintsObjective() {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverResolution solver = new PBSolverResolution(learning,
                new CompetMinHTmixedClauseCardConstrDataStructureFactory(),
                new VarOrderHeapObjective(), new MiniSATRestarts());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        return solver;
    }

    public static PBSolverResolution newPBResMinHTMixedConstraintsObjective() {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        AbstractPBDataStructureFactory ds = new CompetMinHTmixedClauseCardConstrDataStructureFactory();
        ds.setNormalizer(AbstractPBDataStructureFactory.NO_COMPETITION);
        PBSolverResolution solver = new PBSolverResolution(learning, ds,
                new VarOrderHeapObjective(), new MiniSATRestarts());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        return solver;
    }

    public static PBSolverResolution newCompetPBResMixedConstraintsObjectiveExpSimp() {
        PBSolverResolution solver = newPBResMixedConstraintsObjective();
        solver.setSimplifier(solver.EXPENSIVE_SIMPLIFICATION);
        return solver;
    }

    public static PBSolverResolution newPBResHTMixedConstraintsObjectiveExpSimp() {
        PBSolverResolution solver = newPBResHTMixedConstraintsObjective();
        solver.setSimplifier(solver.EXPENSIVE_SIMPLIFICATION);
        return solver;
    }

    public static PBSolverResolution newCompetPBResMinHTMixedConstraintsObjectiveExpSimp() {
        PBSolverResolution solver = newCompetPBResMinHTMixedConstraintsObjective();
        solver.setSimplifier(solver.EXPENSIVE_SIMPLIFICATION);
        return solver;
    }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). A reduction of
     *         PB-constraints to clauses is made in order to simplify cutting
     *         planes.
     */
    public static PBSolverClause newPBCPMixedConstraintsReduceToClause() {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverClause solver = new PBSolverClause(learning,
                new PBMaxClauseCardConstrDataStructure(), new VarOrderHeap());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        return solver;
    }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). A reduction of
     *         PB-constraints to cardinalities is made in order to simplify
     *         cutting planes.
     */
    // public static PBSolverCard
    // newPBCPMixedLongConstraintsReduceToCardinality() {
    // MiniSATLearning<PBDataStructureFactory> learning = new
    // MiniSATLearning<PBDataStructureFactory>();
    // PBSolverCard solver = new PBSolverCard(learning,
    // new PBLongMaxClauseCardConstrDataStructure(),
    // new VarOrderHeap());
    // learning.setDataStructureFactory(solver.getDSFactory());
    // learning.setVarActivityListener(solver);
    // return solver;
    // }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). A reduction of
     *         PB-constraints to clauses is made in order to simplify cutting
     *         planes (if coefficients are larger than bound).
     */
    public static PBSolverCautious newPBCPMixedConstraintsCautious(int bound) {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverCautious solver = new PBSolverCautious(learning,
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective(), bound);
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        return solver;
    }

    public static PBSolverCautious newPBCPMixedConstraintsCautious() {
        return newPBCPMixedConstraintsCautious(PBSolverCautious.BOUND);
    }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). A reduction of
     *         PB-constraints to clauses is made in order to simplify cutting
     *         planes (if coefficients are larger than bound).
     */
    public static PBSolverResCP newPBCPMixedConstraintsResCP(long bound) {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverResCP solver = new PBSolverResCP(learning,
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective(), bound);
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        solver.setSimplifier(solver.EXPENSIVE_SIMPLIFICATION);
        return solver;
    }

    public static PBSolverResCP newPBCPMixedConstraintsResCP() {
        return newPBCPMixedConstraintsResCP(PBSolverResCP.MAXCONFLICTS);
    }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints and
     *         constraint learning. Clauses and cardinalities with watched
     *         literals are also handled (and learnt). a pre-processing is
     *         applied which adds implied clauses from PB-constraints.
     */
    public static PBSolverWithImpliedClause newPBCPMixedConstrainsImplied() {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverWithImpliedClause solver = new PBSolverWithImpliedClause(
                learning, new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeap());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        return solver;
    }

    /**
     * @return MiniSAT with Counter-based pseudo boolean constraints,
     *         counter-based cardinalities, watched clauses and constraint
     *         learning. methods isAssertive() and getBacktrackLevel() are
     *         totally incremental. Conflicts for PB-constraints use a Map
     *         structure
     */
    public static PBSolverCP newMiniOPBClauseAtLeastConstrMax() {
        return newPBCP(new PBMaxClauseAtLeastConstrDataStructure());
    }

    /**
     * @return MiniSAT with WL-based pseudo boolean constraints and clause
     *         learning.
     */
    public static PBSolverResolution newPBResAllPBWL() {
        return newPBRes(new PBMinDataStructure());
    }

    /**
     * @return MiniSAT with WL-based pseudo boolean constraints and constraint
     *         learning.
     */
    public static PBSolverCP newPBCPAllPBWL() {
        return newPBCP(new PBMinDataStructure());
    }

    /**
     * @return MiniSAT with WL-based pseudo boolean constraints and clause
     *         learning.
     */
    public static PBSolverResolution newPBResAllPBWLPueblo() {
        return newPBRes(new PuebloPBMinDataStructure());
    }

    private static PBSolverResolution newPBRes(PBDataStructureFactory dsf) {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverResolution solver = new PBSolverResolution(learning, dsf,
                new VarOrderHeap(), new MiniSATRestarts());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        return solver;
    }

    /**
     * @return MiniSAT with WL-based pseudo boolean constraints and constraint
     *         learning.
     */
    public static PBSolverCP newPBCPAllPBWLPueblo() {
        return newPBCP(new PuebloPBMinDataStructure());
    }

    /**
     * @return MiniSAT with WL-based pseudo boolean constraints and clauses,
     *         cardinalities, and constraint learning.
     */
    public static PBSolverCP newMiniOPBClauseCardMinPueblo() {
        return newPBCP(new PuebloPBMinClauseCardConstrDataStructure());
    }

    /**
     * @return MiniSAT with WL-based pseudo boolean constraints and clauses,
     *         cardinalities, and constraint learning.
     */
    public static PBSolverCP newMiniOPBClauseCardMin() {
        return newPBCP(new PBMinClauseCardConstrDataStructure());
    }

    /**
     * @return MiniSAT with WL-based pseudo boolean constraints and clauses,
     *         counter-based cardinalities, and constraint learning.
     */
    public static PBSolverCP newMiniOPBClauseAtLeastMinPueblo() {
        return newPBCP(new PuebloPBMinClauseAtLeastConstrDataStructure());
    }

    private static PBSolverCP newPBCP(PBDataStructureFactory dsf, IOrder order,
            boolean noRemove) {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverCP solver = new PBSolverCP(learning, dsf, order, noRemove);
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        solver.setRestartStrategy(new ArminRestarts());
        solver.setLearnedConstraintsDeletionStrategy(solver.lbd_based);
        return solver;
    }

    private static PBSolverCP newPBCPStar(PBDataStructureFactory dsf,
            IOrder order, boolean noRemove) {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverCP solver = new PBSolverCPLong(learning, dsf, order, noRemove);
        // PBSolverCP solver = new PBSolverCautious(learning, dsf, order,
        // PBSolverCautious.BOUND);
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        solver.setRestartStrategy(new ArminRestarts());
        solver.setLearnedConstraintsDeletionStrategy(solver.lbd_based);
        return solver;
    }

    private static PBSolverCP newPBCPStarReduceToCard(
            PBDataStructureFactory dsf, IOrder order, boolean noRemove) {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverCP solver = new PBSolverCPLongReduceToCard(learning, dsf,
                order, noRemove);
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        solver.setRestartStrategy(new ArminRestarts());
        solver.setLearnedConstraintsDeletionStrategy(solver.lbd_based);
        return solver;
    }

    public static PBSolverCP newPBCP(PBDataStructureFactory dsf, IOrder order) {
        return newPBCP(dsf, order, true);
    }

    private static PBSolverCP newPBCP(PBDataStructureFactory dsf) {
        return newPBCP(dsf, new VarOrderHeap());
    }

    private static PBSolverCP newPBCP(PBDataStructureFactory dsf,
            boolean noRemove) {
        return newPBCP(dsf, new VarOrderHeap(), noRemove);
    }

    /**
     * Cutting Planes based solver. The inference during conflict analysis is
     * based on cutting planes instead of resolution as in a SAT solver.
     * 
     * @return the best available cutting planes based solver of the library.
     */
    public static IPBSolver newCuttingPlanes() {
        return newCompetPBCPMixedConstraintsObjective();
    }

    public static IPBSolver newCuttingPlanesStar() {
        return newPBCPStar(new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective(), true);
    }

    public static IPBSolver newCuttingPlanesStarReduceToCard() {
        return newPBCPStarReduceToCard(
                new PBMaxClauseCardConstrDataStructure(),
                new VarOrderHeapObjective(), true);
    }

    /**
     * Cutting Planes based solver. The inference during conflict analysis is
     * based on cutting planes instead of resolution as in a SAT solver.
     * 
     * @return the best available cutting planes based solver of the library.
     */
    public static IPBSolver newCuttingPlanesAggressiveCleanup() {
        PBSolverCP solver = newCompetPBCPMixedConstraintsObjective();
        solver.setLearnedConstraintsDeletionStrategy(solver.fixedSize(100));
        return solver;
    }

    /**
     * Resolution based solver (i.e. classic SAT solver able to handle generic
     * constraints. No specific inference mechanism.
     * 
     * @return the best available resolution based solver of the library.
     */
    public static IPBSolver newResolution() {
        return newResolutionGlucoseExpSimp();
    }

    /**
     * Resolution and CuttingPlanes based solvers running in parallel. Does only
     * make sense if run on a computer with several cores.
     * 
     * @return a parallel solver using both resolution and cutting planes proof
     *         system.
     */
    public static IPBSolver newBoth() {
        return new ManyCorePB(newResolution(), newCuttingPlanes());
    }

    /**
     * Two solvers are running in //: one for solving SAT instances, the other
     * one for solving unsat instances.
     * 
     * @return a parallel solver for both SAT and UNSAT problems.
     */
    public static IPBSolver newSATUNSAT() {
        return new ManyCorePB(newSAT(), newUNSAT());
    }

    /**
     * That solver is expected to perform better on satisfiable benchmarks.
     * 
     * @return a solver for satisfiable benchmarks.
     */
    public static PBSolverResolution newSAT() {
        PBSolverResolution solver = newResolutionGlucose();
        solver.setRestartStrategy(new LubyRestarts(100));
        solver.setLearnedConstraintsDeletionStrategy(solver.activity_based_low_memory);
        return solver;
    }

    /**
     * That solver is expected to perform better on unsatisfiable benchmarks.
     * 
     * @return a solver for unsatisfiable benchmarks.
     */
    public static PBSolverResolution newUNSAT() {
        PBSolverResolution solver = newResolutionGlucose();
        solver.setRestartStrategy(new NoRestarts());
        solver.setSimplifier(solver.SIMPLE_SIMPLIFICATION);
        return solver;
    }

    /**
     * Resolution based solver (i.e. classic SAT solver able to handle generic
     * constraints. No specific inference mechanism). Uses glucose based memory
     * management. The reason simplification is now disabled by default because
     * it slows down a lot when long PB or cardinality constraints are used.
     * 
     * @return the best available resolution based solver of the library.
     */
    public static PBSolverResolution newResolutionGlucose() {
        PBSolverResolution solver = newCompetPBResLongWLMixedConstraintsObjectiveExpSimp();
        solver.setSimplifier(Solver.NO_SIMPLIFICATION);
        solver.setRestartStrategy(new Glucose21Restarts());
        solver.setLearnedConstraintsDeletionStrategy(solver.lbd_based);
        return solver;
    }

    /**
     * Resolution based solver (i.e. classic SAT solver able to handle generic
     * constraints. No specific inference mechanism). Uses glucose based memory
     * management. The reason simplification is now disabled by default because
     * it slows down a lot when long PB or cardinality constraints are used.
     * Uses the dynamic restart strategy of Glucose21.
     * 
     * @return the best available resolution based solver of the library.
     */
    public static PBSolverResolution newResolutionGlucose21() {
        PBSolverResolution solver = newResolutionGlucose();
        solver.setRestartStrategy(new Glucose21Restarts());
        return solver;
    }

    /**
     * Resolution based solver (i.e. classic SAT solver able to handle generic
     * constraints. No specific inference mechanism). Uses glucose based memory
     * management.
     * 
     * @return the best available resolution based solver of the library.
     */
    public static PBSolverResolution newResolutionGlucoseSimpleSimp() {
        PBSolverResolution solver = newResolutionGlucose();
        solver.setSimplifier(solver.SIMPLE_SIMPLIFICATION);
        return solver;
    }

    /**
     * Resolution based solver (i.e. classic SAT solver able to handle generic
     * constraints. No specific inference mechanism). Uses glucose based memory
     * management. It uses the expensive reason simplification. If the problem
     * contains long PB or cardinality constraints, it might be slowed down by
     * such treatment.
     * 
     * @return the best available resolution based solver of the library.
     */
    public static PBSolverResolution newResolutionGlucoseExpSimp() {
        PBSolverResolution solver = newResolutionGlucose();
        solver.setSimplifier(solver.EXPENSIVE_SIMPLIFICATION);
        return solver;
    }

    /**
     * Resolution based solver (i.e. classic SAT solver able to handle generic
     * constraints. No specific inference mechanism). Uses glucose based memory
     * management.
     * 
     * @return the best available resolution based solver of the library.
     */
    public static IPBSolver newSimpleSimplification() {
        PBSolverResolution solver = newCompetPBResWLMixedConstraintsObjectiveExpSimp();
        solver.setLearnedConstraintsDeletionStrategy(solver.lbd_based);
        solver.setSimplifier(solver.SIMPLE_SIMPLIFICATION);
        return solver;
    }

    /**
     * Resolution based solver (i.e. classic SAT solver able to handle generic
     * constraints. No specific inference mechanism). Uses glucose based memory
     * management. Uses a simple restart strategy (original Minisat's one).
     * 
     * @return the best available resolution based solver of the library.
     */
    public static IPBSolver newResolutionSimpleRestarts() {
        PBSolverResolution solver = newCompetPBResLongWLMixedConstraintsObjectiveExpSimp();
        solver.setLearnedConstraintsDeletionStrategy(solver.lbd_based);
        solver.setRestartStrategy(new MiniSATRestarts());
        return solver;
    }

    /**
     * Resolution based solver (i.e. classic SAT solver able to handle generic
     * constraints. No specific inference mechanism).
     * 
     * Keeps the constraints as long as there is enough memory available.
     * 
     * @return the best available resolution based solver of the library.
     */
    public static IPBSolver newResolutionMaxMemory() {
        PBSolverResolution solver = newCompetPBResLongWLMixedConstraintsObjectiveExpSimp();
        solver.setLearnedConstraintsDeletionStrategy(solver.activity_based_low_memory);
        return solver;
    }

    /**
     * Default solver of the SolverFactory. This solver is meant to be used on
     * challenging SAT benchmarks.
     * 
     * @return the best "general purpose" SAT solver available in the factory.
     * @see #defaultSolver() the same method, polymorphic, to be called from an
     *      instance of ASolverFactory.
     */
    public static PBSolver newDefault() {
        return newResolutionGlucose21();
    }

    /**
     * Default solver of the SolverFactory for instances not normalized. This
     * solver is meant to be used on challenging SAT benchmarks.
     * 
     * @return the best "general purpose" SAT solver available in the factory.
     * @see #defaultSolver() the same method, polymorphic, to be called from an
     *      instance of ASolverFactory.
     */
    public static IPBSolver newDefaultNonNormalized() {
        PBSolver solver = newDefault();
        CompetResolutionPBLongMixedWLClauseCardConstrDataStructure ds = new CompetResolutionPBLongMixedWLClauseCardConstrDataStructure();
        ds.setNormalizer(AbstractPBDataStructureFactory.NO_COMPETITION);
        solver.setDataStructureFactory(ds);
        return solver;
    }

    /**
     * Provides the best available PB solver of the library ready to solve
     * satisfaction problems. If you need to solve optimization problems, please
     * use {@link #newDefaultOptimizer()}
     * 
     * @return a solver ready to solve satisfaction problems.
     */
    @Override
    public PBSolver defaultSolver() {
        return newDefault();
    }

    /**
     * Provides the best available PB solver of the library ready to solve
     * optimization problems. If you only need to solve satisfaction problems,
     * please use {@link #newDefault()} instead.
     * 
     * @return a solver ready to solve optimization problems.
     * @since 2.3.3
     */
    public static IPBSolver newDefaultOptimizer() {
        return new OptToPBSATAdapter(new PseudoOptDecorator(newDefault()));
    }

    /**
     * Small footprint SAT solver.
     * 
     * @return a SAT solver suitable for solving small/easy SAT benchmarks.
     * @see #lightSolver() the same method, polymorphic, to be called from an
     *      instance of ASolverFactory.
     */
    public static IPBSolver newLight() {
        return newCompetPBResMixedConstraintsObjectiveExpSimp();
    }

    @Override
    public IPBSolver lightSolver() {
        return newLight();
    }

    public static IPBSolver newEclipseP2() {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<PBDataStructureFactory>();
        PBSolverResolution solver = new PBSolverResolution(learning,
                new CompetResolutionPBMixedHTClauseCardConstrDataStructure(),
                new VarOrderHeapObjective(new RSATPhaseSelectionStrategy()),
                new ArminRestarts());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        solver.setTimeoutOnConflicts(300);
        solver.setVerbose(false);
        solver.setLearnedConstraintsDeletionStrategy(solver.activity_based_low_memory);
        return new OptToPBSATAdapter(new PseudoOptDecorator(solver));
    }

    public static PreprocCardConstrLearningSolver<IPBSolver> newDetectCards() {
        return new PreprocCardConstrLearningSolver<IPBSolver>(
                SolverFactory.newCuttingPlanes());
    }

    public static IPBSolver newInprocDetectCards() {
        InprocCardConstrLearningSolver solver = (InprocCardConstrLearningSolver) SolverFactory
                .newLazyInprocDetectCards();
        solver.setDetectCardFromAllConstraintsInCflAnalysis(true);
        return solver;
    }

    public static IPBSolver newLazyInprocDetectCards() {
        return new InprocCardConstrLearningSolver(
                new MiniSATLearning<PBDataStructureFactory>(),
                new PBMaxClauseCardConstrDataStructure(), new VarOrderHeap(),
                true);
    }

}