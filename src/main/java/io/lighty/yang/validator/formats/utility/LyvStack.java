/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats.utility;

import java.util.ArrayDeque;
import java.util.Deque;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

/**
 * A simple stack for tracking where we are in the schema tree.
 */
public final class LyvStack {
    private final Deque<QName> qnames = new ArrayDeque<>();

    public void clear() {
        qnames.clear();
    }

    public void enter(final SchemaNodeIdentifier path) {
        if (path instanceof Absolute) {
            clear();
        }
        path.getNodeIdentifiers().forEach(qnames::addLast);
    }

    public void enter(final SchemaNode node) {
        qnames.addLast(node.getQName());
    }

    public void exit() {
        qnames.removeLast();
    }

    public @NonNull Absolute toSchemaNodeIdentifier() {
        return Absolute.of(qnames);
    }
}
