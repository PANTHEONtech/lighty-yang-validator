/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import io.lighty.yang.validator.GroupArguments;
import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;

public abstract class FormatPlugin {

    EffectiveModelContext schemaContext;
    List<SourceIdentifier> sources;
    SchemaTree schemaTree;
    Path output;
    Configuration configuration;

    void init(final EffectiveModelContext context, final List<SourceIdentifier> testFilesSchemaSources,
            final SchemaTree tree, final Configuration config) {
        this.schemaContext = context;
        this.sources = testFilesSchemaSources;
        this.schemaTree = tree;
        this.configuration = config;
        final String out = config.getOutput();
        if (out == null) {
            this.output = null;
        } else {
            this.output = Paths.get(out);
        }
    }

    /**
     * Logic of the plugin. Use logger to print
     */
    abstract void emitFormat();

    /**
     * This serves to generate help about current plugin, in case that user will use --help option with `lyv` command.
     *
     * @return instance of Help object that will contain name of the format with its description
     */
    abstract Help getHelp();

    /**
     * This serves to resolve configurations based on specific format.
     */
    public abstract Optional<GroupArguments> getGroupArguments();
}
