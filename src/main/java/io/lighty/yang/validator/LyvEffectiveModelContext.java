/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator;

import io.lighty.yang.validator.simplify.SchemaTree;
import java.util.List;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;

public final class LyvEffectiveModelContext {
    private final EffectiveModelContext context;
    private final SchemaTree schemaTree;
    private final List<Module> testedModules;

    public LyvEffectiveModelContext(final EffectiveModelContext context, final SchemaTree schemaTree,
            final List<Module> testedModules) {
        this.context = context;
        this.schemaTree = schemaTree;
        this.testedModules = testedModules;
    }

    public LyvEffectiveModelContext(final EffectiveModelContext context, final SchemaTree schemaTree) {
        this(context, schemaTree, List.of());
    }

    public EffectiveModelContext context() {
        return context;
    }

    public SchemaTree schemaTree() {
        return schemaTree;
    }

    public List<Module> testedModules() {
        return testedModules;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final LyvEffectiveModelContext context1 = (LyvEffectiveModelContext) other;

        if (!context.equals(context1.context)) {
            return false;
        }
        if (!schemaTree.equals(context1.schemaTree)) {
            return false;
        }
        return testedModules.equals(context1.testedModules);
    }

    @Override
    public int hashCode() {
        int result = context.hashCode();
        result = 31 * result + schemaTree.hashCode();
        result = 31 * result + testedModules.hashCode();
        return result;
    }
}
