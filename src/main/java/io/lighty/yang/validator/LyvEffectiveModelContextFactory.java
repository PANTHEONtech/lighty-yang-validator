/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator;

import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.exceptions.LyvApplicationException;
import java.io.IOException;
import java.util.List;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;

public final class LyvEffectiveModelContextFactory {

    private LyvEffectiveModelContextFactory() {
        // hidden on purpose
    }

    public static LyvEffectiveModelContext create(final List<String> yangFiles, final Configuration config)
            throws LyvApplicationException {
        if (yangFiles.isEmpty() && config.getTreeConfiguration().isHelp()) {
            return new LyvEffectiveModelContext(null);
        }
        final var contextFactory = new YangContextFactory(config.getPath(), yangFiles, config.getSupportedFeatures(),
                config.isRecursive());
        final EffectiveModelContext context;
        try {
            context = contextFactory.createContext(config.getSimplify() != null);
            return new LyvEffectiveModelContext(context, contextFactory.getModulesForTesting());
        } catch (final IOException | YangParserException e) {
            throw new LyvApplicationException("Failed to assemble Effective Model Context", e);
        }
    }
}
