package org.sat4j.pb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

public class BugSAT107 {

    private OptToPBSATAdapter solver;

    @Before
    public void setUp() throws ContradictionException {
        solver = new OptToPBSATAdapter(new PseudoOptDecorator(
                SolverFactory.newDefault()));
        solver.newVar(100);
        IVecInt clause = new VecInt();
        for (int i = 2; i < 100; i += 2) {
            clause.push(-1).push(i);
            solver.addClause(clause);
            clause.clear();
            clause.push(-i).push(i + 1);
            solver.addClause(clause);
            clause.clear();
            clause.push(-i - 1).push(i);
            solver.addClause(clause);
        }
        clause.clear();
        IVec<BigInteger> coefs = new Vec<BigInteger>();
        for (int i = 1; i <= 10; i++) {
            clause.push(i);
        }
        solver.addClause(clause);
        clause.clear();
        for (int i = 20; i <= 40; i++) {
            clause.push(i);
        }
        solver.addClause(clause);
        clause.clear();
        for (int i = 1; i <= 100; i++) {
            clause.push(i);
            coefs.push(BigInteger.ONE);
        }
        solver.setObjectiveFunction(new ObjectiveFunction(clause, coefs));

    }

    @Test
    public void testOptimalSolutionfound() throws ContradictionException,
            TimeoutException {
        solver.setTimeoutOnConflicts(100);
        assertTrue(solver.isSatisfiable());
        assertTrue(solver.isOptimal());
    }

    @Test
    public void testNonOptimalSolutionfound() throws ContradictionException,
            TimeoutException {
        solver.setTimeoutOnConflicts(3);
        assertTrue(solver.isSatisfiable());
        assertFalse(solver.isOptimal());
    }

    @Test(expected = TimeoutException.class)
    public void testNoSolutionfound() throws TimeoutException {
        solver.setTimeoutOnConflicts(1);
        solver.isSatisfiable();
    }

    @Test
    public void testNoSolutionExists() throws ContradictionException,
            TimeoutException {
        IVecInt clause = new VecInt();
        clause.push(100).push(99);
        solver.addClause(clause);
        clause.clear();
        clause.push(-99);
        solver.addClause(clause);
        clause.clear();
        clause.push(-100);
        solver.addClause(clause);
        clause.clear();
        assertFalse(solver.isSatisfiable());
    }
}
