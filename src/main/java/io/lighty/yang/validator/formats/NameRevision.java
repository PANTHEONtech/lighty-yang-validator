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
import java.util.Optional;
import io.lighty.yang.validator.exceptions.NotFoundException;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings("SLF4J_FORMAT_SHOULD_BE_CONST")
public class NameRevision extends FormatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(NameRevision.class);
    private static final String HELP_NAME = "name-revision";
    private static final String HELP_DESCRIPTION = "return file name in a <name>@<revision> format";
    private static final String ET = "@";

    @Override
    public void emitFormat() {
        for (final RevisionSourceIdentifier source : this.sources) {
            final Module module = this.schemaContext.findModule(source.getName(), source.getRevision())
                    .orElseThrow(() -> new NotFoundException("Module " + source.getName() + " not found."));
            final Optional<Revision> revision = module.getRevision();
            String moduleName = module.getName();
            if (revision.isPresent()) {
                moduleName += ET + revision.get().toString();
            }
            LOG.info(moduleName);
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
