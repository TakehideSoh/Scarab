package org.sat4j.pb;

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

public class BugSAT130 {

    private IPBSolver solver;

    @Before
    public void init() {
        solver = SolverFactory.newDefaultOptimizer();
    }

    @Test
    public void testMissFirstValue() throws ContradictionException,
            TimeoutException {
        IVecInt vars = new VecInt();
        vars.push(1).push(2).push(3);
        IVec<BigInteger> coefs = new Vec<BigInteger>(3, BigInteger.ONE);
        solver.addExactly(new VecInt(new int[] { 2, 3 }), 1);
        solver.setObjectiveFunction(new ObjectiveFunction(vars, coefs));
        IVecInt sol = new VecInt(solver.findModel());
        assertTrue(sol.contains(-1));
    }

    @Test
    public void testMissSecondValue() throws ContradictionException,
            TimeoutException {
        IVecInt vars = new VecInt();
        vars.push(1).push(2).push(3);
        IVec<BigInteger> coefs = new Vec<BigInteger>(3, BigInteger.ONE);
        solver.addExactly(new VecInt(new int[] { 1, 3 }), 1);
        solver.setObjectiveFunction(new ObjectiveFunction(vars, coefs));
        IVecInt sol = new VecInt(solver.findModel());
        assertTrue(sol.contains(-2));
    }

    @Test
    public void testMissLastValue() throws ContradictionException,
            TimeoutException {
        IVecInt vars = new VecInt();
        vars.push(1).push(2).push(3);
        IVec<BigInteger> coefs = new Vec<BigInteger>(3, BigInteger.ONE);
        solver.addExactly(new VecInt(new int[] { 1, 2 }), 1);
        solver.setObjectiveFunction(new ObjectiveFunction(vars, coefs));
        IVecInt sol = new VecInt(solver.findModel());
        assertTrue(sol.contains(-3));
    }

}
