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

import org.junit.Test;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

/**
 * @author sroussel
 * 
 */
public class BugSAT21 {

    @Test
    public void testAtLeastWithNegativeLiteralsAsText()
            throws ContradictionException {

        IPBSolver pbSolver = new OPBStringSolver();
        pbSolver.newVar(2);
        pbSolver.setExpectedNumberOfClauses(1);

        int[] constr = { -1, 2 };

        pbSolver.addAtLeast(new VecInt(constr), 1);
        String expected = "* #variable= 2 #constraint= 1\n-1 x1 +1 x2 >= 0 ;\n";
        assertEquals(expected, pbSolver.toString());

    }

    @Test
    public void testAtLeastWithNegativeLiterals()
            throws ContradictionException, TimeoutException {

        IPBSolver pbSolver = SolverFactory.newDefault();
        pbSolver.newVar(2);
        pbSolver.setExpectedNumberOfClauses(1);

        int[] constr = { -1, 2 };

        pbSolver.addAtLeast(new VecInt(constr), 1);
        IVecInt assumps = new VecInt();
        assumps.push(1).push(-2);
        assertFalse(pbSolver.isSatisfiable(assumps));
        assumps.clear();
        assumps.push(-1).push(-2);
        assertTrue(pbSolver.isSatisfiable(assumps));
        assumps.clear();
        assumps.push(1).push(2);
        assertTrue(pbSolver.isSatisfiable(assumps));
        assumps.clear();
        assumps.push(-1).push(2);
        assertTrue(pbSolver.isSatisfiable(assumps));

    }

    @Test
    public void testAlMostWithNegativeLiteralsAsText()
            throws ContradictionException {

        IPBSolver pbSolver = new OPBStringSolver();
        pbSolver.newVar(2);
        pbSolver.setExpectedNumberOfClauses(1);

        int[] constr = { -1, 2 };

        pbSolver.addAtMost(new VecInt(constr), 1);
        String expected = "* #variable= 2 #constraint= 1\n+1 x1 -1 x2 >= 0 ;\n";
        assertEquals(expected, pbSolver.toString());

    }

    @Test
    public void testAtMostWithNegativeLiterals() throws ContradictionException,
            TimeoutException {

        IPBSolver pbSolver = SolverFactory.newDefault();
        pbSolver.newVar(2);
        pbSolver.setExpectedNumberOfClauses(1);

        int[] constr = { -1, 2 };

        pbSolver.addAtMost(new VecInt(constr), 1);
        IVecInt assumps = new VecInt();
        assumps.push(1).push(-2);
        assertTrue(pbSolver.isSatisfiable(assumps));
        assumps.clear();
        assumps.push(-1).push(-2);
        assertTrue(pbSolver.isSatisfiable(assumps));
        assumps.clear();
        assumps.push(1).push(2);
        assertTrue(pbSolver.isSatisfiable(assumps));
        assumps.clear();
        assumps.push(-1).push(2);
        assertFalse(pbSolver.isSatisfiable(assumps));

    }

}
