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

package org.sat4j.pb.tools;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.Backbone;
import org.sat4j.tools.GateTranslator;

/**
 * Helper class intended to make life easier to people to feed a sat solver
 * programmatically.
 * 
 * @author daniel
 * 
 * @param <T>
 *            The class of the objects to map into boolean variables.
 * @param <C>
 *            The class of the object to map to each constraint.
 */
public class DependencyHelper<T, C> {

    public static final INegator NO_NEGATION = new INegator() {

        public boolean isNegated(Object thing) {
            return false;
        }

        public Object unNegate(Object thing) {
            return thing;
        }
    };

    public static final INegator BASIC_NEGATION = new INegator() {

        public boolean isNegated(Object thing) {
            return thing instanceof Negation;
        }

        public Object unNegate(Object thing) {
            return ((Negation) thing).getThing();
        }
    };

    private static final class Negation {
        private final Object thing;

        Negation(Object thing) {
            this.thing = thing;
        }

        Object getThing() {
            return this.thing;
        }

        @Override
        public String toString() {
            return "-" + thing;
        }
    }

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private final Map<T, Integer> mapToDimacs = new HashMap<T, Integer>();
    private final Map<Integer, T> mapToDomain = new HashMap<Integer, T>();
    final Map<IConstr, C> descs = new HashMap<IConstr, C>();

    private final XplainPB xplain;
    private final GateTranslator gator;
    final IPBSolver solver;
    private INegator negator = BASIC_NEGATION;

    private ObjectiveFunction objFunction;
    private IVecInt objLiterals;
    private IVec<BigInteger> objCoefs;

    private final boolean explanationEnabled;
    private final boolean canonicalOptFunction;

    /**
     * 
     * @param solver
     *            the solver to be used to solve the problem.
     */
    public DependencyHelper(IPBSolver solver) {
        this(solver, true);
    }

    /**
     * 
     * @param solver
     *            the solver to be used to solve the problem.
     * @param explanationEnabled
     *            if true, will add one new variable per constraint to allow the
     *            solver to compute an explanation in case of failure. Default
     *            is true. Usually, this is set to false when one wants to check
     *            the encoding produced by the helper.
     */
    public DependencyHelper(IPBSolver solver, boolean explanationEnabled) {
        this(solver, explanationEnabled, true);
    }

    /**
     * 
     * @param solver
     *            the solver to be used to solve the problem.
     * @param explanationEnabled
     *            if true, will add one new variable per constraint to allow the
     *            solver to compute an explanation in case of failure. Default
     *            is true. Usually, this is set to false when one wants to check
     *            the encoding produced by the helper.
     * @param canonicalOptFunctionEnabled
     *            when set to true, the objective function sum up all the
     *            coefficients for a given literal. The default is true. It is
     *            useful to set it to false when checking the encoding produced
     *            by the helper.
     * @since 2.2
     */
    public DependencyHelper(IPBSolver solver, boolean explanationEnabled,
            boolean canonicalOptFunctionEnabled) {
        if (explanationEnabled) {
            this.xplain = new XplainPB(solver);
            this.solver = this.xplain;
        } else {
            this.xplain = null;
            this.solver = solver;
        }
        this.gator = new GateTranslator(this.solver);
        this.explanationEnabled = explanationEnabled;
        this.canonicalOptFunction = canonicalOptFunctionEnabled;
    }

    public void setNegator(INegator negator) {
        this.negator = negator;
    }

    /**
     * Translate a domain object into a dimacs variable.
     * 
     * @param thing
     *            a domain object
     * @return the dimacs variable (an integer) representing that domain object.
     */
    protected int getIntValue(T thing) {
        return getIntValue(thing, true);
    }

    /**
     * Translate a domain object into a dimacs variable.
     * 
     * @param thing
     *            a domain object
     * @param create
     *            to allow or not the solver to create a new id if the object in
     *            unknown. If set to false, the method will throw an
     *            IllegalArgumentException if the object is unknown.
     * @return the dimacs variable (an integer) representing that domain object.
     */
    protected int getIntValue(T thing, boolean create) {
        T myThing;
        boolean negated = this.negator.isNegated(thing);
        if (negated) {
            myThing = (T) this.negator.unNegate(thing);
        } else {
            myThing = thing;
        }
        Integer intValue = this.mapToDimacs.get(myThing);
        if (intValue == null) {
            if (create) {
                intValue = this.solver.nextFreeVarId(true);
                this.mapToDomain.put(intValue, myThing);
                this.mapToDimacs.put(myThing, intValue);
            } else {
                throw new IllegalArgumentException("" + myThing
                        + " is unknown in the solver!");
            }
        }
        if (negated) {
            return -intValue;
        }
        return intValue;
    }

    /**
     * Retrieve the solution found.
     * 
     * Note that this method returns a Sat4j specific data structure (IVec).
     * People willing to retrieve a more common data structure should use
     * {@link #getASolution()} instead.
     * 
     * THAT METHOD IS EXPECTED TO BE CALLED IF hasASolution() RETURNS TRUE.
     * 
     * @return the domain object that must be satisfied to satisfy the
     *         constraints entered in the solver.
     * @see {@link #hasASolution()}
     * @see {@link #getASolution()}
     */
    public IVec<T> getSolution() {
        int[] model = this.solver.model();
        IVec<T> toInstall = new Vec<T>();
        if (model != null) {
            for (int i : model) {
                T obj = this.mapToDomain.get(i);
                if (obj != null) {
                    toInstall.push(obj);
                }
            }
        }
        return toInstall;
    }

    /**
     * Retrieve a collection of objects satisfying the constraints.
     * 
     * It behaves basically the same as {@link #getSolution()} but returns a
     * common Collection instead of a Sat4j specific IVec.
     * 
     * THAT METHOD IS EXPECTED TO BE CALLED IF hasASolution() RETURNS TRUE.
     * 
     * @return the domain object that must be satisfied to satisfy the
     *         constraints entered in the solver.
     * @see {@link #hasASolution()}
     * @see {@link #getSolution()}
     * @since 2.3.1
     */
    public Collection<T> getASolution() {
        int[] model = this.solver.model();
        Collection<T> toInstall = new ArrayList<T>();
        if (model != null) {
            for (int i : model) {
                if (i > 0) {
                    toInstall.add(this.mapToDomain.get(i));
                }
            }
        }
        return toInstall;
    }

    public BigInteger getSolutionCost() {
        return this.objFunction.calculateDegree(this.solver);
    }

    /**
     * Retrieve the boolean value associated with a domain object in the
     * solution found by the solver. THAT METHOD IS EXPECTED TO BE CALLED IF
     * hasASolution() RETURNS TRUE.
     * 
     * @param t
     *            a domain object
     * @return true iff the domain object has been set to true in the current
     *         solution.
     * @throws IllegalArgumentException
     *             If the argument of the method is unknown to the solver.
     */
    public boolean getBooleanValueFor(T t) {
        int dimacsValue = getIntValue(t, false);
        if (dimacsValue > 0) {
            return this.solver.model(dimacsValue);
        } else {
            return !this.solver.model(-dimacsValue);
        }
    }

    /**
     * 
     * @return true if the set of constraints entered inside the solver can be
     *         satisfied.
     * @throws TimeoutException
     */
    public boolean hasASolution() throws TimeoutException {
        return this.solver.isSatisfiable();
    }

    /**
     * 
     * @return true if the set of constraints entered inside the solver can be
     *         satisfied.
     * @throws TimeoutException
     */
    public boolean hasASolution(IVec<T> assumps) throws TimeoutException {
        IVecInt assumptions = new VecInt();
        for (Iterator<T> it = assumps.iterator(); it.hasNext();) {
            assumptions.push(getIntValue(it.next()));
        }
        return this.solver.isSatisfiable(assumptions);
    }

    /**
     * 
     * @return true if the set of constraints entered inside the solver can be
     *         satisfied.
     * @throws TimeoutException
     */
    public boolean hasASolution(Collection<T> assumps) throws TimeoutException {
        IVecInt assumptions = new VecInt();
        for (T t : assumps) {
            assumptions.push(getIntValue(t));
        }
        return this.solver.isSatisfiable(assumptions);
    }

    /**
     * Explain the reason of the inconsistency of the set of constraints.
     * 
     * THAT METHOD IS EXPECTED TO BE CALLED IF hasASolution() RETURNS FALSE.
     * 
     * @return a set of objects used to "name" each constraint entered in the
     *         solver.
     * @throws TimeoutException
     * @see {@link #hasASolution()}
     */
    public Set<C> why() throws TimeoutException {
        if (!this.explanationEnabled) {
            throw new UnsupportedOperationException("Explanation not enabled!");
        }
        Collection<IConstr> explanation = this.xplain.explain();
        Set<C> ezexplain = new TreeSet<C>();
        for (IConstr constr : explanation) {
            C desc = this.descs.get(constr);
            if (desc != null) {
                ezexplain.add(desc);
            }
        }
        return ezexplain;
    }

    /**
     * Explain a domain object has been set to true in a solution.
     * 
     * @return a set of objects used to "name" each constraint entered in the
     *         solver.
     * @throws TimeoutException
     * @see {@link #hasASolution()}
     */
    public Set<C> why(T thing) throws TimeoutException {
        IVecInt assumps = new VecInt();
        assumps.push(-getIntValue(thing));
        return why(assumps);
    }

    /**
     * Explain a domain object has been set to false in a solution.
     * 
     * @return a set of objects used to "name" each constraint entered in the
     *         solver.
     * @throws TimeoutException
     * @see {@link #hasASolution()}
     */
    public Set<C> whyNot(T thing) throws TimeoutException {
        IVecInt assumps = new VecInt();
        assumps.push(getIntValue(thing));
        return why(assumps);
    }

    private Set<C> why(IVecInt assumps) throws TimeoutException {
        if (this.xplain.isSatisfiable(assumps)) {
            return new TreeSet<C>();
        }
        return why();
    }

    /**
     * Add a constraint to set the value of a domain object to true.
     * 
     * @param thing
     *            the domain object
     * @param name
     *            the name of the constraint, to be used in an explanation if
     *            needed.
     * @throws ContradictionException
     *             if the set of constraints appears to be trivially
     *             inconsistent.
     */
    public void setTrue(T thing, C name) throws ContradictionException {
        IConstr constr = this.gator.gateTrue(getIntValue(thing));
        if (constr != null) {
            this.descs.put(constr, name);
        }
    }

    /**
     * Add a constraint to set the value of a domain object to false.
     * 
     * @param thing
     *            the domain object
     * @param name
     *            the name of the constraint, to be used in an explanation if
     *            needed.
     * @throws ContradictionException
     *             if the set of constraints appears to be trivially
     *             inconsistent.
     */
    public void setFalse(T thing, C name) throws ContradictionException {
        IConstr constr = this.gator.gateFalse(getIntValue(thing));
        // constraints duplication detection may end up with null constraint
        if (constr != null) {
            this.descs.put(constr, name);
        }

    }

    /**
     * Create a logical implication of the form lhs -> rhs
     * 
     * @param lhs
     *            some domain objects. They form a conjunction in the left hand
     *            side of the implication.
     * @return the right hand side of the implication.
     */
    public ImplicationRHS<T, C> implication(T... lhs) {
        IVecInt clause = new VecInt();
        for (T t : lhs) {
            clause.push(-getIntValue(t));
        }
        return new ImplicationRHS<T, C>(this, clause);
    }

    public DisjunctionRHS<T, C> disjunction(T... lhs) {
        IVecInt literals = new VecInt();
        for (T t : lhs) {
            literals.push(-getIntValue(t));
        }
        return new DisjunctionRHS<T, C>(this, literals);
    }

    /**
     * Create a constraint stating that at most i domain object should be set to
     * true.
     * 
     * @param i
     *            the maximum number of domain object to set to true.
     * @param things
     *            the domain objects.
     * @return an object used to name the constraint. The constraint MUST BE
     *         NAMED.
     * @throws ContradictionException
     */
    public void atLeast(C name, int i, T... things)
            throws ContradictionException {
        IVecInt literals = new VecInt();
        for (T t : things) {
            literals.push(getIntValue(t));
        }
        this.descs.put(this.solver.addAtLeast(literals, i), name);
    }

    /**
     * Create a constraint stating that at most i domain object should be set to
     * true.
     * 
     * @param i
     *            the maximum number of domain object to set to true.
     * @param things
     *            the domain objects.
     * @return an object used to name the constraint. The constraint MUST BE
     *         NAMED.
     * @throws ContradictionException
     */
    public ImplicationNamer<T, C> atMost(int i, T... things)
            throws ContradictionException {
        IVec<IConstr> toName = new Vec<IConstr>();
        IVecInt literals = new VecInt();
        for (T t : things) {
            literals.push(getIntValue(t));
        }
        toName.push(this.solver.addAtMost(literals, i));
        return new ImplicationNamer<T, C>(this, toName);
    }

    /**
     * Create a constraint stating that at most i domain object should be set to
     * true.
     * 
     * @param i
     *            the maximum number of domain object to set to true.
     * @param things
     *            the domain objects.
     * @return an object used to name the constraint. The constraint MUST BE
     *         NAMED.
     * @throws ContradictionException
     */
    public void atMost(C name, int i, T... things)
            throws ContradictionException {
        IVecInt literals = new VecInt();
        for (T t : things) {
            literals.push(getIntValue(t));
        }
        this.descs.put(this.solver.addAtMost(literals, i), name);
    }

    /**
     * Create a clause (thing1 or thing 2 ... or thingn)
     * 
     * @param name
     * @param things
     * 
     * @throws ContradictionException
     */
    public void clause(C name, T... things) throws ContradictionException {
        IVecInt literals = new VecInt(things.length);
        for (T t : things) {
            literals.push(getIntValue(t));
        }
        IConstr constr = this.gator.addClause(literals);
        // constr can be null if duplicated clauses are detected.
        if (constr != null) {
            this.descs.put(constr, name);
        }

    }

    /**
     * Create a constraint using equivalency chains thing <=> (thing1 <=> thing2
     * <=> ... <=> thingn)
     * 
     * @param thing
     *            a domain object
     * @param things
     *            other domain objects.
     * @throws ContradictionException
     */
    public void iff(C name, T thing, T... otherThings)
            throws ContradictionException {
        IVecInt literals = new VecInt(otherThings.length);
        for (T t : otherThings) {
            literals.push(getIntValue(t));
        }
        IConstr[] constrs = this.gator.iff(getIntValue(thing), literals);
        for (IConstr constr : constrs) {
            if (constr != null) {
                this.descs.put(constr, name);
            }
        }
    }

    /**
     * Create a constraint of the form thing <=> (thing1 and thing 2 ... and
     * thingn)
     * 
     * @param name
     * @param thing
     * @param otherThings
     * @throws ContradictionException
     */
    public void and(C name, T thing, T... otherThings)
            throws ContradictionException {
        IVecInt literals = new VecInt(otherThings.length);
        for (T t : otherThings) {
            literals.push(getIntValue(t));
        }
        IConstr[] constrs = this.gator.and(getIntValue(thing), literals);
        for (IConstr constr : constrs) {
            if (constr != null) {
                this.descs.put(constr, name);
            }
        }
    }

    /**
     * Create a constraint of the form thing <=> (thing1 or thing 2 ... or
     * thingn)
     * 
     * @param name
     * @param thing
     * @param otherThings
     * @throws ContradictionException
     */
    public void or(C name, T thing, T... otherThings)
            throws ContradictionException {
        IVecInt literals = new VecInt(otherThings.length);
        for (T t : otherThings) {
            literals.push(getIntValue(t));
        }
        IConstr[] constrs = this.gator.or(getIntValue(thing), literals);
        for (IConstr constr : constrs) {
            if (constr != null) {
                this.descs.put(constr, name);
            }
        }
    }

    /**
     * Create a constraint of the form thing <= (thing1 or thing 2 ... or
     * thingn)
     * 
     * @param name
     * @param thing
     * @param otherThings
     * @throws ContradictionException
     */
    public void halfOr(C name, T thing, T... otherThings)
            throws ContradictionException {
        IVecInt literals = new VecInt(otherThings.length);
        for (T t : otherThings) {
            literals.push(getIntValue(t));
        }
        IConstr[] constrs = this.gator.halfOr(getIntValue(thing), literals);
        for (IConstr constr : constrs) {
            if (constr != null) {
                this.descs.put(constr, name);
            }
        }
    }

    /**
     * Create a constraint of the form thing <=> (if conditionThing then
     * thenThing else elseThing)
     * 
     * @param name
     * @param thing
     * @param otherThings
     * @throws ContradictionException
     */
    public void ifThenElse(C name, T thing, T conditionThing, T thenThing,
            T elseThing) throws ContradictionException {
        IConstr[] constrs = this.gator.ite(getIntValue(thing),
                getIntValue(conditionThing), getIntValue(thenThing),
                getIntValue(elseThing));
        for (IConstr constr : constrs) {
            if (constr != null) {
                this.descs.put(constr, name);
            }
        }
    }

    /**
     * Add an objective function to ask for a solution that minimize the
     * objective function.
     * 
     * @param wobj
     *            a set of weighted objects (pairs of domain object and
     *            BigInteger).
     */
    public void setObjectiveFunction(WeightedObject<T>... wobj) {
        createObjectivetiveFunctionIfNeeded(wobj.length);
        for (WeightedObject<T> wo : wobj) {
            addProperly(wo.thing, wo.getWeight());
        }

    }

    private void addProperly(T thing, BigInteger weight) {
        int lit = getIntValue(thing);
        int index;
        if (this.canonicalOptFunction
                && (index = this.objLiterals.indexOf(lit)) != -1) {
            this.objCoefs.set(index, this.objCoefs.get(index).add(weight));
            if (this.objCoefs.get(index).equals(BigInteger.ZERO)) {
                this.objLiterals.delete(index);
                this.objCoefs.delete(index);
            }
        } else {
            this.objLiterals.push(lit);
            this.objCoefs.push(weight);
        }
    }

    private void createObjectivetiveFunctionIfNeeded(int n) {
        if (this.objFunction == null) {
            this.objLiterals = new VecInt(n);
            this.objCoefs = new Vec<BigInteger>(n);
            this.objFunction = new ObjectiveFunction(this.objLiterals,
                    this.objCoefs);
            this.solver.setObjectiveFunction(this.objFunction);
        }
    }

    /**
     * Add a weighted literal to the objective function.
     * 
     * @param thing
     * @param weight
     */
    public void addToObjectiveFunction(T thing, int weight) {
        addToObjectiveFunction(thing, BigInteger.valueOf(weight));
    }

    /**
     * Add a weighted literal to the objective function.
     * 
     * @param thing
     * @param weight
     */
    public void addToObjectiveFunction(T thing, BigInteger weight) {
        createObjectivetiveFunctionIfNeeded(20);
        addProperly(thing, weight);
    }

    /**
     * Create a PB constraint of the form <code>
     * w1.l1 + w2.l2 + ... wn.ln >= degree
     * </code> where wi are position integers and li are domain objects.
     * 
     * @param degree
     * @param wobj
     * @throws ContradictionException
     */
    public void atLeast(C name, BigInteger degree, WeightedObject<T>... wobj)
            throws ContradictionException {
        IVecInt literals = new VecInt(wobj.length);
        IVec<BigInteger> coeffs = new Vec<BigInteger>(wobj.length);
        for (WeightedObject<T> wo : wobj) {
            literals.push(getIntValue(wo.thing));
            coeffs.push(wo.getWeight());
        }
        this.descs.put(
                this.solver.addPseudoBoolean(literals, coeffs, true, degree),
                name);
    }

    /**
     * Create a PB constraint of the form <code>
     * w1.l1 + w2.l2 + ... wn.ln <= degree
     * </code> where wi are position integers and li are domain objects.
     * 
     * @param degree
     * @param wobj
     * @throws ContradictionException
     */
    public void atMost(C name, BigInteger degree, WeightedObject<T>... wobj)
            throws ContradictionException {
        IVecInt literals = new VecInt(wobj.length);
        IVec<BigInteger> coeffs = new Vec<BigInteger>(wobj.length);
        for (WeightedObject<T> wo : wobj) {
            literals.push(getIntValue(wo.thing));
            coeffs.push(wo.getWeight());
        }
        this.descs.put(
                this.solver.addPseudoBoolean(literals, coeffs, false, degree),
                name);
    }

    public void atMost(C name, int degree, WeightedObject<T>... wobj)
            throws ContradictionException {
        atMost(name, BigInteger.valueOf(degree), wobj);
    }

    /**
     * Stop the SAT solver that is looking for a solution. The solver will throw
     * a TimeoutException.
     */
    public void stopSolver() {
        this.solver.expireTimeout();
    }

    /**
     * Stop the explanation computation. A TimeoutException will be thrown by
     * the explanation algorithm.
     */
    public void stopExplanation() {
        if (!this.explanationEnabled) {
            throw new UnsupportedOperationException("Explanation not enabled!");
        }
        this.xplain.cancelExplanation();
    }

    public void discard(IVec<T> things) throws ContradictionException {
        IVecInt literals = new VecInt(things.size());
        for (Iterator<T> it = things.iterator(); it.hasNext();) {
            literals.push(-getIntValue(it.next()));
        }
        this.solver.addBlockingClause(literals);
    }

    public void discardSolutionsWithObjectiveValueGreaterThan(long value)
            throws ContradictionException {
        ObjectiveFunction obj = this.solver.getObjectiveFunction();
        IVecInt literals = new VecInt(obj.getVars().size());
        obj.getVars().copyTo(literals);
        IVec<BigInteger> coeffs = new Vec<BigInteger>(obj.getCoeffs().size());
        obj.getCoeffs().copyTo(coeffs);
        this.solver.addPseudoBoolean(literals, coeffs, false,
                BigInteger.valueOf(value));
    }

    public String getObjectiveFunction() {
        ObjectiveFunction obj = this.solver.getObjectiveFunction();
        StringBuffer stb = new StringBuffer();
        for (int i = 0; i < obj.getVars().size(); i++) {
            stb.append(obj.getCoeffs().get(i)
                    + (obj.getVars().get(i) > 0 ? " " : "~")
                    + this.mapToDomain.get(Math.abs(obj.getVars().get(i)))
                    + " ");
        }
        return stb.toString();
    }

    public int getNumberOfVariables() {
        return this.mapToDimacs.size();
    }

    public int getNumberOfConstraints() {
        return this.descs.size();
    }

    public Map<Integer, T> getMappingToDomain() {
        return this.mapToDomain;
    }

    public Object not(T thing) {
        return new Negation(thing);
    }

    /**
     * @since 2.2
     * @return the IPBSolver enclosed in the helper.
     */
    public IPBSolver getSolver() {
        if (this.explanationEnabled) {
            return this.xplain.decorated();
        }
        return this.solver;
    }

    /**
     * Reset the state of the helper (mapping, objective function, etc).
     * 
     * @since 2.3.1
     */
    public void reset() {
        this.mapToDimacs.clear();
        this.mapToDomain.clear();
        this.descs.clear();
        this.solver.reset();
        if (this.objLiterals != null) {
            this.objLiterals.clear();
            this.objCoefs.clear();
        }
    }

    /**
     * Compute the objects implied by the assumptions.
     * 
     * @param assumptions
     *            already satisfied objects (e.g. user choices)
     * @param satisfied
     *            the objects that are always satisfied under assumptions
     *            (includes the objects from assumptions)
     * @param falsified
     *            the objects that are always falsified under assumptions.
     * @return the set of objects fixed
     * @throws TimeoutException
     * @since 2.3.3
     */
    public void impliedBy(Collection<T> assumptions, Collection<T> satisfied,
            Collection<T> falsified) throws TimeoutException {
        IVecInt assump = new VecInt(assumptions.size());
        for (T thing : assumptions) {
            assump.push(getIntValue(thing));
        }
        IVecInt implied = Backbone.instance().compute(solver, assump);
        int p;
        for (IteratorInt it = implied.iterator(); it.hasNext();) {
            p = it.next();
            if (p > 0) {
                satisfied.add(mapToDomain.get(p));
            } else {
                falsified.add(mapToDomain.get(-p));
            }
        }
    }
}
