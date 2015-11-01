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

import java.lang.reflect.Field;
import java.math.BigInteger;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.constraints.AbstractDataStructureFactory;
import org.sat4j.minisat.constraints.card.AtLeast;
import org.sat4j.minisat.constraints.cnf.Clauses;
import org.sat4j.minisat.constraints.cnf.LearntBinaryClause;
import org.sat4j.minisat.constraints.cnf.LearntHTClause;
import org.sat4j.minisat.constraints.cnf.Lits;
import org.sat4j.minisat.constraints.cnf.OriginalBinaryClause;
import org.sat4j.minisat.constraints.cnf.OriginalHTClause;
import org.sat4j.minisat.constraints.cnf.UnitClause;
import org.sat4j.minisat.core.ILits;
import org.sat4j.pb.constraints.pb.AtLeastPB;
import org.sat4j.pb.constraints.pb.IDataStructurePB;
import org.sat4j.pb.constraints.pb.Pseudos;
import org.sat4j.pb.core.PBDataStructureFactory;
import org.sat4j.specs.Constr;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

/**
 * @author leberre To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class AbstractPBDataStructureFactory extends
        AbstractDataStructureFactory implements PBDataStructureFactory {

    interface INormalizer {
        PBContainer nice(IVecInt ps, IVec<BigInteger> bigCoefs,
                boolean moreThan, BigInteger bigDeg, ILits voc)
                throws ContradictionException;
    }

    public static final INormalizer FOR_COMPETITION = new INormalizer() {

        public PBContainer nice(IVecInt literals, IVec<BigInteger> coefs,
                boolean moreThan, BigInteger degree, ILits voc)
                throws ContradictionException {
            if (literals.size() != coefs.size()) {
                throw new IllegalArgumentException(
                        "Number of coeff and literals are different!!!");
            }
            IVecInt cliterals = new VecInt(literals.size());
            literals.copyTo(cliterals);
            IVec<BigInteger> ccoefs = new Vec<BigInteger>(literals.size());
            coefs.copyTo(ccoefs);
            for (int i = 0; i < cliterals.size();) {
                if (ccoefs.get(i).equals(BigInteger.ZERO)) {
                    cliterals.delete(i);
                    ccoefs.delete(i);
                } else {
                    if (voc.isSatisfied(cliterals.get(i))) {
                        degree = degree.subtract(ccoefs.get(i));
                        cliterals.delete(i);
                        ccoefs.delete(i);
                    } else {
                        if (voc.isFalsified(cliterals.get(i))) {
                            cliterals.delete(i);
                            ccoefs.delete(i);
                        } else
                            i++;
                    }
                }
            }
            int[] theLits = new int[cliterals.size()];
            cliterals.copyTo(theLits);
            BigInteger[] normCoefs = new BigInteger[ccoefs.size()];
            ccoefs.copyTo(normCoefs);
            BigInteger degRes = Pseudos.niceParametersForCompetition(theLits,
                    normCoefs, moreThan, degree);
            return new PBContainer(theLits, normCoefs, degRes);

        }

    };

    public static final INormalizer NO_COMPETITION = new INormalizer() {

        public PBContainer nice(IVecInt literals, IVec<BigInteger> coefs,
                boolean moreThan, BigInteger degree, ILits voc)
                throws ContradictionException {
            IVecInt cliterals = new VecInt(literals.size());
            literals.copyTo(cliterals);
            IVec<BigInteger> ccoefs = new Vec<BigInteger>(literals.size());
            coefs.copyTo(ccoefs);
            for (int i = 0; i < cliterals.size();) {
                if (voc.isSatisfied(cliterals.get(i))) {
                    degree = degree.subtract(ccoefs.get(i));
                    cliterals.delete(i);
                    ccoefs.delete(i);
                } else {
                    if (voc.isFalsified(cliterals.get(i))) {
                        cliterals.delete(i);
                        ccoefs.delete(i);
                    } else
                        i++;
                }
            }
            IDataStructurePB res = Pseudos.niceParameters(cliterals, ccoefs,
                    moreThan, degree, voc);
            int size = res.size();
            int[] theLits = new int[size];
            BigInteger[] theCoefs = new BigInteger[size];
            res.buildConstraintFromMapPb(theLits, theCoefs);
            BigInteger theDegree = res.getDegree();
            return new PBContainer(theLits, theCoefs, theDegree);
        }
    };

    private INormalizer norm = FOR_COMPETITION;

    protected INormalizer getNormalizer() {
        return this.norm;
    }

    public void setNormalizer(String simp) {
        Field f;
        try {
            f = AbstractPBDataStructureFactory.class.getDeclaredField(simp);
            this.norm = (INormalizer) f.get(this);
        } catch (Exception e) {
            e.printStackTrace();
            this.norm = FOR_COMPETITION;
        }
    }

    public void setNormalizer(INormalizer normalizer) {
        this.norm = normalizer;
    }

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public Constr createClause(IVecInt literals) throws ContradictionException {
        IVecInt v = Clauses.sanityCheck(literals, getVocabulary(), this.solver);
        if (v == null) {
            // tautological clause
            return null;
        }
        if (v.size() == 1) {
            return new UnitClause(v.last());
        }
        if (v.size() == 2) {
            return OriginalBinaryClause.brandNewClause(this.solver,
                    getVocabulary(), v);
        }
        return OriginalHTClause.brandNewClause(this.solver, getVocabulary(), v);
    }

    public Constr createUnregisteredClause(IVecInt literals) {
        if (literals.size() == 1) {
            return new UnitClause(literals.last());
        }
        if (literals.size() == 2) {
            return new LearntBinaryClause(literals, getVocabulary());
        }
        return new LearntHTClause(literals, getVocabulary());
    }

    @Override
    public Constr createCardinalityConstraint(IVecInt literals, int degree)
            throws ContradictionException {
        return AtLeastPB.atLeastNew(this.solver, getVocabulary(), literals,
                degree);
    }

    public Constr createPseudoBooleanConstraint(IVecInt literals,
            IVec<BigInteger> coefs, boolean moreThan, BigInteger degree)
            throws ContradictionException {
        PBContainer res = getNormalizer().nice(literals, coefs, moreThan,
                degree, getVocabulary());
        return constraintFactory(res.lits, res.coefs, res.degree);
    }

    public Constr createAtMostPBConstraint(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree)
            throws ContradictionException {
        return createPseudoBooleanConstraint(literals, coefs, false, degree);
    }

    public Constr createAtLeastPBConstraint(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree)
            throws ContradictionException {
        return createPseudoBooleanConstraint(literals, coefs, true, degree);
    }

    public Constr createUnregisteredPseudoBooleanConstraint(
            IDataStructurePB dspb) {
        return learntConstraintFactory(dspb);
    }

    public Constr createUnregisteredAtLeastConstraint(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree) {
        return learntAtLeastConstraintFactory(literals, coefs, degree);
    }

    public Constr createUnregisteredAtMostConstraint(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree) {
        return learntAtMostConstraintFactory(literals, coefs, degree);
    }

    protected abstract Constr constraintFactory(int[] literals,
            BigInteger[] coefs, BigInteger degree)
            throws ContradictionException;

    protected abstract Constr learntConstraintFactory(IDataStructurePB dspb);

    protected abstract Constr learntAtLeastConstraintFactory(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree);

    protected abstract Constr learntAtMostConstraintFactory(IVecInt literals,
            IVec<BigInteger> coefs, BigInteger degree);

    @Override
    protected ILits createLits() {
        return new Lits();
    }

    @Override
    public Constr createUnregisteredCardinalityConstraint(IVecInt literals,
            int degree) {
        return new AtLeast(getVocabulary(), literals, degree);
    }

}
