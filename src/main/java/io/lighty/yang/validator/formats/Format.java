/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import io.lighty.yang.validator.GroupArguments;
import io.lighty.yang.validator.LyvParameters;
import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;

public class Format implements Emitter, CommandLineOptions {

    private final List<FormatPlugin> formatPlugins = new ArrayList<>();
    private FormatPlugin usedFormat;

    public Format(final List<FormatPlugin> fp) {
        this.formatPlugins.addAll(fp);
    }

    public void createOptions(final LyvParameters lyvParameters) {
        final StringBuilder helpBuilder = new StringBuilder();
        for (final FormatPlugin plugin : this.formatPlugins) {
            final Help help = plugin.getHelp();
            helpBuilder.append(help.generateFromatHelp());
            final Optional<GroupArguments> groupArguments = plugin.getGroupArguments();
            if (groupArguments.isPresent()) {
                lyvParameters.addGroupArguments(groupArguments.get());
            }
        }
        lyvParameters.addFormatArgument(helpBuilder.toString());
    }

    @Override
    public void init(final Configuration config, final EffectiveModelContext context,
            final List<SourceIdentifier> testFilesSchemaSources,
            final SchemaTree schemaTree) {
        final String format = config.getFormat();
        for (final FormatPlugin plugin : this.formatPlugins) {
            if (plugin.getHelp().getName().equals(format)) {
                this.usedFormat = plugin;
                this.usedFormat.init(context, testFilesSchemaSources, schemaTree, config);
            }
        }
    }

    public void emit() {
        this.usedFormat.emitFormat();
    }
}
