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
package org.sat4j.pb.reader;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.reader.JSONReader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

/**
 * Simple JSON reader for boolean optimization problems.
 * 
 * The objective function is represented by an array of weighted literals.
 * Pseudo boolean constraints are represented by an array of weighted literals
 * of the left hand side, a comparator (a string) and an integer.
 * <code>[['min',[[1,1],[20,2],[80,3]]],[-1,-2,-3],[[1,-2,3],'>',2],[4,-3,6],[[[1,1],[2,2],[4,3],[8,4]],'<=',6]</code>
 * represents an optimization problem with an objective function, min: x1 + 20
 * x2, four constraints with two clauses, a cardinality constraint and the
 * pseudo boolean constraint 1 x1 + 2 x2 + 4 x3 + 8 x4 <= 6.
 * 
 * @author leberre
 * @since 2.3.3
 */
public class JSONPBReader extends JSONReader<IPBSolver> {
	public static final String WLITERAL = "\\[(-?\\d+),(-?\\d+)\\]";
	public static final String WCLAUSE = "(\\[(" + WLITERAL + "(," + WLITERAL
			+ ")*)?\\])";
	public static final String PB = "(\\[" + WCLAUSE + ",'[=<>]=?',-?\\d+\\])";

	public static final String OBJECTIVE_FUNCTION = "(\\[('min'|'max'),"
			+ WCLAUSE + "\\])";

	public static final Pattern PSEUDO_PATTERN = Pattern.compile(PB);
	public static final Pattern WCLAUSE_PATTERN = Pattern.compile(WCLAUSE);
	public static final Pattern WLITERAL_PATTERN = Pattern.compile(WLITERAL);
	public static final Pattern OBJECTIVE_FUNCTION_PATTERN = Pattern
			.compile(OBJECTIVE_FUNCTION);

	public JSONPBReader(IPBSolver solver) {
		super(solver);
	}

	@Override
	protected void handleNotHandled(String constraint)
			throws ParseFormatException, ContradictionException {
		if (PSEUDO_PATTERN.matcher(constraint).matches()) {
			handlePB(constraint);
		} else if (OBJECTIVE_FUNCTION_PATTERN.matcher(constraint).matches()) {
			handleObj(constraint);
		} else {
			throw new UnsupportedOperationException("Wrong formula "
					+ constraint);
		}
	}

	private void handleObj(String constraint) {
		Matcher matcher = WCLAUSE_PATTERN.matcher(constraint);
		if (matcher.find()) {
			String weightedLiterals = matcher.group();
			constraint = matcher.replaceFirst("");
			matcher = WLITERAL_PATTERN.matcher(weightedLiterals);
			IVecInt literals = new VecInt();
			String[] pieces = constraint.split(",");
			boolean negate = pieces[0].contains("max");
			IVec<BigInteger> coefs = new Vec<BigInteger>();
			BigInteger coef;
			while (matcher.find()) {
				literals.push(Integer.valueOf(matcher.group(2)));
				coef = new BigInteger(matcher.group(1));
				coefs.push(negate ? coef.negate() : coef);
			}
			solver.setObjectiveFunction(new ObjectiveFunction(literals, coefs));
		}

	}

	private void handlePB(String constraint) throws ContradictionException {
		Matcher matcher = WCLAUSE_PATTERN.matcher(constraint);
		if (matcher.find()) {
			String weightedLiterals = matcher.group();
			constraint = matcher.replaceFirst("");
			matcher = WLITERAL_PATTERN.matcher(weightedLiterals);
			IVecInt literals = new VecInt();
			IVecInt coefs = new VecInt();
			while (matcher.find()) {
				literals.push(Integer.valueOf(matcher.group(2)));
				coefs.push(Integer.valueOf(matcher.group(1)));
			}
			String[] pieces = constraint.split(",");
			String comp = pieces[1].substring(1, pieces[1].length() - 1);
			int degree = Integer.valueOf(pieces[2].substring(0,
					pieces[2].length() - 1));
			if ("=".equals(comp) || "==".equals(comp)) {
				solver.addExactly(literals, coefs, degree);
			} else if ("<=".equals(comp)) {
				solver.addAtMost(literals, coefs, degree);
			} else if ("<".equals(comp)) {
				solver.addAtMost(literals, coefs, degree - 1);
			} else if (">=".equals(comp)) {
				solver.addAtLeast(literals, coefs, degree);
			} else {
				assert ">".equals(comp);
				solver.addAtLeast(literals, coefs, degree + 1);
			}
		}

	}

	@Override
	protected String constraintRegexp() {
		return "(" + CLAUSE + "|" + CARD + "|" + PB + "|" + OBJECTIVE_FUNCTION
				+ ")";
	}
}
