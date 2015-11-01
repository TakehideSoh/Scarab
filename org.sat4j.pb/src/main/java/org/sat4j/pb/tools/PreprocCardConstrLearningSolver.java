package org.sat4j.pb.tools;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sat4j.core.ASolverFactory;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.constraints.PBMaxClauseCardConstrDataStructure;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.ISolverService;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;
import org.sat4j.specs.SearchListener;
import org.sat4j.specs.TimeoutException;

public class PreprocCardConstrLearningSolver<S extends IPBSolver> extends
        ManyCorePB<IPBSolver> {

    private static final long serialVersionUID = 1L;

    private CardConstrFinder cardFinder;

    private boolean initDone = false;

    private boolean preprocessing = true;

    private long preprocessingTime = 0;

    private long preprocessingCardsFound = 0;

    private int solverIndex = 0;

    private final int maxAtMostDegree = 3;

    private boolean verbose = true;

    private Map<Integer, BigInteger> objWeightsMap = null;

    private BigInteger objMinBound = null;

    public PreprocCardConstrLearningSolver(ASolverFactory<IPBSolver> factory,
            String solverName) {
        super(factory, solverName, solverName);
        this.solvers.get(0).setVerbose(true);
        this.solvers.get(1).setVerbose(false);
        this.cardFinder = new CardConstrFinder(this.solvers.get(1));
    }

    public PreprocCardConstrLearningSolver(IPBSolver solverToFill) {
        super(solverToFill, SolverFactory.newPBCP(
                new PBMaxClauseCardConstrDataStructure(), new VarOrderHeap()));
        this.solvers.get(0).setVerbose(true);
        this.solvers.get(1).setVerbose(false);
        this.cardFinder = new CardConstrFinder(this.solvers.get(1));
    }

    public void init() {
        if (getObjectiveFunction() != null) {
            this.objWeightsMap = new HashMap<Integer, BigInteger>();
            IteratorInt litsIt = getObjectiveFunction().getVars().iterator();
            Iterator<BigInteger> weightsIt = getObjectiveFunction().getCoeffs()
                    .iterator();
            for (; litsIt.hasNext();)
                this.objWeightsMap.put(litsIt.next(), weightsIt.next());
        }
        if (this.preprocessing) {
            sat4jPreprocessing();
        } else if (this.rissLocation != null) {
            rissPreprocessing();
        } else {
            noPreprocessing();
        }
    }

    private void noPreprocessing() {
        solverIndex = 1;
        this.cardFinder.forget();
        this.cardFinder = null;
    }

    private void sat4jPreprocessing() {
        if (verbose)
            System.out
                    .println("c launching cardinality constraint revelation process");
        long start = System.currentTimeMillis();
        this.cardFinder.searchCards();
        doPreprocessing();
        this.preprocessingTime += (System.currentTimeMillis() - start);
        this.initDone = true;
        PrintWriter out = new PrintWriter(System.out, true);
        if (verbose)
            printPreprocessingStats(out);
        this.solvers.set(1, null);
    }

    private void updateObjMinBound(AtLeastCard card) {
        AtMostCard atMost = card.toAtMost();
        if (this.objWeightsMap == null)
            return;
        List<BigInteger> weights = new ArrayList<BigInteger>();
        for (IteratorInt it = atMost.getLits().iterator(); it.hasNext();) {
            BigInteger weight = this.objWeightsMap.get(-it.next());
            if (weight != null)
                weights.add(weight);
        }
        Collections.sort(weights);
        BigInteger newMinBound = BigInteger.ZERO;
        for (int i = 0; i < weights.size() - atMost.getDegree(); ++i) {
            newMinBound = newMinBound.add(weights.get(i));
        }
        if (this.objMinBound == null
                || this.objMinBound.compareTo(newMinBound) == -1)
            this.objMinBound = newMinBound;
    }

    private void rissPreprocessing() {
        if (verbose)
            System.out
                    .println("c launching cardinality constraint revelation process");
        long start = System.currentTimeMillis();
        this.cardFinder.rissPreprocessing(this.rissLocation, this.instance);
        doPreprocessing();
        this.preprocessingTime += (System.currentTimeMillis() - start);
        this.initDone = true;
        PrintWriter out = new PrintWriter(System.out, true);
        if (verbose)
            printPreprocessingStats(out);
        this.solvers.set(1, null);
    }

    private void doPreprocessing() {
        for (Iterator<AtLeastCard> it = this.cardFinder; it.hasNext();) {
            AtLeastCard card = it.next();
            updateObjMinBound(card);
            ++this.preprocessingCardsFound;
            try {
                this.solvers.get(0)
                        .addAtLeast(card.getLits(), card.getDegree());
            } catch (ContradictionException e) {
            }
        }
        for (AtLeastCard card : this.cardFinder.remainingAtLeastCards()) {
            try {
                this.solvers.get(0)
                        .addAtLeast(card.getLits(), card.getDegree());
            } catch (ContradictionException e) {
            }
        }
    }

    public void setPreprocessing(boolean preprocessing) {
        this.preprocessing = preprocessing;
        if (verbose)
            System.out.println("c preprocessing set to " + preprocessing);
    }

    //
    // overriding ManyCore methods
    //

    @Override
    public IConstr addClause(IVecInt literals) throws ContradictionException {
        if (literals.size() > 1 && literals.size() <= this.maxAtMostDegree + 1) {
            this.cardFinder.addClause(literals);
            return this.solvers.get(1).addClause(literals);
        } else {
            return super.addClause(literals);
        }
    }

    @Override
    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger d) throws ContradictionException {
        return moreThan ? addAtLeast(lits, coeffs, d) : addAtMost(lits, coeffs,
                d);
    }

    @Override
    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        for (int i = 0; i < coeffs.size(); ++i) {
            int coeff = coeffs.get(i);
            if (coeff < 0) {
                literals.set(i, -literals.get(i));
                coeffs.set(i, -coeff);
                degree -= coeff;
            }
        }
        if (degree == 1)
            return addClause(literals);
        return super.addAtLeast(literals, coeffs, degree);
    }

    @Override
    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        for (int i = 0; i < coeffs.size(); ++i) {
            BigInteger coeff = coeffs.get(i);
            if (coeff.compareTo(BigInteger.ZERO) < 0) {
                literals.set(i, -literals.get(i));
                coeffs.set(i, coeff.negate());
                degree = degree.subtract(coeff);
            }
        }
        if (degree.compareTo(BigInteger.ONE) == 0)
            return addClause(literals);
        return super.addAtLeast(literals, coeffs, degree);
    }

    @Override
    public void expireTimeout() {
        this.solvers.get(solverIndex).expireTimeout();
    }

    @Override
    public Map<String, Number> getStat() {
        return this.solvers.get(solverIndex).getStat();
    }

    @Override
    @Deprecated
    public void printStat(PrintStream out, String prefix) {
        this.solvers.get(0).printStat(out, prefix);
    }

    @Override
    @Deprecated
    public void printStat(PrintWriter out, String prefix) {
        this.solvers.get(0).printStat(out, prefix);
    }

    @Override
    public boolean isSatisfiable() throws TimeoutException {
        if (!initDone) {
            init();
        }
        return this.solvers.get(solverIndex).isSatisfiable();
    }

    @Override
    public synchronized boolean isSatisfiable(IVecInt assumps,
            boolean globalTimeout) throws TimeoutException {
        if (!initDone) {
            init();
        }
        return this.solvers.get(solverIndex).isSatisfiable(assumps,
                globalTimeout);
    }

    @Override
    public boolean isSatisfiable(boolean globalTimeout) throws TimeoutException {
        if (!initDone) {
            init();
        }
        return this.solvers.get(solverIndex).isSatisfiable(globalTimeout);
    }

    @Override
    public boolean isSatisfiable(IVecInt assumps) throws TimeoutException {
        if (!initDone) {
            init();
        }
        return this.solvers.get(solverIndex).isSatisfiable(assumps);
    }

    @Override
    public int[] model() {
        return this.solvers.get(solverIndex).model();
    }

    @Override
    public boolean model(int var) {
        return this.solvers.get(solverIndex).model(var);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void printInfos(PrintWriter out, String prefix) {
        this.solvers.get(solverIndex).printInfos(out, prefix);
    }

    @Override
    public <I extends ISolverService> void setSearchListener(
            SearchListener<I> sl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I extends ISolverService> SearchListener<I> getSearchListener() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVerbose(boolean value) {
        this.verbose = value;
        this.solvers.get(solverIndex).setVerbose(value);
    }

    @Override
    public IVecInt unsatExplanation() {
        return this.solvers.get(solverIndex).unsatExplanation();
    }

    @Override
    public int[] primeImplicant() {
        return this.solvers.get(solverIndex).primeImplicant();
    }

    @Override
    public boolean primeImplicant(int p) {
        return this.solvers.get(solverIndex).primeImplicant(p);
    }

    @Override
    public List<IPBSolver> getSolvers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] modelWithInternalVariables() {
        return this.solvers.get(solverIndex).modelWithInternalVariables();
    }

    @Override
    public void printStat(PrintWriter out) {
        this.solvers.get(solverIndex).printStat(out);
        printPreprocessingStats(out);
    }

    private void printPreprocessingStats(PrintWriter out) {
        if (this.cardFinder == null)
            return;
        out.println("c remaining constraints: "
                + this.cardFinder.remainingAtLeastCards().size() + "/"
                + this.cardFinder.initNumberOfClauses());
        out.println("c cardinality constraints found (preprocessing): "
                + this.preprocessingCardsFound);
        out.println("c cardinality search time (preprocessing): "
                + this.preprocessingTime + "ms");
        Map<Integer, Map<Integer, Integer>> cardsStats = new HashMap<Integer, Map<Integer, Integer>>();
        for (Iterator<AtLeastCard> it = this.cardFinder; it.hasNext();) {
            AtMostCard card = it.next().toAtMost();
            int degree = card.getDegree();
            Map<Integer, Integer> sizeMap = cardsStats.get(degree);
            if (sizeMap == null) {
                sizeMap = new HashMap<Integer, Integer>();
                cardsStats.put(degree, sizeMap);
            }
            Integer size = sizeMap.get(card.getLits().size());
            sizeMap.put(card.getLits().size(), size == null ? 1 : size + 1);
        }
        for (Map.Entry<Integer, Map<Integer, Integer>> cardinality : cardsStats
                .entrySet()) {
            for (Map.Entry<Integer, Integer> size : cardinality.getValue()
                    .entrySet()) {
                out.println("c found " + size.getValue()
                        + " at-most cardinality constraint of degree "
                        + cardinality.getKey() + " and size " + size.getKey());
            }
        }
        System.out
                .println("c solver contains "
                        + this.solvers.get(solverIndex).nConstraints()
                        + " constraints");
    }

    @Override
    public void printInfos(PrintWriter out) {
        this.solvers.get(solverIndex).printInfos(out);
    }

    private boolean toStringRunning = false;

    private String rissLocation = null;

    private String instance;

    @Override
    public String toString(String prefix) {
        if (toStringRunning)
            return "cardConstrFinderListener";
        this.toStringRunning = true;
        String res = this.solvers.get(solverIndex).toString(prefix);
        this.toStringRunning = false;
        return res;
    }

    @Override
    public String toString() {
        return toString("c ");
    }

    public void setPrintCards(boolean b) {
        this.cardFinder.setPrintCards(true);
    }

    public void setRissLocation(String property) {
        this.rissLocation = property;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public void setAuthorizedExtLits(IVecInt lits) {
        this.cardFinder.setAuthorizedExtLits(lits);
    }

    public IVecInt searchCardFromClause(IVecInt clause) {
        return this.cardFinder.searchCardFromClause(clause);
    }

    public BigInteger getObjMinBound() {
        return this.objMinBound;
    }

    @Override
    public boolean removeConstr(IConstr c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeSubsumedConstr(IConstr c) {
        throw new UnsupportedOperationException();
    }

}