package org.sat4j.pb;

import java.math.BigInteger;

import org.sat4j.specs.IConstr;
import org.sat4j.specs.ISolverService;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public interface IPBSolverService extends ISolverService {

    IConstr addAtMostOnTheFly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree);

    IConstr addAtMostOnTheFly(IVecInt literals, IVecInt coeffs, int degree);

    ObjectiveFunction getObjectiveFunction();

}
