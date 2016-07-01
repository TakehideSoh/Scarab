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
package org.sat4j.reader;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;

/**
 * An reader having the responsability to choose the right reader according to
 * the input.
 * 
 * @author leberre
 */
public class InstanceReader extends Reader {

    private AAGReader aag;

    private AIGReader aig;

    private DimacsReader ezdimacs;

    private LecteurDimacs dimacs;

    private Reader reader = null;

    private final ISolver solver;

    public InstanceReader(ISolver solver) {
        // dimacs = new DimacsReader(solver);
        this.solver = solver;
    }

    private Reader getDefaultSATReader() {
        if (this.dimacs == null) {
            this.dimacs = new LecteurDimacs(this.solver);// new
                                                         // LecteurDimacs(solver);
        }
        return this.dimacs;
    }

    private Reader getEZSATReader() {
        if (this.ezdimacs == null) {
            this.ezdimacs = new DimacsReader(this.solver);// new
                                                          // LecteurDimacs(solver);
        }
        return this.ezdimacs;
    }

    private Reader getAIGReader() {
        if (this.aig == null) {
            this.aig = new AIGReader(this.solver);
        }
        return this.aig;
    }

    private Reader getAAGReader() {
        if (this.aag == null) {
            this.aag = new AAGReader(this.solver);
        }
        return this.aag;
    }

    @Override
    public IProblem parseInstance(String filename) throws ParseFormatException,
            IOException, ContradictionException {
        String fname;
        String prefix = "";

        if (filename.startsWith("http://")) {
            filename = filename.substring(filename.lastIndexOf('/'),
                    filename.length() - 1);
        }

        if (filename.indexOf(':') != -1) {
            String[] parts = filename.split(":");
            filename = parts[1];
            prefix = parts[0].toUpperCase(Locale.getDefault());

        }

        if (filename.endsWith(".gz") || filename.endsWith(".bz2")) {
            fname = filename.substring(0, filename.lastIndexOf('.'));
        } else {
            fname = filename;
        }
        this.reader = handleFileName(fname, prefix);
        return this.reader.parseInstance(filename);
    }

    protected Reader handleFileName(String fname, String prefix) {
        if ("EZCNF".equals(prefix)) {
            return getEZSATReader();
        }
        if (fname.endsWith(".aag")) {
            return getAAGReader();
        }
        if (fname.endsWith(".aig")) {
            return getAIGReader();
        }
        return getDefaultSATReader();
    }

    @Override
    @Deprecated
    public String decode(int[] model) {
        return this.reader.decode(model);
    }

    @Override
    public void decode(int[] model, PrintWriter out) {
        this.reader.decode(model, out);
    }

    @Override
    public IProblem parseInstance(java.io.InputStream in)
            throws ParseFormatException, ContradictionException, IOException {
        throw new UnsupportedOperationException(
                "Use a domain specific Reader (LecteurDimacs, AIGReader, etc.) for stream input ");
    }

    @Override
    public boolean hasAMapping() {
        return this.reader.hasAMapping();
    }

    @Override
    public Map<Integer, String> getMapping() {
        return this.reader.getMapping();
    }
}
