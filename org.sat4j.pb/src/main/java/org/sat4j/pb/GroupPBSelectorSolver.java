package org.sat4j.pb;

import java.math.BigInteger;
import java.util.Iterator;

import org.sat4j.core.ConstrGroup;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;
import org.sat4j.tools.GroupClauseSelectorSolver;

public class GroupPBSelectorSolver extends GroupClauseSelectorSolver<IPBSolver>
        implements IGroupPBSolver {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public GroupPBSelectorSolver(IPBSolver solver) {
        super(solver);
    }

    public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs,
            boolean moreThan, BigInteger d) throws ContradictionException {
        return decorated().addPseudoBoolean(lits, coeffs, moreThan, d);
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        return decorated().addAtMost(literals, coeffs, degree);
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        return decorated().addAtMost(literals, coeffs, degree);
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree)
            throws ContradictionException {
        return decorated().addAtLeast(literals, coeffs, degree);
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree) throws ContradictionException {
        return decorated().addAtLeast(literals, coeffs, degree);
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight)
            throws ContradictionException {
        return decorated().addExactly(literals, coeffs, weight);
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight) throws ContradictionException {
        return decorated().addExactly(literals, coeffs, weight);
    }

    public void setObjectiveFunction(ObjectiveFunction obj) {
        decorated().setObjectiveFunction(obj);

    }

    public ObjectiveFunction getObjectiveFunction() {
        return decorated().getObjectiveFunction();
    }

    public IConstr addAtMost(IVecInt literals, int degree, int groupid)
            throws ContradictionException {
        IVecInt coeffs = new VecInt(literals.size(), 1);
        Integer newvar = getGroupVar(literals, groupid);
        literals.push(newvar);
        coeffs.push(degree - coeffs.size());
        return decorated().addAtMost(literals, coeffs, degree);
    }

    public IConstr addAtLeast(IVecInt literals, int degree, int groupid)
            throws ContradictionException {
        IVecInt coeffs = new VecInt(literals.size(), 1);
        int newvar = getGroupVar(literals, groupid);
        literals.push(newvar);
        coeffs.push(degree);
        return decorated().addAtLeast(literals, coeffs, degree);
    }

    public IConstr addExactly(IVecInt literals, int n, int groupid)
            throws ContradictionException {
        int newvar = getGroupVar(literals, groupid);
        // at most
        IVecInt coeffs = new VecInt(literals.size(), 1);
        literals.push(newvar);
        coeffs.push(n - coeffs.size());
        IConstr constr1 = decorated().addAtMost(literals, coeffs, n);
        // at least
        coeffs.pop();
        coeffs.push(n);
        IConstr constr2 = decorated().addAtLeast(literals, coeffs, n);
        if (constr1 == null && constr2 == null) {
            discardLastestVar();
            return null;
        }
        ConstrGroup group = new ConstrGroup();
        group.add(constr1);
        group.add(constr2);
        return group;
    }

    public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree, int groupdId) throws ContradictionException {
        int newvar = getGroupVar(literals, groupdId);
        literals.push(newvar);
        BigInteger sum = BigInteger.ZERO;
        for (Iterator<BigInteger> ite = coeffs.iterator(); ite.hasNext();) {
            sum = sum.add(ite.next());
        }
        sum = sum.subtract(degree);
        coeffs.push(sum.negate());
        return decorated().addAtMost(literals, coeffs, degree);
    }

    public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree,
            int groupdId) throws ContradictionException {
        int newvar = getGroupVar(literals, groupdId);
        literals.push(newvar);
        int sum = 0;
        for (IteratorInt ite = coeffs.iterator(); ite.hasNext();) {
            sum += ite.next();
        }
        sum = sum - degree;
        coeffs.push(-sum);
        return decorated().addAtMost(literals, coeffs, degree);
    }

    public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree,
            int groupId) throws ContradictionException {
        int newvar = getGroupVar(literals, groupId);
        literals.push(newvar);
        if (degree >= 0) {
            coeffs.push(degree);
        } else {
            int sum = 0;
            for (IteratorInt ite = coeffs.iterator(); ite.hasNext();) {
                sum = sum + ite.next();
            }
            sum = sum - degree;
            coeffs.push(-sum);
        }
        return decorated().addAtLeast(literals, coeffs, degree);
    }

    public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger degree, int groupId) throws ContradictionException {
        int newvar = getGroupVar(literals, groupId);
        literals.push(newvar);
        if (degree.signum() >= 0) {
            coeffs.push(degree);
        } else {
            BigInteger sum = BigInteger.ZERO;
            for (Iterator<BigInteger> ite = coeffs.iterator(); ite.hasNext();) {
                sum = sum.add(ite.next());
            }
            sum = sum.subtract(degree);
            coeffs.push(sum.negate());
        }
        return decorated().addAtLeast(literals, coeffs, degree);
    }

    public IConstr addExactly(IVecInt literals, IVecInt coeffs, int weight,
            int groupId) throws ContradictionException {
        throw new UnsupportedOperationException("not implemented yet!");
    }

    public IConstr addExactly(IVecInt literals, IVec<BigInteger> coeffs,
            BigInteger weight, int groupId) throws ContradictionException {
        throw new UnsupportedOperationException("not implemented yet!");
    }

}
