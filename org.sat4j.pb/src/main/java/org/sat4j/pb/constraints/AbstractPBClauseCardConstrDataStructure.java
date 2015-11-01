/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004, 2012 Artois University and CNRS
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU Lesser General Public License Version 2.1 or later (the
 * "LGPL"), in which case the provisions of the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting
 * the provisions above and replace them with the notice and other provisions
 * required by the LGPL. If you do not delete the provisions above, a recipient
 * may use your version of this file under the terms of the EPL or the LGPL.
 *
 * Based on the original MiniSat specification from:
 *
 * An extensible SAT solver. Niklas Een and Niklas Sorensson. Proceedings of the
 * Sixth International Conference on Theory and Applications of Satisfiability
 * Testing, LNCS 2919, pp 502-518, 2003.
 *
 * See www.minisat.se for the original solver in C++.
 *
 * Contributors:
 *   CRIL - initial API and implementation
 *******************************************************************************/
package org.sat4j.pb.constraints;

import java.math.BigInteger;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.constraints.cnf.Clauses;
import org.sat4j.pb.constraints.pb.IDataStructurePB;
import org.sat4j.pb.constraints.pb.MapPb;
import org.sat4j.pb.constraints.pb.Pseudos;
import org.sat4j.specs.Constr;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public abstract class AbstractPBClauseCardConstrDataStructure extends
        AbstractPBDataStructureFactory {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    static final BigInteger MAX_INT_VALUE = BigInteger
            .valueOf(Integer.MAX_VALUE);

    private final IPBConstructor ipbc;
    private final ICardConstructor icardc;
    private final IClauseConstructor iclausec;

    AbstractPBClauseCardConstrDataStructure(IClauseConstructor iclausec,
            ICardConstructor icardc, IPBConstructor ipbc) {
        this.iclausec = iclausec;
        this.icardc = icardc;
        this.ipbc = ipbc;
    }

    @Override
    public Constr createClause(IVecInt literals) throws ContradictionException {
        IVecInt v = Clauses.sanityCheck(literals, getVocabulary(), this.solver);
        return constructClause(v);
    }

    @Override
    public Constr createUnregisteredClause(IVecInt literals) {
        return constructLearntClause(literals);
    }

    @Override
    public Constr createCardinalityConstraint(IVecInt literals, int degree)
            throws ContradictionException {
        return constructCard(literals, degree);
    }

    @Override
    public Constr createUnregisteredCardinalityConstraint(IVecInt literals,
            int degree) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.sat4j.minisat.constraints.AbstractPBDataStructureFactory#
     * constraintFactory(org.sat4j.specs.VecInt, org.sat4j.specs.VecInt,
     * boolean, int)
     */
    @Override
    protected Constr constraintFactory(int[] literals, BigInteger[] coefs,
            BigInteger degree) throws ContradictionException {
        if (literals.length == 0 && degree.signum() <= 0) {
            return null;
        }
        if (degree.equals(BigInteger.ONE)) {
            IVecInt v = Clauses.sanityCheck(new VecInt(literals),
                    getVocabulary(), this.solver);
            if (v == null) {
                return null;
            }
            return constructClause(v);
        }
        if (coefficientsEqualTo(BigInteger.ONE, coefs)) {
            assert degree.compareTo(MAX_INT_VALUE) < 0;
            return constructCard(new VecInt(literals), degree.intValue());
        }
        // if (coefficientsEqualTo(coefs[0], coefs)) {
        // assert degree.compareTo(MAX_INT_VALUE) < 0;
        // System.err.println("Using TCS division!!!");
        // return constructCard(new VecInt(literals), degree.divide(coefs[0])
        // .intValue() + 1);
        // }
        return constructPB(literals, coefs, degree);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.sat4j.minisat.constraints.AbstractPBDataStructureFactory#
     * constraintFactory(org.sat4j.specs.VecInt, org.sat4j.specs.VecInt, int)
     */
    @Override
    protected Constr learntConstraintFactory(IDataStructurePB dspb) {
        if (dspb.getDegree().equals(BigInteger.ONE)) {
            IVecInt literals = new VecInt();
            IVec<BigInteger> resCoefs = new Vec<BigInteger>();
            dspb.buildConstraintFromConflict(literals, resCoefs);
            // then assertive literal must be placed at the first place
            int indLit = dspb.getAssertiveLiteral();
            if (indLit > -1) {
                int tmp = literals.get(indLit);
                literals.set(indLit, literals.get(0));
                literals.set(0, tmp);
            }
            return constructLearntClause(literals);
        }
        if (dspb.isCardinality()) {
            return constructLearntCard(dspb);
        }
        return constructLearntPB(dspb);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.sat4j.minisat.constraints.AbstractPBDataStructureFactory#
     * constraintFactory(org.sat4j.specs.VecInt, org.sat4j.specs.VecInt, int)
     */
    private Constr learntConstraintFactory(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree, boolean moreThan) {
        int[] lits = new int[literals.size()];
        literals.copyTo(lits);
        BigInteger[] bc = new BigInteger[coefs.size()];
        coefs.copyTo(bc);
        // System.out.println("Checking 1");
        degree = Pseudos.niceCheckedParametersForCompetition(lits, bc,
                moreThan, degree);

        if (degree.equals(BigInteger.ONE)) {
            return constructLearntClause(new VecInt(lits));
        }
        if (coefficientsEqualTo(BigInteger.ONE, bc)) {
            return constructLearntCard(new VecInt(lits),
                    new Vec<BigInteger>(bc), degree);
        }
        // System.out.println("Checking 2");
        if (coefficientsEqualTo(bc[0], bc)) {
            // System.out.println("Learned new card ! ");
            return constructLearntCard(new VecInt(lits),
                    new Vec<BigInteger>(bc),
                    degree.divide(bc[0]).add(BigInteger.ONE));
        }
        return constructLearntPB(new VecInt(lits), new Vec<BigInteger>(bc),
                degree);
    }

    @Override
    protected Constr learntAtLeastConstraintFactory(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree) {
        return learntConstraintFactory(literals, coefs, degree, true);
    }

    @Override
    protected Constr learntAtMostConstraintFactory(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree) {
        return learntConstraintFactory(literals, coefs, degree, false);
    }

    static boolean coefficientsEqualTo(BigInteger value, BigInteger[] coefs) {
        for (int i = 0; i < coefs.length; i++) {
            if (!coefs[i].equals(value)) {
                return false;
            }
        }
        return true;
    }

    protected Constr constructClause(IVecInt v) {
        return this.iclausec.constructClause(this.solver, getVocabulary(), v);
    }

    protected Constr constructCard(IVecInt theLits, int degree)
            throws ContradictionException {
        return this.icardc.constructCard(this.solver, getVocabulary(), theLits,
                degree);
    }

    protected Constr constructPB(int[] theLits, BigInteger[] coefs,
            BigInteger degree) throws ContradictionException {
        return this.ipbc.constructPB(this.solver, getVocabulary(), theLits,
                coefs, degree, sumOfCoefficients(coefs));
    }

    protected Constr constructLearntClause(IVecInt literals) {
        return this.iclausec.constructLearntClause(getVocabulary(), literals);
    }

    protected Constr constructLearntCard(IDataStructurePB dspb) {
        return this.icardc.constructLearntCard(getVocabulary(), dspb);
    }

    protected Constr constructLearntCard(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree) {
        return this.icardc.constructLearntCard(getVocabulary(), new MapPb(
                literals, coefs, degree));
    }

    protected Constr constructLearntPB(IDataStructurePB dspb) {
        return this.ipbc.constructLearntPB(getVocabulary(), dspb);
    }

    protected Constr constructLearntPB(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree) {
        return this.ipbc.constructLearntPB(getVocabulary(), new MapPb(literals,
                coefs, degree));
    }

    public static final BigInteger sumOfCoefficients(BigInteger[] coefs) {
        BigInteger sumCoefs = BigInteger.ZERO;
        for (BigInteger c : coefs) {
            sumCoefs = sumCoefs.add(c);
        }
        return sumCoefs;
    }

}
