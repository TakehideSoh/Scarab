package org.sat4j.pb.multiobjective;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IIntegerPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.core.IntegerPBSolverDecorator;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class OS3OWAOptimizerTest {

    private OrderedObjsOWAOptimizer optimizer;

    @Before
    public void setUp() {
        IIntegerPBSolver integerSolver = new IntegerPBSolverDecorator(
                SolverFactory.newDefault());
        BigInteger weights[] = new BigInteger[3];
        weights[0] = BigInteger.valueOf(1);
        weights[1] = BigInteger.valueOf(2);
        weights[2] = BigInteger.valueOf(3);
        optimizer = new OrderedObjsOWAOptimizer(integerSolver, weights);
    }

    @Test
    public void test1() {
        try {
            optimizer.newVar(7);
            optimizer.addClause(new VecInt(new int[] { 4, 5, 7 }));
            optimizer.addClause(new VecInt(new int[] { 3, 6 }));
            optimizer.addClause(new VecInt(new int[] { 1, 2 }));
            optimizer.addClause(new VecInt(new int[] { -4, -3, 1, 2 }));
            optimizer.addClause(new VecInt(new int[] { -7, 3 }));
            optimizer.addClause(new VecInt(new int[] { -7, 2 }));
            optimizer.addClause(new VecInt(new int[] { -6, 4 }));
            optimizer.addClause(new VecInt(new int[] { -6, 1 }));
            optimizer.addClause(new VecInt(new int[] { -5, 3 }));
            optimizer.addClause(new VecInt(new int[] { -5, 1 }));
            optimizer.addObjectiveFunction(new ObjectiveFunction(new VecInt(
                    new int[] { 4, 5, 7 }), new Vec<BigInteger>(
                    new BigInteger[] { BigInteger.valueOf(9),
                            BigInteger.valueOf(8), BigInteger.valueOf(10) })));
            optimizer.addObjectiveFunction(new ObjectiveFunction(new VecInt(
                    new int[] { 3, 6 }), new Vec<BigInteger>(new BigInteger[] {
                    BigInteger.valueOf(10), BigInteger.valueOf(9) })));
            optimizer.addObjectiveFunction(new ObjectiveFunction(new VecInt(
                    new int[] { 1, 2 }), new Vec<BigInteger>(new BigInteger[] {
                    BigInteger.valueOf(12), BigInteger.valueOf(11) })));
        } catch (ContradictionException e) {
            fail(e.getMessage());
        }
        try {
            if (optimizer.isSatisfiable()) {
                int[] expectedModel = new int[] { -1, 2, 3, 4, -5, -6, -7 };
                int[] actualModel = optimizer.model();
                for (int i = 0; i < expectedModel.length; ++i) {
                    assertEquals(expectedModel[i], actualModel[i]);
                }
                int[] expectedValues = new int[] { 9, 10, 11 };
                BigInteger[] actualValues = optimizer.getObjectiveValues();
                for (int i = 0; i < expectedValues.length; ++i) {
                    assertEquals(BigInteger.valueOf(expectedValues[i]),
                            actualValues[i]);
                }
            }
        } catch (TimeoutException e) {
            fail(e.getMessage());
        }
    }

}
