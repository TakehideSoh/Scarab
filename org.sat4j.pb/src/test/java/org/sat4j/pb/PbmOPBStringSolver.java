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

import java.math.BigInteger;

import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public class PbmOPBStringSolver {

    private static final String STRING1 = "* #variable= 3 #constraint= 1\n+1 x1 +1 x2 +1 x3 >= 1 ;\n";
    private static final String STRING2 = "* #variable= 3 #constraint= 1\n"
            + "min: +10 x2 +32 x3 ;\n+1 x1 +1 x2 +1 x3 >= 1 ;\n";

    @Test
    public void testNoMin() throws ContradictionException {
        IPBSolver solver = new OPBStringSolver();
        solver.newVar(3);
        IVecInt clause = new VecInt();
        clause.push(1).push(2).push(3);
        solver.addClause(clause);
        assertEquals(STRING1, solver.toString());
    }

    @Test
    public void testWithMin() throws ContradictionException {
        IPBSolver solver = new OPBStringSolver();
        solver.newVar(3);
        IVecInt clause = new VecInt();
        clause.push(1).push(2).push(3);
        solver.addClause(clause);
        IVecInt vars = new VecInt();
        vars.push(2).push(3);
        IVec<BigInteger> coeffs = new Vec<BigInteger>();
        coeffs.push(BigInteger.TEN).push(BigInteger.valueOf(32));
        ObjectiveFunction obj = new ObjectiveFunction(vars, coeffs);
        solver.setObjectiveFunction(obj);
        assertEquals(STRING2, solver.toString());

    }
}
