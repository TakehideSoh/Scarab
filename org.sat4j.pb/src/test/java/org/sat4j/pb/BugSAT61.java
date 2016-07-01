package org.sat4j.pb;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.junit.Test;
import org.sat4j.pb.reader.OPBReader2010;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class BugSAT61 {

    private static final String DUPLICATED_CONJUNCTS = "* #variable= 11 #constraint= 1\n"
            + "-1 x1 -2 x9 x4 -3 x10 x4 -4 x11 x4 +5 x8 x4 -6 x9 x4 -7 x10 x4 -8 x11 x4 +9 x8 x4 -10 x9 x4 -11 x10 x4 -12 x11 x4 +13 x8 x4 -14 x9 x4 -15 x10 x4 -16 x11 x4 +17 x8 x4 -18 x9 x4 -19 x10 x4 -20 x11 x4 +21 x8 x4 = 9;\n";

    private static final String NORMALIZED_CONJUNCTS = "* #variable= 11 #constraint= 1\n"
            + "-1 x1 -50 x9 x4 -55 x10 x4 -60 x11 x4 +65 x8 x4 = 9;";

    @Test
    public void testDuplicatedConjuncts() throws ParseFormatException,
            ContradictionException, TimeoutException {
        IPBSolver solver = SolverFactory.newDefaultNonNormalized();
        OPBReader2010 reader = new OPBReader2010(solver);
        reader.parseInstance(new StringReader(DUPLICATED_CONJUNCTS));
        assertTrue(solver.isSatisfiable());
    }

    @Test
    public void testNoDuplicatedConjuncts() throws ParseFormatException,
            ContradictionException, TimeoutException {
        IPBSolver solver = SolverFactory.newDefaultNonNormalized();
        OPBReader2010 reader = new OPBReader2010(solver);
        reader.parseInstance(new StringReader(NORMALIZED_CONJUNCTS));
        assertTrue(solver.isSatisfiable());
    }
}
