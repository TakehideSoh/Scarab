package org.sat4j.pb;

import java.math.BigInteger;

import org.sat4j.pb.core.IntegerVariable;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

/**
 * A PBSolver which implements this interface must be able to handle bounded
 * positive integer variables.
 * 
 * It defines PB constraints adding methods which are composed of a boolean
 * part, and an integer part. Each part is made of a variable vector and a
 * weight vector.
 * 
 * @author lonca
 * 
 */
public interface IIntegerPBSolver extends IPBSolver {

    IntegerVariable newIntegerVar(BigInteger maxValue);

    BigInteger getIntegerVarValue(IntegerVariable var);

    IConstr addAtLeast(IntegerVariable var, int degree)
            throws ContradictionException;

    IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger degree)
            throws ContradictionException;

    IConstr addAtLeast(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int degree)
            throws ContradictionException;

    IConstr addAtMost(IntegerVariable var, int degree)
            throws ContradictionException;

    IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger degree)
            throws ContradictionException;

    IConstr addAtMost(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int degree)
            throws ContradictionException;

    IConstr addExactly(IntegerVariable var, int degree)
            throws ContradictionException;

    IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, BigInteger weight)
            throws ContradictionException;

    IConstr addExactly(IVecInt literals, IVecInt coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, int weight)
            throws ContradictionException;

    IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            IVec<IntegerVariable> integerVars,
            IVec<BigInteger> integerVarsCoeffs, boolean moreThan, BigInteger d)
            throws ContradictionException;

    void addIntegerVariableToObjectiveFunction(IntegerVariable var,
            BigInteger weight);

}
