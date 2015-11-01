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

import java.math.BigInteger;

import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.RemiUtils;

public class BugSAT16 {

    @Test
    public void testCNFCase() throws ContradictionException, TimeoutException {

        IPBSolver solver = SolverFactory.newDefault();

        // +1 x6 >= 1 [+1 x6 >= 1]
        solver.addClause(transform1(new int[] { 6 }));

        // +1 x6 +1 ~x5 >= 1 [+1 x6 -1 x5 >= 0]
        solver.addClause(transform1(new int[] { 6, -5 }));

        // +1 ~x6 +1 x5 >= 1 [-1 x6 +1 x5 >= 0]
        solver.addClause(transform1(new int[] { -6, 5 }));

        // +1 x6 +1 ~x4 >= 1 [+1 x6 -1 x4 >= 0]
        solver.addClause(transform1(new int[] { 6, -4 }));

        // +1 ~x6 +1 x4 >= 1 [-1 x6 +1 x4 >= 0]
        solver.addClause(transform1(new int[] { -6, 4 }));

        // +1 x4 +1 ~x2 +1 ~x1 >= 2 [+1 x4 -1 x2 -1 x1 >= 0]
        solver.addClause(transform1(new int[] { 4, -2, -1 }));

        // +1 ~x4 +1 x2 +1 x1 >= 1 [-1 x4 +1 x2 +1 x1 >= 0]
        solver.addClause(transform1(new int[] { -4, 2, 1 }));

        IVecInt backbone = RemiUtils.backbone(solver);
        assertEquals(3, backbone.size());
        assertTrue(backbone.contains(6));
        assertTrue(backbone.contains(5));
        assertTrue(backbone.contains(4));
    }

    @Test
    public void testPBCase() throws ContradictionException, TimeoutException {

        IPBSolver solver = SolverFactory.newDefault();

        // +1 x6 >= 1 [+1 x6 >= 1]
        solver.addPseudoBoolean(transform1(new int[] { 6 }),
                transform2(new int[] { 1 }), true, BigInteger.valueOf(1));

        // +1 x6 +1 ~x5 >= 1 [+1 x6 -1 x5 >= 0]
        solver.addPseudoBoolean(transform1(new int[] { 6, -5 }),
                transform2(new int[] { 1, 1 }), true, BigInteger.valueOf(1));

        // +1 ~x6 +1 x5 >= 1 [-1 x6 +1 x5 >= 0]
        solver.addPseudoBoolean(transform1(new int[] { -6, 5 }),
                transform2(new int[] { 1, 1 }), true, BigInteger.valueOf(1));

        // +1 x6 +1 ~x4 >= 1 [+1 x6 -1 x4 >= 0]
        solver.addPseudoBoolean(transform1(new int[] { 6, -4 }),
                transform2(new int[] { 1, 1 }), true, BigInteger.valueOf(1));

        // +1 ~x6 +1 x4 >= 1 [-1 x6 +1 x4 >= 0]
        solver.addPseudoBoolean(transform1(new int[] { -6, 4 }),
                transform2(new int[] { 1, 1 }), true, BigInteger.valueOf(1));

        // +1 x4 +1 ~x2 +1 ~x1 >= 2 [+1 x4 -1 x2 -1 x1 >= 0]
        solver.addPseudoBoolean(transform1(new int[] { 4, -2, -1 }),
                transform2(new int[] { 1, 1, 1 }), true, BigInteger.valueOf(2));

        // +1 ~x4 +1 x2 +1 x1 >= 1 [-1 x4 +1 x2 +1 x1 >= 0]
        solver.addPseudoBoolean(transform1(new int[] { -4, 2, 1 }),
                transform2(new int[] { 1, 1, 1 }), true, BigInteger.valueOf(1));

        IVecInt backbone = RemiUtils.backbone(solver);
        assertEquals(3, backbone.size());
        assertTrue(backbone.contains(6));
        assertTrue(backbone.contains(5));
        assertTrue(backbone.contains(4));
    }

    static IVecInt transform1(int[] intArray) {
        return new VecInt(intArray);
    }

    static IVec<BigInteger> transform2(int[] intArray) {
        BigInteger[] result = new BigInteger[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            result[i] = BigInteger.valueOf(intArray[i]);
        }
        return new Vec<BigInteger>(result);
    }
}
