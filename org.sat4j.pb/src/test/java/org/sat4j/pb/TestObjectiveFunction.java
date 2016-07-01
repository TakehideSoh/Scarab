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

import org.junit.Before;
import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.TimeoutException;

public class TestObjectiveFunction {

    private DependencyHelper<String, String> helper;

    @Before
    public void setUp() {
        this.helper = new DependencyHelper<String, String>(
                SolverFactory.newEclipseP2());
    }

    @Test
    public void testObjectiveFunctionWithAllWeightsToNull()
            throws ContradictionException, TimeoutException {
        this.helper.clause("C1", "A1", "A2");
        this.helper.clause("C2", "A1", "A3");
        this.helper.addToObjectiveFunction("A2", 0);
        this.helper.addToObjectiveFunction("A3", 0);
        IVec<String> assump = new Vec<String>();
        assump.push("A1");
        assertTrue(this.helper.hasASolution(assump));
        IVec<String> solution = this.helper.getSolution();
        assertEquals(1, solution.size());
        assertTrue(solution.contains("A1"));
    }
}
