package org.sat4j.pb;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.tools.AllMUSes;

public class AllMusesPBTest {

    private GroupPBSelectorSolver solver;
    private AllMUSes muses;

    @Before
    public void setup() {
        this.solver = new GroupPBSelectorSolver(SolverFactory.newDefault());
        this.muses = new AllMUSes(this.solver, SolverFactory.instance());
    }

    @Test
    public void musesWithOnlyCLauses() throws ContradictionException {
        solver.newVar(2);
        IVecInt clause = new VecInt();
        clause.push(-1).push(-2);
        solver.addClause(clause, 1);
        clause.clear();
        clause.push(1);
        solver.addClause(clause, 2);
        clause.clear();
        clause.push(2);
        solver.addClause(clause, 3);
        clause.clear();
        clause.push(1).push(-2);
        solver.addClause(clause, 4);
        clause.clear();
        List<IVecInt> allMUSes = muses.computeAllMUSes();
        assertEquals(2, allMUSes.size());
        for (IVecInt mus : allMUSes) {
            assertEquals(3, mus.size());
        }
    }

    @Test
    public void musesWithCardAndCLauses() throws ContradictionException {
        solver.newVar(2);
        IVecInt lits = new VecInt();
        lits.push(-1).push(-2);
        solver.addClause(lits, 1);
        lits.clear();
        lits.push(1);
        solver.addClause(lits, 2);
        lits.clear();
        lits.push(2);
        solver.addClause(lits, 3);
        lits.clear();
        lits.push(1).push(2);
        solver.addAtMost(lits, 1, 4);
        lits.clear();
        List<IVecInt> allMUSes = muses.computeAllMUSes();
        assertEquals(2, allMUSes.size());
        for (IVecInt mus : allMUSes) {
            assertEquals(3, mus.size());
        }
    }

    @Test
    public void musesWithPBAndCLauses() throws ContradictionException {
        solver.newVar(2);
        IVecInt lits = new VecInt();
        lits.push(-1).push(-2);
        solver.addClause(lits, 1);
        lits.clear();
        lits.push(1);
        solver.addClause(lits, 2);
        lits.clear();
        lits.push(2);
        solver.addClause(lits, 3);
        lits.clear();
        IVecInt coefs = new VecInt(2, 1);
        lits.push(1).push(2);
        solver.addAtMost(lits, coefs, 1, 4);
        lits.clear();
        List<IVecInt> allMUSes = muses.computeAllMUSes();
        assertEquals(2, allMUSes.size());
        for (IVecInt mus : allMUSes) {
            assertEquals(3, mus.size());
        }
    }

    @Test
    public void musesWithPBAndCLauses2() throws ContradictionException {
        solver.newVar(3);
        IVecInt lits = new VecInt();
        lits.push(-1).push(-2);
        solver.addClause(lits, 1);
        lits.clear();
        lits.push(1);
        solver.addClause(lits, 2);
        lits.clear();
        lits.push(2);
        solver.addClause(lits, 3);
        lits.clear();
        IVecInt coefs = new VecInt();
        coefs.push(2).push(3).push(1);
        lits.push(1).push(2).push(3);
        solver.addAtMost(lits, coefs, 2, 4);
        lits.clear();
        List<IVecInt> allMUSes = muses.computeAllMUSes();
        assertEquals(2, allMUSes.size());
    }
}
