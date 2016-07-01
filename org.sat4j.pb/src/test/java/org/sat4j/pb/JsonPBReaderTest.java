package org.sat4j.pb;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.reader.JSONPBReader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public class JsonPBReaderTest {

    private IPBSolver solver;
    private JSONPBReader reader;

    @Before
    public void setUp() throws Exception {
        solver = mock(IPBSolver.class);
        reader = new JSONPBReader(solver);
    }

    @Test
    public void testReadingSimplePseudoAtLeast() throws ParseFormatException,
            ContradictionException, IOException {
        String json = "[[[[1,1],[23,-2],[-64,3]],'>=',24]]";
        reader.parseString(json);
        IVecInt clause = new VecInt().push(1).push(-2).push(3);
        IVecInt coefs = new VecInt().push(1).push(23).push(-64);
        verify(solver).addAtLeast(clause, coefs, 24);

    }

    @Test
    public void testReadingSimplePseudoAtMost() throws ParseFormatException,
            ContradictionException, IOException {
        String json = "[[[[1,1],[23,-2],[-64,3]],'<=',24]]";
        reader.parseString(json);
        IVecInt clause = new VecInt().push(1).push(-2).push(3);
        IVecInt coefs = new VecInt().push(1).push(23).push(-64);
        verify(solver).addAtMost(clause, coefs, 24);

    }

    @Test
    public void testReadingSimplePseudoExactly() throws ParseFormatException,
            ContradictionException, IOException {
        String json = "[[[[1,1],[23,-2],[-64,3]],'=',24]]";
        reader.parseString(json);
        IVecInt clause = new VecInt().push(1).push(-2).push(3);
        IVecInt coefs = new VecInt().push(1).push(23).push(-64);
        verify(solver).addExactly(clause, coefs, 24);

    }

    @Test
    public void testReadingSimplePseudoAtLeastStrictly()
            throws ParseFormatException, ContradictionException, IOException {
        String json = "[[[[1,1],[23,-2],[-64,3]],'>',24]]";
        reader.parseString(json);
        IVecInt clause = new VecInt().push(1).push(-2).push(3);
        IVecInt coefs = new VecInt().push(1).push(23).push(-64);
        verify(solver).addAtLeast(clause, coefs, 25);

    }

    @Test
    public void testReadingSimplePseudoAtMostStrictly()
            throws ParseFormatException, ContradictionException, IOException {
        String json = "[[[[1,1],[23,-2],[-64,3]],'<',24]]";
        reader.parseString(json);
        IVecInt clause = new VecInt().push(1).push(-2).push(3);
        IVecInt coefs = new VecInt().push(1).push(23).push(-64);
        verify(solver).addAtMost(clause, coefs, 23);

    }

    @Test
    public void testOrderofMixedConstraints() throws ParseFormatException,
            ContradictionException {
        String json = "[[-1,-2,-3],[[1,-2,3],'>',2],[4,-3,6],[[[1,1],[2,2],[4,3],[8,4]],'<=',6]]";
        reader.parseString(json);
        IVecInt clause1 = new VecInt().push(-1).push(-2).push(-3);
        IVecInt card = new VecInt().push(1).push(-2).push(3);
        IVecInt clause2 = new VecInt().push(4).push(-3).push(6);
        IVecInt lits = new VecInt().push(1).push(2).push(3).push(4);
        IVecInt coefs = new VecInt().push(1).push(2).push(4).push(8);
        InOrder inOrder = inOrder(solver);
        inOrder.verify(solver).addClause(clause1);
        inOrder.verify(solver).addAtLeast(card, 3);
        inOrder.verify(solver).addClause(clause2);
        inOrder.verify(solver).addAtMost(lits, coefs, 6);
    }

    @Test
    public void testObjectiveFunctionMin() throws ParseFormatException,
            ContradictionException {
        String json = "[['min',[[1,1],[20,2],[80,3]]],[-1,-2,-3],[[1,-2,3],'>',2],[4,-3,6],[[[1,1],[2,2],[4,3],[8,4]],'<=',6]]";
        reader.parseString(json);
        IVecInt objvars = new VecInt().push(1).push(2).push(3);
        IVec<BigInteger> objcoefs = new Vec<BigInteger>()
                .push(BigInteger.valueOf(1)).push(BigInteger.valueOf(20))
                .push(BigInteger.valueOf(80));
        ObjectiveFunction obj = new ObjectiveFunction(objvars, objcoefs);
        IVecInt clause1 = new VecInt().push(-1).push(-2).push(-3);
        IVecInt card = new VecInt().push(1).push(-2).push(3);
        IVecInt clause2 = new VecInt().push(4).push(-3).push(6);
        IVecInt lits = new VecInt().push(1).push(2).push(3).push(4);
        IVecInt coefs = new VecInt().push(1).push(2).push(4).push(8);
        InOrder inOrder = inOrder(solver);
        inOrder.verify(solver).setObjectiveFunction(obj);
        inOrder.verify(solver).addClause(clause1);
        inOrder.verify(solver).addAtLeast(card, 3);
        inOrder.verify(solver).addClause(clause2);
        inOrder.verify(solver).addAtMost(lits, coefs, 6);
    }

    @Test
    public void testObjectiveFunctionMax() throws ParseFormatException,
            ContradictionException {
        String json = "[['max',[[1,1],[20,2],[80,3]]],[-1,-2,-3],[[1,-2,3],'>',2],[4,-3,6],[[[1,1],[2,2],[4,3],[8,4]],'<=',6]]";
        reader.parseString(json);
        IVecInt objvars = new VecInt().push(1).push(2).push(3);
        IVec<BigInteger> objcoefs = new Vec<BigInteger>()
                .push(new BigInteger("-1")).push(new BigInteger("-20"))
                .push(new BigInteger("-80"));
        ObjectiveFunction obj = new ObjectiveFunction(objvars, objcoefs);
        IVecInt clause1 = new VecInt().push(-1).push(-2).push(-3);
        IVecInt card = new VecInt().push(1).push(-2).push(3);
        IVecInt clause2 = new VecInt().push(4).push(-3).push(6);
        IVecInt lits = new VecInt().push(1).push(2).push(3).push(4);
        IVecInt coefs = new VecInt().push(1).push(2).push(4).push(8);
        InOrder inOrder = inOrder(solver);
        inOrder.verify(solver).setObjectiveFunction(obj);
        inOrder.verify(solver).addClause(clause1);
        inOrder.verify(solver).addAtLeast(card, 3);
        inOrder.verify(solver).addClause(clause2);
        inOrder.verify(solver).addAtMost(lits, coefs, 6);
    }

}
