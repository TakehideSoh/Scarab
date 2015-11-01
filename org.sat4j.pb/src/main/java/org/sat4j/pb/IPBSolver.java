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

import java.math.BigInteger;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

/**
 * A solver able to deal with pseudo boolean constraints.
 * 
 * @author daniel
 * 
 */
public interface IPBSolver extends ISolver {

	/**
	 * Create a Pseudo-Boolean constraint of the type "at least n or at most n
	 * of those literals must be satisfied"
	 * 
	 * @param lits
	 *            a set of literals. The vector can be reused since the solver
	 *            is not supposed to keep a reference to that vector.
	 * @param coeffs
	 *            the coefficients of the literals. The vector can be reused
	 *            since the solver is not supposed to keep a reference to that
	 *            vector.
	 * @param moreThan
	 *            true if it is a constraint >= degree, false if it is a
	 *            constraint <= degree
	 * @param d
	 *            the degree of the cardinality constraint
	 * @return a reference to the constraint added in the solver, to use in
	 *         removeConstr().
	 * @throws ContradictionException
	 *             iff the vector of literals is empty or if the constraint is
	 *             falsified after unit propagation
	 * @see #removeConstr(IConstr)
	 */
	IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
			boolean moreThan, BigInteger d) throws ContradictionException;

	/**
	 * Create a pseudo boolean constraint of the type "at most".
	 * 
	 * @param literals
	 *            a set of literals The vector can be reused since the solver is
	 *            not supposed to keep a reference to that vector.
	 * @param coeffs
	 *            the coefficients of the literals. The vector can be reused
	 *            since the solver is not supposed to keep a reference to that
	 *            vector.
	 * @param degree
	 *            the degree of the pseudo-boolean constraint
	 * @return a reference to the constraint added in the solver, to use in
	 *         removeConstr().
	 * @throws ContradictionException
	 *             iff the constraint is found trivially unsat.
	 * @see #removeConstr(IConstr)
	 * @since 2.3.1
	 */

	IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
			throws ContradictionException;

	/**
	 * Create a pseudo boolean constraint of the type "at most".
	 * 
	 * @param literals
	 *            a set of literals The vector can be reused since the solver is
	 *            not supposed to keep a reference to that vector.
	 * @param coeffs
	 *            the coefficients of the literals. The vector can be reused
	 *            since the solver is not supposed to keep a reference to that
	 *            vector.
	 * @param degree
	 *            the degree of the pseudo-boolean constraint
	 * @return a reference to the constraint added in the solver, to use in
	 *         removeConstr().
	 * @throws ContradictionException
	 *             iff the constraint is found trivially unsat.
	 * @see #removeConstr(IConstr)
	 * @since 2.3.1
	 */

	IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
			BigInteger degree) throws ContradictionException;

	/**
	 * Create a pseudo-boolean constraint of the type "at least".
	 * 
	 * @param literals
	 *            a set of literals. The vector can be reused since the solver
	 *            is not supposed to keep a reference to that vector.
	 * @param coeffs
	 *            the coefficients of the literals. The vector can be reused
	 *            since the solver is not supposed to keep a reference to that
	 *            vector.
	 * @param degree
	 *            the degree of the pseudo-boolean constraint
	 * @return a reference to the constraint added in the solver, to use in
	 *         removeConstr().
	 * @throws ContradictionException
	 *             iff the constraint is found trivially unsat.
	 * @see #removeConstr(IConstr)
	 * @since 2.3.1
	 */
	IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
			throws ContradictionException;

	/**
	 * Create a pseudo-boolean constraint of the type "at least".
	 * 
	 * @param literals
	 *            a set of literals. The vector can be reused since the solver
	 *            is not supposed to keep a reference to that vector.
	 * @param coeffs
	 *            the coefficients of the literals. The vector can be reused
	 *            since the solver is not supposed to keep a reference to that
	 *            vector.
	 * @param degree
	 *            the degree of the pseudo-boolean constraint
	 * @return a reference to the constraint added in the solver, to use in
	 *         removeConstr().
	 * @throws ContradictionException
	 *             iff the constraint is found trivially unsat.
	 * @see #removeConstr(IConstr)
	 * @since 2.3.1
	 */
	IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
			BigInteger degree) throws ContradictionException;

	/**
	 * Create a pseudo-boolean constraint of the type "subset sum".
	 * 
	 * @param literals
	 *            a set of literals. The vector can be reused since the solver
	 *            is not supposed to keep a reference to that vector.
	 * @param coeffs
	 *            the coefficients of the literals. The vector can be reused
	 *            since the solver is not supposed to keep a reference to that
	 *            vector.
	 * @param weight
	 *            the number of literals that must be satisfied
	 * @return a reference to the constraint added to the solver. It might
	 *         return an object representing a group of constraints.
	 * @throws ContradictionException
	 *             iff the constraint is trivially unsatisfiable.
	 * @since 2.3.1
	 */
	IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
			throws ContradictionException;

	/**
	 * Create a pseudo-boolean constraint of the type "subset sum".
	 * 
	 * @param literals
	 *            a set of literals. The vector can be reused since the solver
	 *            is not supposed to keep a reference to that vector.
	 * @param coeffs
	 *            the coefficients of the literals. The vector can be reused
	 *            since the solver is not supposed to keep a reference to that
	 *            vector.
	 * @param weight
	 *            the number of literals that must be satisfied
	 * @return a reference to the constraint added to the solver. It might
	 *         return an object representing a group of constraints.
	 * @throws ContradictionException
	 *             iff the constraint is trivially unsatisfiable.
	 * @since 2.3.1
	 */
	IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
			BigInteger weight) throws ContradictionException;

	/**
	 * Provide an objective function to the solver.
	 * 
	 * @param obj
	 *            the objective function
	 */
	void setObjectiveFunction(ObjectiveFunction obj);

	/**
	 * Retrieve the objective function from the solver.
	 * 
	 * @return the objective function
	 */
	ObjectiveFunction getObjectiveFunction();
}
