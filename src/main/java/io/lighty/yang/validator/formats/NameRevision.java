/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.yang.validator.GroupArguments;
import io.lighty.yang.validator.exceptions.NotFoundException;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NameRevision extends FormatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(NameRevision.class);
    private static final String HELP_NAME = "name-revision";
    private static final String HELP_DESCRIPTION = "return file name in a <name>@<revision> format";
    private static final String ET = "@";

    @Override
    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    public void emitFormat() {
        for (final SourceIdentifier source : this.sources) {
            final Module module = this.schemaContext.findModule(source.name().getLocalName(), source.revision())
                    .orElseThrow(() -> new NotFoundException("Module", source.name().getLocalName()));
            final Optional<Revision> revision = module.getRevision();
            String moduleName = module.getName();
            if (revision.isPresent()) {
                moduleName += ET + revision.get();
            }
            LOG.info("{}", moduleName);
        }
    }

    @Override
    public Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        return Optional.empty();
    }
}
