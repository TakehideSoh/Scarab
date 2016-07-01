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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;

import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

public class TestLonca {

    @Test
    public void testIteratingWithNoObjectiveFunction() {
        IPBSolver solver = buildSolver1();

        IProblem problem = solver;
        int nbModel = 0;
        try {
            while (problem.isSatisfiable()) {
                int[] mod = problem.model();
                solver.addBlockingClause(new VecInt(invert(mod)));
                nbModel++;
            }
        } catch (TimeoutException e) {
            fail();
        } catch (ContradictionException e) {
            fail();
        }
        assertEquals(4, nbModel);
    }

    @Test
    public void testIteratingWithObjectiveFunctionCard() {
        IPBSolver solver = buildSolver2();
        IProblem problem = solver;
        int nbModel = 0;
        try {
            while (problem.isSatisfiable()) {
                int[] mod = problem.model();
                solver.addBlockingClause(new VecInt(invert(mod)));
                nbModel++;
            }
        } catch (TimeoutException e) {
            fail();
        } catch (ContradictionException e) {
            fail();
        }
        assertEquals(4, nbModel);
    }

    @Test
    public void testIteratingWithObjectiveFunctionPseudo() {
        IPBSolver solver = buildSolver3();
        IProblem problem = solver;
        int nbModel = 0;
        try {
            while (problem.isSatisfiable()) {
                int[] mod = problem.model();
                solver.addBlockingClause(new VecInt(invert(mod)));
                nbModel++;
            }
        } catch (TimeoutException e) {
            fail();
        } catch (ContradictionException e) {
            fail();
        }
        assertEquals(4, nbModel);
    }

    @Test
    public void testIteratingWithObjectiveFunctionWithDecorator() {
        IPBSolver solver = buildSolver2();

        IProblem problem = new ModelIterator(solver);
        int nbModel = 0;
        try {
            while (problem.isSatisfiable()) {
                problem.model(); // needed to discard that model
                nbModel++;
            }
        } catch (TimeoutException e) {
            fail();
        }
        assertEquals(4, nbModel);
    }

    private static int[] invert(int[] mod) {
        int[] res = new int[mod.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = -mod[i];
        }
        return res;
    }

    private static IPBSolver buildSolver1() {
        IPBSolver solver = new OptToPBSATAdapter(new PseudoOptDecorator(
                SolverFactory.newResolution()));

        try {
            solver.addClause(new VecInt(new int[] { 1, 2, 3 }));
            solver.addClause(new VecInt(new int[] { -1, -2 }));
            solver.addClause(new VecInt(new int[] { -2, -3 }));
        } catch (ContradictionException e) {
            fail();
        }
        return solver;
    }

    private static IPBSolver buildSolver2() {
        IPBSolver solver = buildSolver1();
        IVecInt vars = new VecInt(new int[] { 1, 2, 3 });
        IVec<BigInteger> coeffs = new Vec<BigInteger>(new BigInteger[] {
                BigInteger.valueOf(1), BigInteger.valueOf(1),
                BigInteger.valueOf(1) });
        ObjectiveFunction func = new ObjectiveFunction(vars, coeffs);
        solver.setObjectiveFunction(func);
        return solver;
    }

    private static IPBSolver buildSolver3() {
        IPBSolver solver = buildSolver1();
        IVecInt vars = new VecInt(new int[] { 1, 2, 3 });
        IVec<BigInteger> coeffs = new Vec<BigInteger>(new BigInteger[] {
                BigInteger.valueOf(8), BigInteger.valueOf(4),
                BigInteger.valueOf(2) });
        ObjectiveFunction func = new ObjectiveFunction(vars, coeffs);

        solver.setObjectiveFunction(func);
        return solver;
    }

    @Test
    public void testRemovalOfConstraintsPropagatingLiterals()
            throws ContradictionException, TimeoutException {
        IPBSolver solver = buildSolver1();
        IVecInt literals = new VecInt(new int[] { 4, 5, 6, 7 });
        IVecInt coeffs = new VecInt(new int[] { 12, 10, 8, 6 });
        IConstr c1 = solver.addAtMost(literals, coeffs, 8);
        IConstr c2 = solver.addAtMost(literals, coeffs, 6);
        assertTrue(solver.isSatisfiable());
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 4 })));
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 5 })));
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 6 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 7 })));
        solver.removeConstr(c2);
        assertTrue(solver.isSatisfiable());
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 4 })));
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 5 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 6 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 7 })));
        solver.removeConstr(c1);
        assertTrue(solver.isSatisfiable());
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 4 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 5 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 6 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 7 })));

    }

    @Test
    public void testRemovalOfConstraintsPropagatingLiteralsBis()
            throws ContradictionException, TimeoutException {
        IPBSolver solver = buildSolver1();
        IVecInt literals = new VecInt(new int[] { 4, 5, 6, 7 });
        IVecInt coeffs = new VecInt(new int[] { 12, 10, 8, 6 });
        IConstr c1 = solver.addAtMost(literals, coeffs, 6);
        IConstr c2 = solver.addAtMost(literals, coeffs, 8);
        assertTrue(solver.isSatisfiable());
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 4 })));
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 5 })));
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 6 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 7 })));
        solver.removeConstr(c2);
        assertTrue(solver.isSatisfiable());
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 4 })));
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 5 })));
        assertFalse(solver.isSatisfiable(new VecInt(new int[] { 6 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 7 })));
        solver.removeConstr(c1);
        assertTrue(solver.isSatisfiable());
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 4 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 5 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 6 })));
        assertTrue(solver.isSatisfiable(new VecInt(new int[] { 7 })));

    }
}
