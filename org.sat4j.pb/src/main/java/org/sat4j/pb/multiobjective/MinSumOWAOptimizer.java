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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IIntegerPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.core.IntegerVariable;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;

/**
 * An optimizer intended to solve an increasing weights OWA based problem. The
 * encoding takes benefit from the fact that a weighted permutation of integer
 * values is minimal if the lowest weights go with the lowest values.
 * 
 * @author lonca
 */
public class MinSumOWAOptimizer extends AbstractLinMultiObjOptimizer {

    private static final long serialVersionUID = 1L;
    private final BigInteger[] weights;
    private IntegerVariable objBoundVar;

    /**
     * @see MinSumOWAOptimizer(IIntegerPBSolver solver, BigInteger[] weights)
     */
    public MinSumOWAOptimizer(IIntegerPBSolver solver, int[] weights) {
        super(solver);
        this.weights = new BigInteger[weights.length];
        for (int i = 0; i < weights.length; ++i) {
            this.weights[i] = BigInteger.valueOf(weights[i]);
        }
    }

    /**
     * @param solver
     *            an Integer PB Solver
     * @param weights
     *            OWA not decreasing weights
     */
    public MinSumOWAOptimizer(IIntegerPBSolver solver, BigInteger[] weights) {
        super(solver);
        this.weights = weights;
    }

    @Override
    protected void setInitConstraints() {
        try {
            objBoundVar = this.integerSolver.newIntegerVar(globalObjBound());
            for (List<Integer> permutation : new PermutationComputer(
                    super.objs.size())) {
                addConstraint(objBoundVar, permutation);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        // setInitConstraintsAux();
    }

    private void addConstraint(IntegerVariable boundVar,
            List<Integer> permutation) {
        IVecInt vars = new VecInt();
        IVec<BigInteger> weights = new Vec<BigInteger>();
        for (int i = 0; i < super.objs.size(); ++i) {
            ObjectiveFunction currentObj = super.objs.get(permutation.get(i));
            BigInteger curWeight = this.weights[i];
            currentObj.getVars().copyTo(vars);
            for (Iterator<BigInteger> coeffIt = currentObj.getCoeffs()
                    .iterator(); coeffIt.hasNext();) {
                weights.push(coeffIt.next().multiply(curWeight));
            }
        }
        BigInteger fact = BigInteger.ONE;
        for (IteratorInt litsIt = boundVar.getVars().iterator(); litsIt
                .hasNext();) {
            int nextLit = litsIt.next() * -1;
            vars.push(nextLit);
            weights.push(fact);
            fact = fact.shiftLeft(1);
        }
        try {
            decorated().addAtMost(vars, weights, fact.subtract(BigInteger.ONE));
        } catch (ContradictionException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private BigInteger globalObjBound() {
        BigInteger bound = BigInteger.ONE;
        for (ObjectiveFunction obj : super.objs) {
            for (Iterator<BigInteger> coeffIt = obj.getCoeffs().iterator(); coeffIt
                    .hasNext();) {
                bound = bound.add(coeffIt.next());
            }
        }
        return bound;
    }

    @Override
    protected void setGlobalObj() {
        decorated().setObjectiveFunction(
                new ObjectiveFunction(new VecInt(), new Vec<BigInteger>()));
        this.integerSolver.addIntegerVariableToObjectiveFunction(
                this.objBoundVar, BigInteger.ONE);
    }

    @Override
    public Number calculateObjective() {
        super.objectiveValue = BigInteger.ZERO;
        BigInteger[] objValues = getObjectiveValues();
        for (int i = 0; i < objValues.length; ++i) {
            super.objectiveValue = super.objectiveValue.add(objValues[i]
                    .multiply(this.weights[i]));
        }
        return getObjectiveValue();
    }

    class PermutationComputer implements Iterator<List<Integer>>,
            Iterable<List<Integer>> {

        private int currentElement;

        private final SortedSet<Integer> elements;

        private PermutationComputer end = null;

        private boolean soleIntegerReturned = false;

        public PermutationComputer(int nbElem) {
            this.elements = new TreeSet<Integer>();
            for (int i = 0; i < nbElem; ++i) {
                this.elements.add(i);
            }
        }

        public PermutationComputer(Set<Integer> elements) {
            this.elements = new TreeSet<Integer>(elements);
        }

        public Iterator<List<Integer>> iterator() {
            return this;
        }

        public boolean hasNext() {
            return !soleIntegerReturned
                    && (this.end == null || this.end.hasNext() || this.currentElement != this.elements
                            .last());
        }

        public List<Integer> next() {
            List<Integer> res = new ArrayList<Integer>();
            if (this.elements.size() == 1) {
                return addSoleInteger(res);
            }
            if (this.end == null) {
                createChildPermutation();
            }
            if (!this.end.hasNext()) {
                changeCurrentAndChild();
            }
            return addCurrentAndNextInChild(res);
        }

        private void changeCurrentAndChild() {
            this.currentElement = this.elements
                    .tailSet(this.currentElement + 1).first();
            SortedSet<Integer> endElements = new TreeSet<Integer>();
            for (Integer element : this.elements) {
                if (!element.equals(currentElement)) {
                    endElements.add(element);
                }
            }
            this.end = new PermutationComputer(endElements);
        }

        private List<Integer> addCurrentAndNextInChild(List<Integer> res) {
            res.add(this.currentElement);
            res.addAll(this.end.next());
            return res;
        }

        private void createChildPermutation() {
            this.currentElement = this.elements.first();
            SortedSet<Integer> endElements = this.elements
                    .tailSet(this.currentElement + 1);
            this.end = new PermutationComputer(endElements);
        }

        private List<Integer> addSoleInteger(List<Integer> res) {
            this.soleIntegerReturned = true;
            res.add(this.elements.first());
            return res;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    // modif begin

    // private ObjectiveFunction sumObj;
    // private ObjectiveFunction lexObj;
    // private IConstr lexCstr;
    // private IConstr sumCstr;
    // protected final List<IntegerVariable> objBoundVariables = new
    // ArrayList<IntegerVariable>();
    // private final List<IVecInt> atLeastFlags = new ArrayList<IVecInt>();
    //
    // @Override
    // public void discardCurrentSolution() throws ContradictionException {
    // if (this.sumCstr != null) {
    // this.decorated().removeSubsumedConstr(this.sumCstr);
    // }
    // if (this.lexCstr != null) {
    // this.decorated().removeSubsumedConstr(this.lexCstr);
    // }
    // super.discardCurrentSolution();
    // this.lexCstr = decorated().addAtMost(this.lexObj.getVars(),
    // this.lexObj.getCoeffs(), maxLexBound());
    // this.sumCstr = decorated().addAtMost(this.sumObj.getVars(),
    // this.sumObj.getCoeffs(), maxSumBound());
    // }
    //
    // private BigInteger maxSumBound() {
    // BigInteger weigthsSum = BigInteger.ZERO;
    // for (BigInteger weight : weights)
    // weigthsSum = weigthsSum.add(weight);
    // BigInteger res = super.objectiveValue.divide(weigthsSum);
    // res = res.add(BigInteger.ONE);
    // res = res.multiply(BigInteger.valueOf(super.objs.size()));
    // return res;
    // }
    //
    // private BigInteger maxLexBound() {
    // BigInteger maxObjValue = super.objectiveValue.divide(
    // BigInteger.valueOf(super.objs.size())).add(BigInteger.ONE);
    // return maxObjValue.multiply(BigInteger.valueOf(
    // 1 << objBoundVariables.get(0).getVars().size()).multiply(
    // BigInteger.valueOf(super.objs.size() - 1)));
    // }

    // private void setInitConstraintsAux() {
    // String owaWeightsProperty = System.getProperty("_owaWeights");
    // if (owaWeightsProperty != null) {
    // String[] weights = owaWeightsProperty.split(",");
    // for (int i = 0; i < this.weights.length; ++i) {
    // this.weights[i] = BigInteger.valueOf(Long.valueOf(weights[i]));
    // }
    // }
    // if (decorated().isVerbose()) {
    // System.out.println(getLogPrefix() + "OWA weights : "
    // + Arrays.toString(weights));
    // }
    // BigInteger minObjValuesBound = minObjValuesBound();
    // for (int i = 0; i < super.objs.size(); ++i) {
    // IntegerVariable boundVar = this.integerSolver
    // .newIntegerVar(minObjValuesBound);
    // this.objBoundVariables.add(boundVar);
    // this.atLeastFlags.add(new VecInt());
    // for (int j = 0; j < super.objs.size(); ++j) {
    // addBoundConstraint(i, boundVar, j);
    // }
    // addFlagsCardinalityConstraint(i);
    // }
    // createSumAndLexObjs();
    // }
    //
    // private void addBoundConstraint(int boundVarIndex,
    // IntegerVariable boundVar, int objIndex) {
    // IVecInt literals = new VecInt();
    // IVec<BigInteger> coeffs = new Vec<BigInteger>();
    // super.objs.get(objIndex).getVars().copyTo(literals);
    // super.objs.get(objIndex).getCoeffs().copyTo(coeffs);
    // int flagLit = decorated().nextFreeVarId(true);
    // this.atLeastFlags.get(boundVarIndex).push(flagLit);
    // literals.push(flagLit);
    // coeffs.push(minObjValuesBound().negate());
    // try {
    // this.integerSolver.addAtMost(literals, coeffs,
    // new Vec<IntegerVariable>().push(boundVar),
    // new Vec<BigInteger>().push(BigInteger.ONE.negate()),
    // BigInteger.ZERO);
    // } catch (ContradictionException e) {
    // throw new RuntimeException(e);
    // }
    // }

    // private void createSumAndLexObjs() {
    // IVecInt auxObjsVars = new VecInt();
    // IVec<BigInteger> sumObjCoeffs = new Vec<BigInteger>();
    // IVec<BigInteger> lexObjCoeffs = new Vec<BigInteger>();
    // BigInteger lexFactor = BigInteger.ONE;
    // for (Iterator<IntegerVariable> intVarIt = objBoundVariables.iterator();
    // intVarIt
    // .hasNext();) {
    // BigInteger sumFactor = BigInteger.ONE;
    // IntegerVariable nextBoundVar = intVarIt.next();
    // for (IteratorInt nextBoundVarLitsIt = nextBoundVar.getVars()
    // .iterator(); nextBoundVarLitsIt.hasNext();) {
    // auxObjsVars.push(nextBoundVarLitsIt.next());
    // sumObjCoeffs.push(sumFactor);
    // sumFactor = sumFactor.shiftLeft(1);
    // lexObjCoeffs.push(lexFactor);
    // lexFactor = lexFactor.shiftLeft(1);
    // }
    // }
    // this.sumObj = new ObjectiveFunction(auxObjsVars, sumObjCoeffs);
    // this.lexObj = new ObjectiveFunction(auxObjsVars, lexObjCoeffs);
    // }
    //
    // private void addFlagsCardinalityConstraint(int card) {
    // try {
    // decorated().addAtMost(this.atLeastFlags.get(card), card);
    // } catch (ContradictionException e) {
    // throw new RuntimeException(e);
    // }
    // }

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

}
