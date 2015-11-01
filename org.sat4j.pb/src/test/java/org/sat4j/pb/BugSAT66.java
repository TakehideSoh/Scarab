package org.sat4j.pb;

import static org.junit.Assert.fail;

import java.math.BigInteger;

import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.tools.XplainPB;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.tools.xplain.Xplain;

public class BugSAT66 {

    @Test(expected = IllegalStateException.class)
    public void testMissingNewVarWithClauseInXplainPB()
            throws ContradictionException {
        Xplain<IPBSolver> solver = new XplainPB(SolverFactory.newDefault());
        IVecInt clause = new VecInt();
        clause.push(-1).push(-2);
        solver.addClause(clause);
        fail("Should not accept clauses if newvar has not been called!!!");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingNewVarWithAtLeastInXplainPB()
            throws ContradictionException {
        Xplain<IPBSolver> solver = new XplainPB(SolverFactory.newDefault());
        IVecInt clause = new VecInt();
        clause.push(-1).push(-2).push(-3);
        solver.addAtLeast(clause, 2);
        fail("Should not accept clauses if newvar has not been called!!!");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingNewVarWithAtMostInXplainPB()
            throws ContradictionException {
        Xplain<IPBSolver> solver = new XplainPB(SolverFactory.newDefault());
        IVecInt clause = new VecInt();
        clause.push(-1).push(-2).push(-3);
        solver.addAtMost(clause, 2);
        fail("Should not accept clauses if newvar has not been called!!!");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingNewVarWithExactlyInXplainPB()
            throws ContradictionException {
        Xplain<IPBSolver> solver = new XplainPB(SolverFactory.newDefault());
        IVecInt clause = new VecInt();
        clause.push(-1).push(-2).push(-3);
        solver.addExactly(clause, 2);
        fail("Should not accept clauses if newvar has not been called!!!");
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingNewVarWithPBInXplainPB()
            throws ContradictionException {
        XplainPB solver = new XplainPB(SolverFactory.newDefault());
        IVecInt clause = new VecInt();
        clause.push(-1).push(-2).push(-3);
        IVec<BigInteger> coeffs = new Vec<BigInteger>();
        coeffs.push(BigInteger.valueOf(5));
        coeffs.push(BigInteger.valueOf(15));
        coeffs.push(BigInteger.valueOf(30));
        solver.addPseudoBoolean(clause, coeffs, true, BigInteger.valueOf(10));
        fail("Should not accept clauses if newvar has not been called!!!");
    }

}
