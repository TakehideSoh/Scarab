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
package org.sat4j.pb;

import java.util.ArrayList;
import java.util.List;

public class DepdendenyNode<C> {
    private final C name;
    private List<DepdendenyNode<C>> children;
    private final Explanation<C> explanation;

    public DepdendenyNode(C name, Explanation<C> explanation) {
        this.name = name;
        this.explanation = explanation;
    }

    public C getName() {
        return this.name;
    }

    public DepdendenyNode<C> newChild(C name) {
        DepdendenyNode<C> newNode = this.explanation.newNode(name);
        if (this.children == null) {
            this.children = new ArrayList<DepdendenyNode<C>>();
        }
        this.children.add(newNode);
        return newNode;
    }

    public boolean hasBranches() {
        if (this.children == null || this.children.isEmpty()) {
            return false;
        }
        if (this.children.size() > 1) {
            return true;
        }
        return this.children.get(0).hasBranches();
    }

    public int getMaxDepth() {
        if (this.children == null || this.children.isEmpty()) {
            return 1;
        }
        int maxChildDepth = 0;
        for (DepdendenyNode<C> child : this.children) {
            int childDepth = child.getMaxDepth();
            if (childDepth > maxChildDepth) {
                maxChildDepth = childDepth;
            }
        }
        return maxChildDepth + 1;
    }

    public DepdendenyNode<C> getOnlyChild() {
        if (this.children == null || this.children.isEmpty()) {
            return null;
        }
        if (this.children.size() > 1) {
            throw new IllegalStateException(this + " has "
                    + this.children.size() + " children.");
        }
        return this.children.get(0);
    }
}
