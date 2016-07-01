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

import org.sat4j.pb.constraints.pb.IDataStructurePB;
import org.sat4j.pb.constraints.pb.MapPb;
import org.sat4j.pb.constraints.pb.PBConstr;
import org.sat4j.pb.constraints.pb.Pseudos;
import org.sat4j.pb.constraints.pb.PuebloMinWatchPb;
import org.sat4j.specs.Constr;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

/**
 * @author leberre To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PuebloPBMinDataStructure extends AbstractPBDataStructureFactory {

    private static final long serialVersionUID = 1L;

    /*
     * (non-Javadoc)
     * 
     * @seeorg.sat4j.minisat.constraints.AbstractPBDataStructureFactory#
     * constraintFactory(org.sat4j.specs.VecInt, org.sat4j.specs.VecInt,
     * boolean, int)
     */
    @Override
    protected PBConstr constraintFactory(int[] literals, BigInteger[] coefs,
            BigInteger degree) throws ContradictionException {
        return PuebloMinWatchPb.normalizedMinWatchPbNew(this.solver,
                getVocabulary(), literals, coefs, degree);
    }

    @Override
    protected PBConstr learntConstraintFactory(IDataStructurePB dspb) {
        return PuebloMinWatchPb.normalizedWatchPbNew(getVocabulary(), dspb);
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
        degree = Pseudos.niceCheckedParametersForCompetition(lits, bc,
                moreThan, degree);

        return PuebloMinWatchPb.normalizedWatchPbNew(getVocabulary(),
                new MapPb(literals, coefs, degree));
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

}
