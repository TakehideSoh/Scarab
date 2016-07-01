package org.sat4j.pb.tools;

import java.math.BigInteger;

import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.PBSolverDecorator;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

public class OptimalModelIterator extends PBSolverDecorator {

    private final long bound;
    protected long nbModelFound = 0;
    private boolean trivialfalsity = false;

    private boolean isFirstModel = true;
    private BigInteger objectiveValue;

    private final IPBSolver solver;

    private static final long serialVersionUID = 1L;

    /**
     * Create an iterator over the solutions available in <code>solver</code>.
     * The iterator will look for one new model at each call to isSatisfiable()
     * and will discard that model at each call to model().
     * 
     * @param solver
     *            a solver containing the constraints to satisfy.
     * @see #isSatisfiable()
     * @see #isSatisfiable(boolean)
     * @see #isSatisfiable(IVecInt)
     * @see #isSatisfiable(IVecInt, boolean)
     * @see #model()
     */
    public OptimalModelIterator(IPBSolver solver) {
        this(solver, Long.MAX_VALUE);
    }

    /**
     * Create an iterator over a limited number of solutions available in
     * <code>solver</code>. The iterator will look for one new model at each
     * call to isSatisfiable() and will discard that model at each call to
     * model(). At most <code>bound</code> calls to models() will be allowed
     * before the method <code>isSatisfiable()</code> returns false.
     * 
     * @param solver
     *            a solver containing the constraints to satisfy.
     * @param bound
     *            the maximum number of models to return.
     * @since 2.1
     * @see #isSatisfiable()
     * @see #isSatisfiable(boolean)
     * @see #isSatisfiable(IVecInt)
     * @see #isSatisfiable(IVecInt, boolean)
     * @see #model()
     */
    public OptimalModelIterator(IPBSolver solver, long bound) {
        super(solver);
        this.solver = solver;
        this.bound = bound;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sat4j.ISolver#model()
     */
    @Override
    public int[] model() {
        int[] last = super.model();
        this.nbModelFound++;

        try {
            ObjectiveFunction obj = this.getObjectiveFunction();
            if (isFirstModel && obj != null) {
                objectiveValue = obj.calculateDegree(solver);
                solver.addAtMost(obj.getVars(), obj.getCoeffs(), objectiveValue);
                isFirstModel = false;
            }
        } catch (ContradictionException e) {
            trivialfalsity = true;
        }

        return last;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sat4j.ISolver#isSatisfiable()
     */
    @Override
    public boolean isSatisfiable() throws TimeoutException {
        if (this.trivialfalsity || this.nbModelFound >= this.bound) {
            return false;
        }
        this.trivialfalsity = false;
        return super.isSatisfiable(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sat4j.ISolver#isSatisfiable(org.sat4j.datatype.VecInt)
     */
    @Override
    public boolean isSatisfiable(IVecInt assumps) throws TimeoutException {
        if (this.trivialfalsity || this.nbModelFound >= this.bound) {
            return false;
        }
        this.trivialfalsity = false;
        return super.isSatisfiable(assumps, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sat4j.ISolver#reset()
     */
    @Override
    public void reset() {
        this.trivialfalsity = false;
        this.nbModelFound = 0;
        super.reset();
    }

}
