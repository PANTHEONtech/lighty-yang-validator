/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.util.Collection;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;

public interface Emitter {

    /**
     * Initialize option that emits some output.
     *
     * @param config                 all the configuration chosen by user
     * @param context                yang schema context
     * @param schemaTree             Tree representation of schema nodes
     */
    void init(Configuration config, EffectiveModelContext context, SchemaTree schemaTree);

    /**
     * Create logic and emit output.
     *
     * @param module                 module to emit
     */
    void emit(Module module);

    /**
     * Close after printing out the module.
     *
     * @param modules collection of {@link Module} objects
     */
    void close(Collection<Module> modules);
}
