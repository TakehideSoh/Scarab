package org.sat4j.pb.tools;

import java.math.BigInteger;

import org.sat4j.core.LiteralsUtils;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.LearningStrategy;
import org.sat4j.minisat.core.Pair;
import org.sat4j.minisat.core.RestartStrategy;
import org.sat4j.minisat.core.SearchParams;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.IPBSolverService;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.constraints.pb.IConflict;
import org.sat4j.pb.constraints.pb.PBConstr;
import org.sat4j.pb.core.PBDataStructureFactory;
import org.sat4j.pb.core.PBSolverCP;
import org.sat4j.specs.Constr;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.SearchListenerAdapter;

public class InprocCardConstrLearningSolver extends PBSolverCP {

    private static final long serialVersionUID = 1L;

    private final IPBSolver coSolver;
    private final CardConstrFinder cardFinder;

    private Constr extendedConstr;

    private boolean detectCardFromAllConstraintsInCflAnalysis = false;

    public InprocCardConstrLearningSolver(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, IOrder order, boolean noRemove) {
        super(learner, dsf, order, noRemove);
        this.coSolver = SolverFactory.newResolution();
        this.cardFinder = new CardConstrFinder(this.coSolver);
        configureSolver();
    }

    public InprocCardConstrLearningSolver(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, IOrder order) {
        super(learner, dsf, order);
        this.coSolver = SolverFactory.newResolution();
        this.cardFinder = new CardConstrFinder(this.coSolver);
        configureSolver();
    }

    public InprocCardConstrLearningSolver(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, SearchParams params, IOrder order,
            boolean noRemove) {
        super(learner, dsf, params, order, noRemove);
        this.coSolver = SolverFactory.newResolution();
        this.cardFinder = new CardConstrFinder(this.coSolver);
        configureSolver();
    }

    public InprocCardConstrLearningSolver(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, SearchParams params, IOrder order,
            RestartStrategy restarter, boolean noRemove) {
        super(learner, dsf, params, order, restarter, noRemove);
        this.coSolver = SolverFactory.newResolution();
        this.cardFinder = new CardConstrFinder(this.coSolver);
        configureSolver();
    }

    public InprocCardConstrLearningSolver(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, SearchParams params, IOrder order,
            RestartStrategy restarter) {
        super(learner, dsf, params, order, restarter);
        this.coSolver = SolverFactory.newResolution();
        this.cardFinder = new CardConstrFinder(this.coSolver);
        configureSolver();
    }

    public InprocCardConstrLearningSolver(
            LearningStrategy<PBDataStructureFactory> learner,
            PBDataStructureFactory dsf, SearchParams params, IOrder order) {
        super(learner, dsf, params, order);
        this.coSolver = SolverFactory.newResolution();
        this.cardFinder = new CardConstrFinder(this.coSolver);
        configureSolver();
    }

    public void setDetectCardFromAllConstraintsInCflAnalysis(boolean value) {
        this.detectCardFromAllConstraintsInCflAnalysis = value;
    }

    private void configureSolver() {
        this.setSearchListener(new SearchListenerAdapter<IPBSolverService>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void conflictFound(IConstr confl, int dlevel, int trailLevel) {
                handleConflict(confl);
            }
        });
    }

    protected void handleConflict(IConstr confl) {
        this.extendedConstr = null;
        if (constraintIsAdmissibleForExtension(confl)) {
            tryToExtendConstraint((PBConstr) confl);
        }
    }

    private void tryToExtendConstraint(PBConstr confl) {
        // translation from Minisat literals to Dimacs literals
        IVecInt atMostLits = new VecInt(confl.getLits().length);
        for (int lit : confl.getLits()) {
            atMostLits.push((lit >> 1) * ((lit & 1) == 1 ? -1 : 1));
        }
        IVecInt discovered = this.cardFinder.searchCardFromAtMostCard(
                atMostLits, atMostLits.size() - 1);
        if (discovered != null) {
            IConstr constr = this.addAtMostOnTheFly(discovered, new VecInt(
                    discovered.size(), 1), atMostLits.size() - 1);
            // if (this.isVerbose())
            // this.out.log(getLogPrefix() + "newCard " + constr
            // + " discovered from " + confl);
            this.sharedConflict = null;
            this.extendedConstr = (Constr) constr;
        }
        // else {
        // if (this.isVerbose())
        // this.out.log(getLogPrefix() + "noCard from " + confl);
        // }
    }

    private boolean constraintIsAdmissibleForExtension(IConstr confl) {
        return confl instanceof PBConstr
                && ((PBConstr) confl).canBeSatisfiedByCountingLiterals()
                && ((PBConstr) confl).requiredNumberOfSatisfiedLiterals() == 1;
    }

    @Override
    public void analyzeCP(Constr myconfl, Pair results) throws TimeoutException {
        if (this.detectCardFromAllConstraintsInCflAnalysis) {
            if (extendedConstr == null) {
                cardDetectionAnalyzeCP(myconfl, results);
            } else {
                cardDetectionAnalyzeCP(extendedConstr, results);
            }
        } else {
            if (extendedConstr == null) {
                super.analyzeCP(myconfl, results);
            } else {
                super.analyzeCP(extendedConstr, results);
            }
        }
    }

    public void cardDetectionAnalyzeCP(Constr myconfl, Pair results)
            throws TimeoutException {
        int litImplied = this.trail.last();
        int currentLevel = this.voc.getLevel(litImplied);
        IConflict confl = chooseConflict((PBConstr) myconfl, currentLevel);
        assert confl.slackConflict().signum() < 0;
        while (!confl.isAssertive(currentLevel)) {
            if (!this.undertimeout) {
                throw new TimeoutException();
            }
            PBConstr constraint = (PBConstr) this.voc.getReason(litImplied);
            this.extendedConstr = null;
            if (constraintIsAdmissibleForExtension(constraint)) {
                tryToExtendConstraint(constraint);
            }
            if (this.extendedConstr != null) {
                constraint = (PBConstr) extendedConstr;
            }
            // result of the resolution is in the conflict (confl)
            confl.resolve(constraint, litImplied, this);
            updateNumberOfReductions(confl);
            assert confl.slackConflict().signum() <= 0;
            // implication trail is reduced
            if (this.trail.size() == 1) {
                break;
            }
            undoOne();
            // assert decisionLevel() >= 0;
            if (decisionLevel() == 0) {
                break;
            }
            litImplied = this.trail.last();
            if (this.voc.getLevel(litImplied) != currentLevel) {
                this.trailLim.pop();
                slistener.backtracking(LiteralsUtils.toDimacs(litImplied));
                confl.updateSlack(this.voc.getLevel(litImplied));
            }
            assert this.voc.getLevel(litImplied) <= currentLevel;
            currentLevel = this.voc.getLevel(litImplied);
            assert confl.slackIsCorrect(currentLevel);
            assert currentLevel == decisionLevel();
            assert litImplied > 1;
        }
        assert confl.isAssertive(currentLevel) || this.trail.size() == 1
                || decisionLevel() == 0;

        assert currentLevel == decisionLevel();
        undoOne();
        this.qhead = this.trail.size();
        updateNumberOfReducedLearnedConstraints(confl);
        // necessary informations to build a PB-constraint
        // are kept from the conflict
        if (confl.size() == 0
                || (decisionLevel() == 0 || this.trail.size() == 0)
                && confl.slackConflict().signum() < 0) {
            results.reason = null;
            results.backtrackLevel = -1;
            return;
        }

        // assertive PB-constraint is build and referenced
        PBConstr resConstr = (PBConstr) this.dsfactory
                .createUnregisteredPseudoBooleanConstraint(confl);
        results.reason = resConstr;

        // the conflict give the highest decision level for the backtrack
        // (which is less than current level)
        // assert confl.isAssertive(currentLevel);
        if (decisionLevel() == 0 || this.trail.size() == 0) {
            results.backtrackLevel = -1;
            results.reason = null;
        } else {
            results.backtrackLevel = confl.getBacktrackLevel(currentLevel);
        }
    }

    // Overriding constraint adding methods to store constraints in both solver
    // and coSolver

    @Override
    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger d) throws ContradictionException {
        this.coSolver.addPseudoBoolean(lits, coeffs, moreThan, d);
        return super.addPseudoBoolean(lits, coeffs, moreThan, d);
    }

    @Override
    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        this.coSolver.addAtMost(literals, coeffs, degree);
        return super.addAtMost(literals, coeffs, degree);
    }

    @Override
    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        this.coSolver.addAtMost(literals, coeffs, degree);
        return super.addAtMost(literals, coeffs, degree);
    }

    @Override
    public IConstr addClause(IVecInt literals) throws ContradictionException {
        this.coSolver.addClause(literals);
        return super.addClause(literals);
    }

    @Override
    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        this.coSolver.addAtLeast(literals, coeffs, degree);
        return super.addAtLeast(literals, coeffs, degree);
    }

    @Override
    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        this.coSolver.addAtLeast(literals, coeffs, degree);
        return super.addAtLeast(literals, coeffs, degree);
    }

    @Override
    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
            throws ContradictionException {
        this.coSolver.addExactly(literals, coeffs, weight);
        return super.addExactly(literals, coeffs, weight);
    }

    @Override
    public void addAllClauses(IVec<IVecInt> clauses)
            throws ContradictionException {
        this.coSolver.addAllClauses(clauses);
        super.addAllClauses(clauses);
    }

    @Override
    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight) throws ContradictionException {
        this.coSolver.addExactly(literals, coeffs, weight);
        return super.addExactly(literals, coeffs, weight);
    }

    @Override
    public IConstr addAtMost(IVecInt literals, int degree)
            throws ContradictionException {
        this.coSolver.addAtMost(literals, degree);
        return super.addAtMost(literals, degree);
    }

    @Override
    public IConstr addAtLeast(IVecInt literals, int degree)
            throws ContradictionException {
        this.coSolver.addAtLeast(literals, degree);
        return super.addAtLeast(literals, degree);
    }

    @Override
    public IConstr addExactly(IVecInt literals, int n)
            throws ContradictionException {
        this.coSolver.addExactly(literals, n);
        return super.addExactly(literals, n);
    }

    @Override
    public IConstr addConstr(Constr constr) {
        this.coSolver.addConstr(constr);
        return super.addConstr(constr);
    }

}
