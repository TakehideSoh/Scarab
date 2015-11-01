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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
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

public class ObjectiveReducerPBSolverDecorator implements IPBSolver {

    private static final long serialVersionUID = -1637773414229087105L;

    private final IPBSolver decorated;

    private final List<IVecInt> atMostOneCstrs = new ArrayList<IVecInt>();

    public ObjectiveReducerPBSolverDecorator(IPBSolver decorated) {
        this.decorated = decorated;
    }

    public int[] model() {
        return decorated.model();
    }

    @SuppressWarnings("deprecation")
    public int newVar() {
        return decorated.newVar();
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger d) throws ContradictionException {
        return decorated.addPseudoBoolean(lits, coeffs, moreThan, d);
    }

    public boolean model(int var) {
        return decorated.model(var);
    }

    public int nextFreeVarId(boolean reserve) {
        return decorated.nextFreeVarId(reserve);
    }

    public int[] primeImplicant() {
        return decorated.primeImplicant();
    }

    public boolean primeImplicant(int p) {
        return decorated.primeImplicant(p);
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        for (IteratorInt it = coeffs.iterator(); it.hasNext();) {
            if (it.next() != degree) {
                return decorated.addAtMost(literals, coeffs, degree);
            }
        }
        this.atMostOneCstrs.add(literals);
        return decorated.addAtMost(literals, coeffs, degree);
    }

    public boolean isSatisfiable() throws TimeoutException {
        return decorated.isSatisfiable();
    }

    public boolean isSatisfiable(IVecInt assumps, boolean globalTimeout)
            throws TimeoutException {
        return decorated.isSatisfiable(assumps, globalTimeout);
    }

    public void registerLiteral(int p) {
        decorated.registerLiteral(p);
    }

    public void setExpectedNumberOfClauses(int nb) {
        decorated.setExpectedNumberOfClauses(nb);
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        for (Iterator<BigInteger> it = coeffs.iterator(); it.hasNext();) {
            if (!it.next().equals(degree)) {
                return decorated.addAtMost(literals, coeffs, degree);
            }
        }
        this.atMostOneCstrs.add(literals);
        return decorated.addAtMost(literals, coeffs, degree);
    }

    public boolean isSatisfiable(boolean globalTimeout) throws TimeoutException {
        return decorated.isSatisfiable(globalTimeout);
    }

    public IConstr addClause(IVecInt literals) throws ContradictionException {
        return decorated.addClause(literals);
    }

    public boolean isSatisfiable(IVecInt assumps) throws TimeoutException {
        return decorated.isSatisfiable(assumps);
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        return decorated.addAtLeast(literals, coeffs, degree);
    }

    public int[] findModel() throws TimeoutException {
        return decorated.findModel();
    }

    public IConstr addBlockingClause(IVecInt literals)
            throws ContradictionException {
        return decorated.addBlockingClause(literals);
    }

    public IConstr discardCurrentModel() throws ContradictionException {
        return decorated.discardCurrentModel();
    }

    public IVecInt createBlockingClauseForCurrentModel() {
        return decorated.createBlockingClauseForCurrentModel();
    }

    public boolean removeConstr(IConstr c) {
        return decorated.removeConstr(c);
    }

    public int[] findModel(IVecInt assumps) throws TimeoutException {
        return decorated.findModel(assumps);
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        return decorated.addAtLeast(literals, coeffs, degree);
    }

    public boolean removeSubsumedConstr(IConstr c) {
        return decorated.removeSubsumedConstr(c);
    }

    public int nConstraints() {
        return decorated.nConstraints();
    }

    public int newVar(int howmany) {
        return decorated.newVar(howmany);
    }

    public void addAllClauses(IVec<IVecInt> clauses)
            throws ContradictionException {
        for (Iterator<IVecInt> it = clauses.iterator(); it.hasNext();) {
            addClause(it.next());
        }
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
            throws ContradictionException {
        return decorated.addExactly(literals, coeffs, weight);
    }

    public int nVars() {
        return decorated.nVars();
    }

    @SuppressWarnings("deprecation")
    public void printInfos(PrintWriter out, String prefix) {
        decorated.printInfos(out, prefix);
    }

    public IConstr addAtMost(IVecInt literals, int degree)
            throws ContradictionException {
        if (degree == 1) {
            this.atMostOneCstrs.add(literals);
        }
        return decorated.addAtMost(literals, degree);
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight) throws ContradictionException {
        return decorated.addExactly(literals, coeffs, weight);
    }

    public void printInfos(PrintWriter out) {
        decorated.printInfos(out);
    }

    public IConstr addAtLeast(IVecInt literals, int degree)
            throws ContradictionException {
        return decorated.addAtLeast(literals, degree);
    }

    public void setObjectiveFunction(ObjectiveFunction obj) {
        if (obj != null) {
            IVecInt newVars = new VecInt();
            IVec<BigInteger> newCoeffs = new Vec<BigInteger>();
            Set<Integer> oldVarsToIgnore = new HashSet<Integer>();
            IVecInt oldObjVars = obj.getVars();
            IVec<BigInteger> oldObjCoeffs = obj.getCoeffs();
            int nbReduc;
            nbReduc = processAtMostOneCstrs(obj, newVars, newCoeffs,
                    oldVarsToIgnore);
            System.out.println("c " + nbReduc
                    + " reductions due to atMostOne constraints");
            for (int i = 0; i < oldObjVars.size(); ++i) {
                if (!oldVarsToIgnore.contains(oldObjVars.get(i))) {
                    newVars.push(oldObjVars.get(i));
                    newCoeffs.push(oldObjCoeffs.get(i));
                }
            }
            obj = new ObjectiveFunction(newVars, newCoeffs);
        }
        decorated.setObjectiveFunction(obj);
    }

    private int processAtMostOneCstrs(ObjectiveFunction obj, IVecInt newVars,
            IVec<BigInteger> newCoeffs, Set<Integer> oldVarsToIgnore) {
        System.out.println("c " + this.atMostOneCstrs.size()
                + " atMostOne constraints found");
        int nbReduc = 0;
        IVecInt oldObjVars = obj.getVars();
        IVec<BigInteger> oldObjCoeffs = obj.getCoeffs();
        for (IVecInt lits : this.atMostOneCstrs) {
            boolean allLitsIn = true;
            for (IteratorInt it = lits.iterator(); it.hasNext();) {
                int next = it.next();
                if (oldVarsToIgnore.contains(next)) {
                    allLitsIn = false;
                    break;
                }
                int indexOf = oldObjVars.indexOf(next);
                if (indexOf == -1
                        || !oldObjCoeffs.get(indexOf).equals(BigInteger.ONE)) {
                    allLitsIn = false;
                    break;
                }
            }
            if (allLitsIn) {
                int newObjVar = nextFreeVarId(true);
                try {
                    for (IteratorInt it = lits.iterator(); it.hasNext();) {
                        int nextInt = it.next();
                        this.decorated.addClause(new VecInt(new int[] {
                                nextInt, newObjVar }));
                        oldVarsToIgnore.add(Integer.valueOf(nextInt));
                        ++nbReduc;
                    }
                } catch (ContradictionException e) {
                    // should not occur
                    e.printStackTrace();
                }

                newVars.push(newObjVar);
                newCoeffs.push(BigInteger.ONE);
                --nbReduc;
            }
        }
        return nbReduc;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return decorated.getObjectiveFunction();
    }

    public IConstr addExactly(IVecInt literals, int n)
            throws ContradictionException {
        return decorated.addExactly(literals, n);
    }

    public void setTimeout(int t) {
        decorated.setTimeout(t);
    }

    public void setTimeoutOnConflicts(int count) {
        decorated.setTimeoutOnConflicts(count);
    }

    public void setTimeoutMs(long t) {
        decorated.setTimeoutMs(t);
    }

    public int getTimeout() {
        return decorated.getTimeout();
    }

    public long getTimeoutMs() {
        return decorated.getTimeoutMs();
    }

    public void expireTimeout() {
        decorated.expireTimeout();
    }

    public void reset() {
        decorated.reset();
    }

    @SuppressWarnings("deprecation")
    public void printStat(PrintStream out, String prefix) {
        decorated.printStat(out, prefix);
    }

    @SuppressWarnings("deprecation")
    public void printStat(PrintWriter out, String prefix) {
        decorated.printStat(out, prefix);
    }

    public void printStat(PrintWriter out) {
        decorated.printStat(out);
    }

    public Map<String, Number> getStat() {
        return decorated.getStat();
    }

    public String toString(String prefix) {
        return decorated.toString(prefix);
    }

    public void clearLearntClauses() {
        decorated.clearLearntClauses();
    }

    public void setDBSimplificationAllowed(boolean status) {
        decorated.setDBSimplificationAllowed(status);
    }

    public boolean isDBSimplificationAllowed() {
        return decorated.isDBSimplificationAllowed();
    }

    public <S extends ISolverService> void setSearchListener(
            SearchListener<S> sl) {
        decorated.setSearchListener(sl);
    }

    public <S extends ISolverService> SearchListener<S> getSearchListener() {
        return decorated.getSearchListener();
    }

    public boolean isVerbose() {
        return decorated.isVerbose();
    }

    public void setVerbose(boolean value) {
        decorated.setVerbose(value);
    }

    public void setLogPrefix(String prefix) {
        decorated.setLogPrefix(prefix);
    }

    public String getLogPrefix() {
        return decorated.getLogPrefix();
    }

    public IVecInt unsatExplanation() {
        return decorated.unsatExplanation();
    }

    public int[] modelWithInternalVariables() {
        return decorated.modelWithInternalVariables();
    }

    public int realNumberOfVariables() {
        return decorated.realNumberOfVariables();
    }

    public boolean isSolverKeptHot() {
        return decorated.isSolverKeptHot();
    }

    public void setKeepSolverHot(boolean keepHot) {
        decorated.setKeepSolverHot(keepHot);
    }

    public ISolver getSolvingEngine() {
        return decorated.getSolvingEngine();
    }

    public void setUnitClauseProvider(UnitClauseProvider ucp) {
        decorated.setUnitClauseProvider(ucp);
    }

    public IConstr addConstr(Constr constr) {
        return decorated.addConstr(constr);
    }

}
