package org.sat4j.pb;

import java.math.BigInteger;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IGroupSolver;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public interface IGroupPBSolver extends IGroupSolver, IPBSolver {

    IConstr addAtMost(IVecInt literals, int degree, int groupid)
            throws ContradictionException;

    IConstr addAtLeast(IVecInt literals, int degree, int groupid)
            throws ContradictionException;

    IConstr addExactly(IVecInt literals, int n, int groupid)
            throws ContradictionException;

    IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException;

    IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree, int groupdId) throws ContradictionException;

    IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree, int groupId)
            throws ContradictionException;

    IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree, int groupId)
            throws ContradictionException;

    IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree, int groupId) throws ContradictionException;

    IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight, int groupId)
            throws ContradictionException;

    IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight, int groupId) throws ContradictionException;

}
