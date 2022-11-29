/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.yang.validator.GroupArguments;
import io.lighty.yang.validator.config.DependConfiguration;
import io.lighty.yang.validator.exceptions.NotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.sourceforge.argparse4j.impl.choice.CollectionArgumentChoice;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.api.ModuleLike;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Depends extends FormatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(Depends.class);
    private static final String HELP_NAME = "depend";
    private static final String HELP_DESCRIPTION = "return all module`s dependencies";
    private static final String MODULE = "module ";
    private static final String AT = "@";
    private static final String DEPENDS_TEXT = " depends on following modules: ";
    private static final String SPACE = " ";
    private static final String NON_RECURSIVE = "(not recursively)";
    private static final String ONLY_MODULES = "(Imports only)";
    private static final String ONLY_SUBMODULES = "(Submodules only)";

    private final Set<String> modules = new HashSet<>();

    @Override
    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    public void emitFormat() {
        final DependConfiguration dependConfiguration = configuration.getDependConfiguration();
        for (final SourceIdentifier source : sources) {
            final Module module = schemaContext.findModule(source.name().getLocalName(), source.revision())
                    .orElseThrow(() -> new NotFoundException("Module", source.name().getLocalName()));
            final StringBuilder dependantsBuilder = new StringBuilder(MODULE);
            dependantsBuilder.append(module.getName())
                    .append(AT);
            module.getRevision().ifPresent(dependantsBuilder::append);

            dependantsBuilder.append(DEPENDS_TEXT);
            if (!dependConfiguration.isModuleImportsOnly()) {
                resolveSubmodules(module, dependConfiguration);
            }
            if (!dependConfiguration.isModuleIncludesOnly()) {
                resolveImports(module, dependConfiguration);
            }
            for (final String name : modules) {
                dependantsBuilder.append(name)
                        .append(SPACE);
            }
            if (dependConfiguration.isModuleImportsOnly()) {
                dependantsBuilder.append(ONLY_MODULES);
            } else if (dependConfiguration.isModuleIncludesOnly()) {
                dependantsBuilder.append(ONLY_SUBMODULES);
            }
            if (dependConfiguration.isModuleDependentsOnly()) {
                dependantsBuilder.append(NON_RECURSIVE);
            }
            final String dependandsText = dependantsBuilder.toString();
            LOG.info("{}", dependandsText);
        }
    }

    private void resolveImports(final ModuleLike module, final DependConfiguration dependConfiguration) {
        for (final ModuleImport moduleImport : module.getImports()) {
            final String moduleName = moduleImport.getModuleName().getLocalName();
            if (dependConfiguration.getExcludedModuleNames().contains(moduleName)) {
                continue;
            }
            resolveImportsInSchemaContextModules(dependConfiguration, moduleImport, moduleName);
        }
    }

    private void resolveImportsInSchemaContextModules(final DependConfiguration dependConfiguration,
            final ModuleImport moduleImport, final String moduleName) {
        for (final Module contextModule : schemaContext.getModules()) {
            if (moduleName.equals(contextModule.getName()) && isRevisionsEqualsOrNull(moduleImport, contextModule)) {
                addContextModuleToModulesAndResolveImports(dependConfiguration, contextModule);
                break;
            }
        }
    }

    private void addContextModuleToModulesAndResolveImports(final DependConfiguration dependConfiguration,
            final Module contextModule) {
        modules.add(contextModule.getName());
        if (!dependConfiguration.isModuleDependentsOnly()) {
            if (!dependConfiguration.isModuleImportsOnly()) {
                resolveSubmodules(contextModule, dependConfiguration);
            }
            resolveImports(contextModule, dependConfiguration);
        }
    }

    private static boolean isRevisionsEqualsOrNull(final ModuleImport moduleImport, final Module contextModule) {
        final Revision moduleImportRevision = moduleImport.getRevision().orElse(null);
        final Revision contextModuleRevision = contextModule.getRevision().orElse(null);
        return moduleImportRevision == null || contextModuleRevision == null
                || contextModuleRevision.toString().equals(moduleImportRevision.toString());
    }

    private void resolveSubmodules(final ModuleLike module, final DependConfiguration dependConfiguration) {
        final StringBuilder dependantsBuilder = new StringBuilder();
        for (final ModuleLike subModule : module.getSubmodules()) {
            final String moduleName = subModule.getName();
            if (dependConfiguration.getExcludedModuleNames().contains(moduleName)) {
                continue;
            }
            if (dependantsBuilder.length() > 0) {
                dependantsBuilder.append(' ');
            }
            dependantsBuilder.append(moduleName);
            final Optional<Revision> revision = subModule.getRevision();
            if (revision.isPresent()) {
                dependantsBuilder
                        .append(AT)
                        .append(revision.get());
            }
            final String moduleWithRevision = dependantsBuilder.toString();
            if (dependConfiguration.getExcludedModuleNames().contains(moduleWithRevision)) {
                continue;
            }
            modules.add(moduleWithRevision);
            dependantsBuilder.setLength(0);
            if (!dependConfiguration.isModuleDependentsOnly()) {
                if (!dependConfiguration.isModuleIncludesOnly()) {
                    resolveImports(subModule, dependConfiguration);
                }
                resolveSubmodules(subModule, dependConfiguration);
            }
        }
    }

    @Override
    public Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        final GroupArguments groupArguments = new GroupArguments(HELP_NAME,
                "Depend format based arguments: ");
        groupArguments.addOption("List dependencies of the module only (do not look"
                        + "recursively in other imported, included modules).",
                Collections.singletonList("--module-depends-only"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("List module dependencies of the module"
                        + " (only imports, no submodules will be included).",
                Collections.singletonList("--modules-only"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("List submodule dependencies of the module"
                        + " (only includes, no imported modules will be included).",
                Collections.singletonList("--submodules-only"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("List dependencies of the module"
                        + " but exclude any module listed in this option. Warning -"
                        + " This will exclude all the (sub)modules in the excluded module"
                        + " as well.",
                Collections.singletonList("--exclude-module-name"), false, "*", Collections.emptyList(),
                new CollectionArgumentChoice<>(Collections.emptyList()), List.class);
        return Optional.of(groupArguments);
    }
}
