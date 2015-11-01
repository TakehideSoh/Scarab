package org.sat4j.pb.core;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IIntegerPBSolver;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.PBSolverDecorator;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;

/**
 * A PBSolver decorator which allow a PBSolver to handle integer variables.
 * 
 * @author lonca
 */
public class IntegerPBSolverDecorator extends PBSolverDecorator implements
        IIntegerPBSolver {

    private static final long serialVersionUID = 1L;

    private final IPBSolver decorated;

    public IntegerPBSolverDecorator(IPBSolver solver) {
        super(solver);
        this.decorated = solver;
    }

    public IntegerVariable newIntegerVar(BigInteger maxValue) {
        if (maxValue.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "the integer variable maximum value must be at least 1");
        }
        int nbVars = maxValue.bitLength();
        IVecInt vars = new VecInt(nbVars);
        for (int i = 0; i < nbVars; ++i) {
            vars.push(decorated.nextFreeVarId(true));
        }
        return new IntegerVariable(vars);
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger degree)
            throws ContradictionException {
        literals = new VecInt(
                Arrays.copyOf(literals.toArray(), literals.size()));
        coeffs = copyBigIntVec(coeffs);
        pushIntegerVariables(literals, coeffs, integerVars, integerVarsCoeffs);
        return decorated.addAtLeast(literals, coeffs, degree);
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int degree)
            throws ContradictionException {
        literals = new VecInt(
                Arrays.copyOf(literals.toArray(), literals.size()));
        coeffs = new VecInt(coeffs.toArray());
        pushIntegerVariables(literals, coeffs, integerVars, integerVarsCoeffs);
        return decorated.addAtLeast(literals, coeffs, degree);
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger degree)
            throws ContradictionException {
        literals = new VecInt(
                Arrays.copyOf(literals.toArray(), literals.size()));
        coeffs = copyBigIntVec(coeffs);
        pushIntegerVariables(literals, coeffs, integerVars, integerVarsCoeffs);
        return decorated.addAtMost(literals, coeffs, degree);
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int degree)
            throws ContradictionException {
        literals = new VecInt(
                Arrays.copyOf(literals.toArray(), literals.size()));
        coeffs = new VecInt(coeffs.toArray());
        pushIntegerVariables(literals, coeffs, integerVars, integerVarsCoeffs);
        return decorated.addAtMost(literals, coeffs, degree);
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger weight)
            throws ContradictionException {
        literals = new VecInt(
                Arrays.copyOf(literals.toArray(), literals.size()));
        coeffs = copyBigIntVec(coeffs);
        pushIntegerVariables(literals, coeffs, integerVars, integerVarsCoeffs);
        return decorated.addExactly(literals, coeffs, weight);
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int weight)
            throws ContradictionException {
        literals = new VecInt(
                Arrays.copyOf(literals.toArray(), literals.size()));
        coeffs = new VecInt(coeffs.toArray());
        pushIntegerVariables(literals, coeffs, integerVars, integerVarsCoeffs);
        return decorated.addExactly(literals, coeffs, weight);
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, boolean moreThan, BigInteger d)
            throws ContradictionException {
        lits = new VecInt(Arrays.copyOf(lits.toArray(), lits.size()));
        coeffs = copyBigIntVec(coeffs);
        pushIntegerVariables(lits, coeffs, integerVars, integerVarsCoeffs);
        return decorated.addPseudoBoolean(lits, coeffs, moreThan, d);
    }

    private void pushIntegerVariables(IVecInt literals,
            IVec<BigInteger> coeffs, IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs) {
        if (integerVars.size() != integerVarsCoeffs.size()) {
            throw new IllegalArgumentException(
                    "different number of integer variables and integer variables coeffs");
        }
        Iterator<IntegerVariable> intVarsIt = integerVars.iterator();
        Iterator<BigInteger> intVarsCoeffsIt = integerVarsCoeffs.iterator();
        while (intVarsIt.hasNext()) {
            BigInteger factor = intVarsCoeffsIt.next();
            IteratorInt intVarLitsIt = intVarsIt.next().getVars().iterator();
            while (intVarLitsIt.hasNext()) {
                literals.push(intVarLitsIt.next());
                coeffs.push(factor);
                factor = factor.shiftLeft(1);
            }
        }
    }

    private void pushIntegerVariables(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs) {
        if (integerVars.size() != integerVarsCoeffs.size()) {
            throw new IllegalArgumentException(
                    "different number of integer variables and integer variables coeffs");
        }
        Iterator<IntegerVariable> intVarsIt = integerVars.iterator();
        Iterator<BigInteger> intVarsCoeffsIt = integerVarsCoeffs.iterator();
        while (intVarsIt.hasNext()) {
            BigInteger factor = intVarsCoeffsIt.next();
            IteratorInt intVarLitsIt = intVarsIt.next().getVars().iterator();
            while (intVarLitsIt.hasNext()) {
                literals.push(intVarLitsIt.next());
                coeffs.push(factor.intValue());
                factor = factor.shiftLeft(1);
            }
        }
    }

    private void pushIntegerVariables(IVecInt vars, IVec<BigInteger> coeffs,
            IntegerVariable var, BigInteger weight) {
        pushIntegerVariables(vars, coeffs,
                new Vec<IntegerVariable>().push(var),
                new Vec<BigInteger>().push(weight));
    }

    public void addIntegerVariableToObjectiveFunction(IntegerVariable var,
            BigInteger weight) {
        IVecInt varsCopy = new VecInt(decorated().getObjectiveFunction()
                .getVars().toArray());
        Object[] coeffsArray = decorated().getObjectiveFunction().getCoeffs()
                .toArray();
        IVec<BigInteger> coeffsCopy = new Vec<BigInteger>();
        for (int i = 0; i < coeffsArray.length; ++i) {
            coeffsCopy.push((BigInteger) coeffsArray[i]);
        }
        pushIntegerVariables(varsCopy, coeffsCopy, var, weight);
        decorated().setObjectiveFunction(
                new ObjectiveFunction(varsCopy, coeffsCopy));
    }

    public IConstr addAtLeast(IntegerVariable var, int degree)
            throws ContradictionException {
        return decorated.addAtLeast(var.getVars(), integerVariableCoeffs(var),
                degree);
    }

    public IConstr addAtMost(IntegerVariable var, int degree)
            throws ContradictionException {
        return decorated.addAtMost(var.getVars(), integerVariableCoeffs(var),
                degree);
    }

    public IConstr addExactly(IntegerVariable var, int degree)
            throws ContradictionException {
        return decorated.addExactly(var.getVars(), integerVariableCoeffs(var),
                degree);
    }

    private IVecInt integerVariableCoeffs(IntegerVariable var) {
        int nbLits = var.getVars().size();
        IVecInt coeffs = new VecInt(nbLits);
        int factor = 1;
        for (int i = 0; i < nbLits; ++i) {
            coeffs.push(factor);
            factor <<= 1;
        }
        return coeffs;
    }

    private IVec<BigInteger> copyBigIntVec(IVec<BigInteger> src) {
        IVec<BigInteger> res = new Vec<BigInteger>();
        for (int i = 0; i < src.size(); ++i) {
            res.push(src.get(i));
        }
        return res;
    }

    public BigInteger getIntegerVarValue(IntegerVariable var) {
        BigInteger res = BigInteger.ZERO;
        BigInteger factor = BigInteger.ONE;
        for (IteratorInt it = var.getVars().iterator(); it.hasNext();) {
            if (decorated.model(it.next())) {
                res = res.add(factor);
            }
            factor = factor.shiftLeft(1);
        }
        return res;
    }

}
