package org.sat4j.pb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sat4j.core.VecInt;
import org.sat4j.pb.tools.AtLeastCard;
import org.sat4j.pb.tools.CardConstrFinder;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;

public class CardConstrFinderTest {

    private IPBSolver solver;
    private CardConstrFinder ccFinder;
    private Set<AtLeastCard> expectedCards;

    @Before
    public void setUp() {
        solver = SolverFactory.newDefault();
        ccFinder = new CardConstrFinder(solver);
        ccFinder.setVerbose(false);
        expectedCards = new HashSet<AtLeastCard>();
    }

    private void addClause(int... lits) throws ContradictionException {
        IVecInt vec = new VecInt(lits);
        solver.addClause(vec);
        ccFinder.addClause(vec);
    }

    private void addAtLeast(int threshold, int... lits)
            throws ContradictionException {
        IVecInt vec = new VecInt(lits);
        solver.addAtLeast(vec, threshold);
        ccFinder.addAtLeast(vec, threshold);
    }

    private void addAtMost(int threshold, int... lits)
            throws ContradictionException {
        IVecInt vec = new VecInt(lits);
        solver.addAtMost(vec, threshold);
        ccFinder.addAtMost(vec, threshold);
    }

    private void addExpectedAtLeastCard(int threshold, int... lits) {
        this.expectedCards.add(new AtLeastCard(new VecInt(lits), threshold));
    }

    private void assertRightDetection(int expectedRemainingConstrs) {
        ccFinder.searchCards();
        int nCards = this.expectedCards.size();
        StringBuffer actual = new StringBuffer();
        actual.append('[');
        for (AtLeastCard card : ccFinder) {
            assertTrue(card + " is not valid here",
                    this.expectedCards.contains(card));
            actual.append(card + " ");
            --nCards;
        }
        if (nCards != 0) {
            actual.append(']');
            fail("found " + (this.expectedCards.size() - nCards)
                    + " cards instead of " + this.expectedCards.size()
                    + " (expected=" + this.expectedCards + ", actual="
                    + actual.toString());
        }
        assertEquals(expectedRemainingConstrs, ccFinder.remainingAtLeastCards()
                .size());
    }

    @Test
    public void testEmpty() {
        ccFinder.searchCards();
        assertFalse(ccFinder.hasNext());
    }

    @Test
    public void testNoCards() throws ContradictionException {
        addClause(1, 2, 3);
        assertRightDetection(1);
    }

    @Test
    public void testThresholdClause() throws ContradictionException {
        addClause(-1, -2);
        addClause(-1, -3);
        addClause(-2, -3);
        addExpectedAtLeastCard(2, -1, -2, -3);
        assertRightDetection(0);
    }

    @Test
    public void testThresholdAtLeast() throws ContradictionException {
        addAtLeast(1, -1, -2);
        addAtLeast(1, -1, -3);
        addAtLeast(1, -2, -3);
        addExpectedAtLeastCard(2, -1, -2, -3);
        assertRightDetection(0);
    }

    @Test
    public void testThresholdAtLeast2() throws ContradictionException {
        addAtLeast(2, -1, -2, -4);
        addAtLeast(2, -1, -3, -4);
        addAtLeast(2, -2, -3, -4);
        addExpectedAtLeastCard(3, -1, -2, -3, -4);
        assertRightDetection(0);
    }

    @Test
    public void testThresholdAtMost() throws ContradictionException {
        addAtMost(1, 1, 2);
        addAtMost(1, 1, 3);
        addAtMost(1, 2, 3);
        addExpectedAtLeastCard(2, -1, -2, -3);
        assertRightDetection(0);
    }

    @Test
    public void testThresholdAtMost2() throws ContradictionException {
        addAtMost(2, 1, 2, 3);
        addAtMost(2, 1, 2, 4);
        addAtMost(2, 2, 3, 4);
        addExpectedAtLeastCard(2, -1, -2, -3, -4);
    }

    @Test
    public void testThresholdMixed() throws ContradictionException {
        addAtLeast(2, -1, -2, -3);
        addClause(-4, -1);
        addClause(-4, -2);
        addClause(-4, -3);
        addExpectedAtLeastCard(3, -1, -2, -3, -4);
        assertRightDetection(0);
    }

    @Test
    public void testThresholdMixedReverse() throws ContradictionException {
        addClause(-4, -1);
        addClause(-4, -2);
        addClause(-4, -3);
        addAtLeast(2, -1, -2, -3);
        addExpectedAtLeastCard(3, -1, -2, -3, -4);
        assertRightDetection(0);
    }

    @Test
    public void setSubsumed1() throws ContradictionException {
        addClause(-4, -1);
        addClause(-4, -2);
        addClause(-4, -3);
        addAtLeast(2, -1, -2, -3);
        addClause(-4, -3);
        addExpectedAtLeastCard(3, -1, -2, -3, -4);
        assertRightDetection(0);
    }

}
