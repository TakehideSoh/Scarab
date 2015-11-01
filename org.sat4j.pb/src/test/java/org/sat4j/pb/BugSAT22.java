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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.TimeoutException;

public class BugSAT22 {
    @Test
    public void testSimpleResolverUnitFirst() throws ContradictionException,
            TimeoutException {
        IPBSolver solver = SolverFactory.newEclipseP2();
        DependencyHelper<Named, String> helper = new DependencyHelper<Named, String>(
                solver, false);
        Set<Named> slice = new HashSet<Named>();
        Named A1 = new Named("A1");
        slice.add(A1);
        Named A2 = new Named("A2");
        slice.add(A2);
        Named B = new Named("B");
        slice.add(B);
        Named X = new Named("X");
        // base
        helper.setTrue(X, "Build");
        // objective function
        helper.addToObjectiveFunction(A2, 1);
        helper.addToObjectiveFunction(A1, 2);
        helper.addToObjectiveFunction(B, 1);
        // depends
        helper.or("a", X, new Named[] { A1, A2 });
        helper.or("b", X, new Named[] { B });
        // solve
        assertTrue(helper.hasASolution());
        IVec<Named> solution = helper.getSolution();
        assertEquals(3, solution.size());
        assertTrue(solution.contains(B));
        assertTrue(solution.contains(X));
        assertTrue(solution.contains(A2));
    }

    @Test
    public void testSimpleResolverUnitLast() throws ContradictionException,
            TimeoutException {
        IPBSolver solver = SolverFactory.newEclipseP2();
        DependencyHelper<Named, String> helper = new DependencyHelper<Named, String>(
                solver, false);
        Set<Named> slice = new HashSet<Named>();
        Named A1 = new Named("A1");
        slice.add(A1);
        Named A2 = new Named("A2");
        slice.add(A2);
        Named B = new Named("B");
        slice.add(B);
        Named X = new Named("X");
        // objective function
        helper.addToObjectiveFunction(A2, 1);
        helper.addToObjectiveFunction(A1, 2);
        helper.addToObjectiveFunction(B, 1);
        // depends
        helper.or("a", X, new Named[] { A1, A2 });
        helper.or("b", X, new Named[] { B });
        // base
        helper.setTrue(X, "Build");
        // solve
        assertTrue(helper.hasASolution());
        IVec<Named> solution = helper.getSolution();
        assertEquals(3, solution.size());
        assertTrue(solution.contains(B));
        assertTrue(solution.contains(X));
        assertTrue(solution.contains(A2));
    }

    public class Named {
        public String name;

        public Named(String name) {
            this.name = name;
        }
    }
}
