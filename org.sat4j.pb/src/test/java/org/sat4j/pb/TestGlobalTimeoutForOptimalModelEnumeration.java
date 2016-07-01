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

import org.junit.Before;
import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

public class TestGlobalTimeoutForOptimalModelEnumeration {

    private ISolver solver;

    @Before
    public void setUp() throws ContradictionException {
        PseudoOptDecorator pbsolver = new PseudoOptDecorator(
                SolverFactory.newDefault());
        IVecInt clause = new VecInt();
        pbsolver.newVar(1000);
        for (int i = 1; i <= 1000; i++) {
            clause.push(-i);
        }
        pbsolver.addClause(clause);
        Vec<BigInteger> weights = new Vec<BigInteger>();
        for (int i = 1; i <= 1000; i++) {
            weights.push(BigInteger.valueOf(5));
        }
        pbsolver.setObjectiveFunction(new ObjectiveFunction(clause, weights));
        this.solver = new ModelIterator(pbsolver);
    }

    @Test(expected = TimeoutException.class, timeout = 10000)
    public void testTimeoutOnSeconds() throws TimeoutException {
        this.solver.setTimeout(2);
        while (this.solver.isSatisfiable()) {
            this.solver.model(); // needed to
            // discard
            // that
            // solution
        }
    }

    // this test is no longer possible because the solver hardly conflicts.
    public void testTimeoutOnConflicts() throws TimeoutException {
        this.solver.setTimeoutOnConflicts(1000);
        while (this.solver.isSatisfiable()) {
            this.solver.model(); // needed to discard that solution
        }
    }
}
