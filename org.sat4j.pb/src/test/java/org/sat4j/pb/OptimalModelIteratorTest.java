package org.sat4j.pb;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.tools.OptimalModelIterator;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIteratorToSATAdapter;
import org.sat4j.tools.SolutionFoundListener;

public class OptimalModelIteratorTest {

    private IPBSolver solver;

    @Before
    public void setUp() throws Exception {
        solver = SolverFactory.newDefault();
    }

    @Test
    public void testSimple() throws ContradictionException, TimeoutException {
        IVecInt clause = new VecInt();
        clause.push(1).push(2).push(3);
        solver.addClause(clause);
        clause.clear();
        clause.push(2).push(4).push(5);
        solver.addClause(clause);
        clause.clear();
        clause.push(6).push(7).push(8);
        solver.addClause(clause);

        IVecInt vars = new VecInt();
        clause.clear();
        vars.push(1).push(2).push(3).push(4).push(5).push(6).push(7).push(8);

        IVec<BigInteger> coeffs = new Vec<BigInteger>();
        coeffs.push(BigInteger.ONE).push(BigInteger.ONE).push(BigInteger.ONE)
                .push(BigInteger.ONE).push(BigInteger.ONE).push(BigInteger.ONE)
                .push(BigInteger.ONE).push(BigInteger.ONE);

        solver.setObjectiveFunction(new ObjectiveFunction(vars, coeffs));

        SolutionFoundListener slf = new SolutionFoundListener() {

            private int nbSolutions = 0;

            public void onUnsatTermination() {
                // TODO Auto-generated method stub

            }

            public void onSolutionFound(IVecInt solution) {
                nbSolutions++;
                System.out.println(nbSolutions);
            }

            public void onSolutionFound(int[] solution) {
                nbSolutions++;
                System.out.println(nbSolutions);
            }
        };

        ISolver decore = new ModelIteratorToSATAdapter(
                new OptimalModelIterator(new OptToPBSATAdapter(
                        new PseudoOptDecorator(solver))), slf);

        decore.isSatisfiable();

    }

}
