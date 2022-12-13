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

public record LyvEffectiveModelContext(EffectiveModelContext context, SchemaTree schemaTree,
                                       List<Module> testedModules) {
    public LyvEffectiveModelContext(final EffectiveModelContext context, final SchemaTree schemaTree) {
        this(context, schemaTree, List.of());
    }
}
