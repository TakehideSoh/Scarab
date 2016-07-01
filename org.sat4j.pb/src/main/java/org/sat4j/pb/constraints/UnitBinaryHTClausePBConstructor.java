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

import org.sat4j.minisat.core.ILits;
import org.sat4j.pb.constraints.pb.LearntBinaryClausePB;
import org.sat4j.pb.constraints.pb.LearntHTClausePB;
import org.sat4j.pb.constraints.pb.OriginalBinaryClausePB;
import org.sat4j.pb.constraints.pb.OriginalHTClausePB;
import org.sat4j.pb.constraints.pb.UnitClausePB;
import org.sat4j.specs.Constr;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.UnitPropagationListener;

public class UnitBinaryHTClausePBConstructor implements IClauseConstructor {

    public Constr constructClause(UnitPropagationListener solver, ILits voc,
            IVecInt v) {
        if (v == null) {
            // tautological clause
            return null;
        }
        if (v.size() == 1) {
            return new UnitClausePB(v.last(), voc);
        }
        if (v.size() == 2) {
            return OriginalBinaryClausePB.brandNewClause(solver, voc, v);
        }
        return OriginalHTClausePB.brandNewClause(solver, voc, v);
    }

    public Constr constructLearntClause(ILits voc, IVecInt literals) {
        if (literals.size() == 1) {
            return new UnitClausePB(literals.last(), voc);
        }
        if (literals.size() == 2) {
            return new LearntBinaryClausePB(literals, voc);
        }
        return new LearntHTClausePB(literals, voc);
    }

}
