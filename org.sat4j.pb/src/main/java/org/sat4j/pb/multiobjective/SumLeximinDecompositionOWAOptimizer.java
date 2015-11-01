package org.sat4j.pb.multiobjective;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IIntegerPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.pb.core.IntegerVariable;
import org.sat4j.specs.Constr;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.ISolverService;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;
import org.sat4j.specs.SearchListener;
import org.sat4j.specs.TimeoutException;
import org.sat4j.specs.UnitClauseProvider;

public class SumLeximinDecompositionOWAOptimizer implements
        IMultiObjOptimizationProblem, IIntegerPBSolver {

    private final List<ObjectiveFunction> objs = new ArrayList<ObjectiveFunction>();
    private final BigInteger[] weights;

    private final IIntegerPBSolver solver;

    protected final List<IntegerVariable> objBoundVariables = new ArrayList<IntegerVariable>();

    private final List<IVecInt> atLeastFlags = new ArrayList<IVecInt>();

    private boolean initDone = false;
    private boolean sumLexStepDone = false;

    private ObjectiveFunction lexObj;
    private ObjectiveFunction sumObj;
    private ObjectiveFunction owaObj;

    private IConstr additionnalCstr = null;
    private BigInteger bestValue = null;
    private PseudoOptDecorator optPb;
    private int[] lastModel;
    private int[] lastModelWithInternalVars;

    public SumLeximinDecompositionOWAOptimizer(IIntegerPBSolver solver,
            int[] weights) {
        this.solver = solver;
        this.weights = new BigInteger[weights.length];
        for (int i = 0; i < weights.length; ++i) {
            this.weights[i] = BigInteger.valueOf(weights[i]);
        }
    }

    public SumLeximinDecompositionOWAOptimizer(IIntegerPBSolver solver,
            BigInteger[] weights) {
        this.solver = solver;
        this.weights = weights;
    }

    public boolean admitABetterSolution() throws TimeoutException {
        return admitABetterSolution(VecInt.EMPTY);
    }

    public boolean admitABetterSolution(IVecInt assumps)
            throws TimeoutException {
        boolean sat = false;
        if (!sumLexStepDone) {
            if (!this.initDone) {
                this.initDone = true;
                setInitConstraints();
                createGlobalObj();
                this.optPb = new PseudoOptDecorator(this.solver);
            }
        }
        if (!sumLexStepDone) {
            this.solver.setObjectiveFunction(sumObj);
            sat = launchSubOpt();
            if (!sat) {
                this.sumLexStepDone = true;
                this.initDone = false;
            }
        }
        if (!sumLexStepDone) {
            this.solver.setObjectiveFunction(lexObj);
            sat = launchSubOpt();
            if (!sat) {
                this.sumLexStepDone = true;
                this.initDone = false;
            }
        }
        if (sumLexStepDone) {
            boolean optFound = checkLeximinSumDecomposition();
            if (optFound) {
                return false;
            }
            if (!this.initDone) {
                System.out.println(this.solver.getLogPrefix()
                        + "Sum-Lex optimization step done");
                this.optPb = new PseudoOptDecorator(this.solver);
                this.optPb.setObjectiveFunction(owaObj);
                this.initDone = true;
            }
            sat = launchSubOpt();
        }
        return sat;
    }

    private boolean checkLeximinSumDecomposition() {
        BigInteger weightsSum = BigInteger.ZERO;
        for (int i = 0; i < weights.length; ++i) {
            weightsSum = weightsSum.add(weights[i]);
        }
        BigInteger minDiv = weights[weights.length - 1];
        for (int i = 0; i < weights.length - 1; ++i) {
            minDiv = minDiv.min(weights[i + 1].multiply(weightsSum).divide(
                    weights[i]));
        }
        BigInteger objBound = weightsSum.divide(weights[weights.length - 1])
                .multiply(getObjectiveValues()[weights.length - 1]);
        return (objBound.compareTo(minDiv) <= 0);
    }

    private boolean launchSubOpt() throws TimeoutException {
        boolean sat = false;
        while (optPb.admitABetterSolution()) {
            sat = true;
            this.lastModel = this.solver.model();
            this.lastModelWithInternalVars = this.solver
                    .modelWithInternalVariables();
            if (this.additionnalCstr != null) {
                System.out.println(this.solver.getLogPrefix()
                        + "Current objectives values: "
                        + Arrays.toString(this.getObjectiveValues()));
                System.out.println(this.solver.getLogPrefix()
                        + "Current owa function value: owa=" + this.bestValue
                        + ", sum=" + sumObj.calculateDegree(this.solver)
                        + ", lex=" + lexObj.calculateDegree(this.solver));
                this.solver.removeSubsumedConstr(this.additionnalCstr);
            }
            try {
                calculateObjective();
                optPb.discardCurrentSolution();
                this.additionnalCstr = this.solver.addAtMost(
                        this.owaObj.getVars(), this.owaObj.getCoeffs(),
                        this.bestValue.subtract(BigInteger.ONE));
            } catch (ContradictionException e) {
                break;
            }
        }
        return sat;
    }

    protected void setInitConstraints() {
        String owaWeightsProperty = System.getProperty("_owaWeights");
        if (owaWeightsProperty != null) {
            String[] weights = owaWeightsProperty.split(",");
            for (int i = 0; i < this.weights.length; ++i) {
                this.weights[i] = BigInteger.valueOf(Long.valueOf(weights[i]));
            }
        }
        if (this.solver.isVerbose()) {
            System.out.println(this.solver.getLogPrefix() + "OWA weights : "
                    + Arrays.toString(weights));
        }
        BigInteger minObjValuesBound = minObjValuesBound();
        for (int i = 0; i < this.objs.size(); ++i) {
            IntegerVariable boundVar = this.solver
                    .newIntegerVar(minObjValuesBound);
            this.objBoundVariables.add(boundVar);
            this.atLeastFlags.add(new VecInt());
            for (int j = 0; j < this.objs.size(); ++j) {
                addBoundConstraint(i, boundVar, j);
            }
            addFlagsCardinalityConstraint(i);
        }
        createSumAndLexObjs();
    }

    private void createSumAndLexObjs() {
        IVecInt auxObjsVars = new VecInt();
        IVec<BigInteger> sumObjCoeffs = new Vec<BigInteger>();
        IVec<BigInteger> lexObjCoeffs = new Vec<BigInteger>();
        BigInteger lexFactor = BigInteger.ONE;
        for (Iterator<IntegerVariable> intVarIt = objBoundVariables.iterator(); intVarIt
                .hasNext();) {
            BigInteger sumFactor = BigInteger.ONE;
            IntegerVariable nextBoundVar = intVarIt.next();
            for (IteratorInt nextBoundVarLitsIt = nextBoundVar.getVars()
                    .iterator(); nextBoundVarLitsIt.hasNext();) {
                auxObjsVars.push(nextBoundVarLitsIt.next());
                sumObjCoeffs.push(sumFactor);
                sumFactor = sumFactor.shiftLeft(1);
                lexObjCoeffs.push(lexFactor);
                lexFactor = lexFactor.shiftLeft(1);
            }
        }
        this.sumObj = new ObjectiveFunction(auxObjsVars, sumObjCoeffs);
        this.lexObj = new ObjectiveFunction(auxObjsVars, lexObjCoeffs);
    }

    private void addBoundConstraint(int boundVarIndex,
            IntegerVariable boundVar, int objIndex) {
        IVecInt literals = new VecInt();
        IVec<BigInteger> coeffs = new Vec<BigInteger>();
        this.objs.get(objIndex).getVars().copyTo(literals);
        this.objs.get(objIndex).getCoeffs().copyTo(coeffs);
        int flagLit = this.solver.nextFreeVarId(true);
        this.atLeastFlags.get(boundVarIndex).push(flagLit);
        literals.push(flagLit);
        coeffs.push(minObjValuesBound().negate());
        try {
            this.solver.addAtMost(literals, coeffs,
                    new Vec<IntegerVariable>().push(boundVar),
                    new Vec<BigInteger>().push(BigInteger.ONE.negate()),
                    BigInteger.ZERO);
        } catch (ContradictionException e) {
            throw new RuntimeException(e);
        }
    }

    private void addFlagsCardinalityConstraint(int card) {
        try {
            this.solver.addAtMost(this.atLeastFlags.get(card), card);
        } catch (ContradictionException e) {
            throw new RuntimeException(e);
        }
    }

    protected BigInteger minObjValuesBound() {
        BigInteger maxValue = BigInteger.ZERO;
        for (Iterator<ObjectiveFunction> objsIt = this.objs.iterator(); objsIt
                .hasNext();) {
            ObjectiveFunction nextObj = objsIt.next();
            BigInteger maxObjValue = maxObjValue(nextObj);
            if (maxValue.compareTo(maxObjValue) < 0) {
                maxValue = maxObjValue;
            }
        }
        return maxValue.add(BigInteger.ONE);
    }

    private BigInteger maxObjValue(ObjectiveFunction obj) {
        IVec<BigInteger> objCoeffs = obj.getCoeffs();
        BigInteger coeffsSum = BigInteger.ZERO;
        for (Iterator<BigInteger> objCoeffsIt = objCoeffs.iterator(); objCoeffsIt
                .hasNext();) {
            coeffsSum = coeffsSum.add(objCoeffsIt.next());
        }
        return coeffsSum;
    }

    private void createGlobalObj() {
        ObjectiveFunction oldObj = getObjectiveFunction();
        this.solver.setObjectiveFunction(new ObjectiveFunction(new VecInt(),
                new Vec<BigInteger>()));
        for (int i = 0; i < objBoundVariables.size(); ++i) {
            this.solver.addIntegerVariableToObjectiveFunction(
                    objBoundVariables.get(i), weights[weights.length - i - 1]);
        }
        this.owaObj = this.solver.getObjectiveFunction();
        this.solver.setObjectiveFunction(oldObj);
    }

    public boolean hasNoObjectiveFunction() {
        return false;
    }

    public boolean nonOptimalMeansSatisfiable() {
        return true;
    }

    public Number calculateObjective() {
        this.bestValue = BigInteger.ZERO;
        BigInteger[] values = getObjectiveValues();
        for (int i = 0; i < this.objs.size(); ++i) {
            this.bestValue = this.bestValue.add(values[i]
                    .multiply(this.weights[i]));
        }
        return this.bestValue;
    }

    public Number getObjectiveValue() {
        return this.bestValue;
    }

    public void forceObjectiveValueTo(Number forcedValue)
            throws ContradictionException {
        throw new UnsupportedOperationException();
    }

    public void discard() throws ContradictionException {
        discardCurrentSolution();

    }

    public void discardCurrentSolution() throws ContradictionException {
        // TODO Auto-generated method stub

    }

    public boolean isOptimal() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setTimeoutForFindingBetterSolution(int seconds) {
        throw new UnsupportedOperationException();
    }

    public void addObjectiveFunction(ObjectiveFunction obj) {
        this.objs.add(obj);
    }

    public BigInteger[] getObjectiveValues() {
        BigInteger[] res = new BigInteger[this.objs.size()];
        for (int i = 0; i < this.objs.size(); ++i) {
            res[i] = this.objs.get(i).calculateDegree(solver);
        }
        Arrays.sort(res);
        return res;
    }

    public boolean model(int var) {
        return this.lastModelWithInternalVars[var - 1] > 0;
    }

    public int[] model() {
        return this.lastModel;
    }

    public int[] modelWithInternalVariables() {
        return this.lastModelWithInternalVars;
    }

    // BEGIN DELEGATION

    public int[] primeImplicant() {
        return solver.primeImplicant();
    }

    public boolean primeImplicant(int p) {
        return solver.primeImplicant(p);
    }

    public boolean isSatisfiable() throws TimeoutException {
        return solver.isSatisfiable();
    }

    public boolean isSatisfiable(IVecInt assumps, boolean globalTimeout)
            throws TimeoutException {
        return solver.isSatisfiable(assumps, globalTimeout);
    }

    public boolean isSatisfiable(boolean globalTimeout) throws TimeoutException {
        return solver.isSatisfiable(globalTimeout);
    }

    public boolean isSatisfiable(IVecInt assumps) throws TimeoutException {
        return solver.isSatisfiable(assumps);
    }

    public int[] findModel() throws TimeoutException {
        return solver.findModel();
    }

    public int[] findModel(IVecInt assumps) throws TimeoutException {
        return solver.findModel(assumps);
    }

    public int nConstraints() {
        return solver.nConstraints();
    }

    public int newVar(int howmany) {
        return solver.newVar(howmany);
    }

    public int nVars() {
        return solver.nVars();
    }

    @SuppressWarnings("deprecation")
    public void printStat(PrintStream out, String prefix) {
        solver.printStat(out, prefix);
    }

    @SuppressWarnings("deprecation")
    public void printStat(PrintWriter out, String prefix) {
        solver.printStat(out, prefix);
    }

    public void printStat(PrintWriter out) {
        solver.printStat(out);
    }

    public Map<String, Number> getStat() {
        return solver.getStat();
    }

    public String toString(String prefix) {
        return solver.toString(prefix);
    }

    public void clearLearntClauses() {
        solver.clearLearntClauses();
    }

    public void setDBSimplificationAllowed(boolean status) {
        solver.setDBSimplificationAllowed(status);
    }

    public boolean isDBSimplificationAllowed() {
        return solver.isDBSimplificationAllowed();
    }

    public <S extends ISolverService> void setSearchListener(
            SearchListener<S> sl) {
        solver.setSearchListener(sl);
    }

    public void setUnitClauseProvider(UnitClauseProvider ucp) {
        solver.setUnitClauseProvider(ucp);
    }

    public <S extends ISolverService> SearchListener<S> getSearchListener() {
        return solver.getSearchListener();
    }

    public boolean isVerbose() {
        return solver.isVerbose();
    }

    public void setVerbose(boolean value) {
        solver.setVerbose(value);
    }

    public void setLogPrefix(String prefix) {
        solver.setLogPrefix(prefix);
    }

    public String getLogPrefix() {
        return solver.getLogPrefix();
    }

    public IVecInt unsatExplanation() {
        return solver.unsatExplanation();
    }

    public int realNumberOfVariables() {
        return solver.realNumberOfVariables();
    }

    public boolean isSolverKeptHot() {
        return solver.isSolverKeptHot();
    }

    public void setKeepSolverHot(boolean keepHot) {
        solver.setKeepSolverHot(keepHot);
    }

    public ISolver getSolvingEngine() {
        return solver.getSolvingEngine();
    }

    public IntegerVariable newIntegerVar(BigInteger maxValue) {
        return solver.newIntegerVar(maxValue);
    }

    public BigInteger getIntegerVarValue(IntegerVariable var) {
        return solver.getIntegerVarValue(var);
    }

    public IConstr addAtLeast(IntegerVariable var, int degree)
            throws ContradictionException {
        return solver.addAtLeast(var, degree);
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger degree)
            throws ContradictionException {
        return solver.addAtLeast(literals, coeffs, integerVars,
                integerVarsCoeffs, degree);
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int degree)
            throws ContradictionException {
        return solver.addAtLeast(literals, coeffs, integerVars,
                integerVarsCoeffs, degree);
    }

    public IConstr addAtMost(IntegerVariable var, int degree)
            throws ContradictionException {
        return solver.addAtMost(var, degree);
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger degree)
            throws ContradictionException {
        return solver.addAtMost(literals, coeffs, integerVars,
                integerVarsCoeffs, degree);
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int degree)
            throws ContradictionException {
        return solver.addAtMost(literals, coeffs, integerVars,
                integerVarsCoeffs, degree);
    }

    public IConstr addExactly(IntegerVariable var, int degree)
            throws ContradictionException {
        return solver.addExactly(var, degree);
    }

    public int newVar() {
        return solver.newVar();
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger weight)
            throws ContradictionException {
        return solver.addExactly(literals, coeffs, integerVars,
                integerVarsCoeffs, weight);
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger d) throws ContradictionException {
        return solver.addPseudoBoolean(lits, coeffs, moreThan, d);
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int weight)
            throws ContradictionException {
        return solver.addExactly(literals, coeffs, integerVars,
                integerVarsCoeffs, weight);
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, boolean moreThan, BigInteger d)
            throws ContradictionException {
        return solver.addPseudoBoolean(lits, coeffs, integerVars,
                integerVarsCoeffs, moreThan, d);
    }

    public int nextFreeVarId(boolean reserve) {
        return solver.nextFreeVarId(reserve);
    }

    public void addIntegerVariableToObjectiveFunction(IntegerVariable var,
            BigInteger weight) {
        solver.addIntegerVariableToObjectiveFunction(var, weight);
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        return solver.addAtMost(literals, coeffs, degree);
    }

    public void registerLiteral(int p) {
        solver.registerLiteral(p);
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        return solver.addAtMost(literals, coeffs, degree);
    }

    public void setExpectedNumberOfClauses(int nb) {
        solver.setExpectedNumberOfClauses(nb);
    }

    public IConstr addClause(IVecInt literals) throws ContradictionException {
        return solver.addClause(literals);
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        return solver.addAtLeast(literals, coeffs, degree);
    }

    public IConstr addBlockingClause(IVecInt literals)
            throws ContradictionException {
        return solver.addBlockingClause(literals);
    }

    public IConstr discardCurrentModel() throws ContradictionException {
        return solver.discardCurrentModel();
    }

    public IVecInt createBlockingClauseForCurrentModel() {
        return solver.createBlockingClauseForCurrentModel();
    }

    public boolean removeConstr(IConstr c) {
        return solver.removeConstr(c);
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        return solver.addAtLeast(literals, coeffs, degree);
    }

    public boolean removeSubsumedConstr(IConstr c) {
        return solver.removeSubsumedConstr(c);
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
            throws ContradictionException {
        return solver.addExactly(literals, coeffs, weight);
    }

    public void addAllClauses(IVec<IVecInt> clauses)
            throws ContradictionException {
        solver.addAllClauses(clauses);
    }

    public void printInfos(PrintWriter out, String prefix) {
        solver.printInfos(out, prefix);
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight) throws ContradictionException {
        return solver.addExactly(literals, coeffs, weight);
    }

    public void printInfos(PrintWriter out) {
        solver.printInfos(out);
    }

    public IConstr addAtMost(IVecInt literals, int degree)
            throws ContradictionException {
        return solver.addAtMost(literals, degree);
    }

    public void setObjectiveFunction(ObjectiveFunction obj) {
        solver.setObjectiveFunction(obj);
    }

    public IConstr addAtLeast(IVecInt literals, int degree)
            throws ContradictionException {
        return solver.addAtLeast(literals, degree);
    }

    public ObjectiveFunction getObjectiveFunction() {
        return solver.getObjectiveFunction();
    }

    public IConstr addExactly(IVecInt literals, int n)
            throws ContradictionException {
        return solver.addExactly(literals, n);
    }

    public IConstr addConstr(Constr constr) {
        return solver.addConstr(constr);
    }

    public void setTimeout(int t) {
        solver.setTimeout(t);
    }

    public void setTimeoutOnConflicts(int count) {
        solver.setTimeoutOnConflicts(count);
    }

    public void setTimeoutMs(long t) {
        solver.setTimeoutMs(t);
    }

    public int getTimeout() {
        return solver.getTimeout();
    }

    public long getTimeoutMs() {
        return solver.getTimeoutMs();
    }

    public void expireTimeout() {
        solver.expireTimeout();
    }

    public void reset() {
        solver.reset();
    }

    // END DELEGATION

}