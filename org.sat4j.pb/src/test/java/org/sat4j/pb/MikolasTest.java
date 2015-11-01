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

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

public class MikolasTest {
    static boolean solve(PseudoOptDecorator optproblem) throws TimeoutException {
        boolean isSatisfiable = false;

        try {
            while (optproblem.admitABetterSolution()) {
                if (!isSatisfiable) {
                    if (optproblem.nonOptimalMeansSatisfiable()) {
                        if (optproblem.hasNoObjectiveFunction()) {
                            return true;
                        }
                    }
                    isSatisfiable = true;
                }
                optproblem.discardCurrentSolution();
            }
        } catch (ContradictionException ex) {
            assertTrue(isSatisfiable);
        }
        return isSatisfiable;
    }

    public static IVecInt vector(int... ls) {
        return new VecInt(ls);
    }

    public static IVec<BigInteger> constant_vector(final BigInteger value,
            int sz) {
        IVec<BigInteger> ones = new Vec<BigInteger>(sz);
        for (int index = 0; index < sz; ++index) {
            ones.push(value);
        }
        return ones;
    }

    public static void print_model(IProblem solver) {
        for (int index = 1; index <= solver.nVars(); ++index) {
            System.err.print((solver.model(index) ? "+" : "-") + index + " ");
        }
    }

    @Test
    public void testLexicoOptimizationWithPseudoOptDecorator()
            throws ContradictionException, TimeoutException {
        PseudoOptDecorator solver = new PseudoOptDecorator(
                org.sat4j.pb.SolverFactory.newDefault());
        solver.newVar(8);
        solver.setObjectiveFunction(new ObjectiveFunction(vector(6, 7, 8),
                constant_vector(BigInteger.valueOf(-1), 3)));
        System.err.println(solver.getObjectiveFunction());

        solver.addClause(vector(-3, 6));
        solver.addClause(vector(-1, -6));
        solver.addClause(vector(-4, 7));
        solver.addClause(vector(1, -7));
        solver.addClause(vector(-5, 8));
        solver.addClause(vector(-2, -8));

        // only this, crashes
        solver.addClause(vector(1, 2, 3));
        assertTrue(solve(solver));
        print_model(solver);
        System.err.println();
        solver.addClause(vector(1));
        assertTrue(solve(solver));
    }

}
