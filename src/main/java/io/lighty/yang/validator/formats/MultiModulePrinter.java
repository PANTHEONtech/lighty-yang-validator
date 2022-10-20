/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import io.lighty.yang.validator.GroupArguments;
import io.lighty.yang.validator.exceptions.NotFoundException;
import io.lighty.yang.validator.formats.yang.printer.ModulePrinter;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.UnresolvedQName.Unqualified;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiModulePrinter extends FormatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(MultiModulePrinter.class);
    private static final String HELP_NAME = "yang";
    private static final String MODULE_STRING = "Module";
    private static final String HELP_DESCRIPTION = "print raw yang module according to RFC7950";

    private final Map<QNameModule, Set<TypeDefinition<?>>> usedImportedTypeDefs = new HashMap<>();
    private final Map<QNameModule, Set<String>> usedImports = new HashMap<>();
    private final Map<QNameModule, Set<SchemaTree>> subtrees = new HashMap<>();

    @Override
    protected void emitFormat() {
        splitTree(this.schemaTree);
        if (this.output != null) {
            try {
                Files.createDirectories(this.output);
            } catch (final IOException e) {
                LOG.error("Can not create directory {}", this.output, e);
            }
        }
        //resolve imports by augmentations and typedefs
        resolveAugmentationsImports();

        for (final QNameModule name : this.usedImportedTypeDefs.keySet()) {
            subtrees.putIfAbsent(name, Collections.emptySet());
        }
        //print each yang module
        printEachYangModule();
    }

    private void printEachYangModule() {
        for (final Map.Entry<QNameModule, Set<SchemaTree>> entry : subtrees.entrySet()) {
            final Module module = this.schemaContext.findModule(entry.getKey())
                    .orElseThrow(() -> new NotFoundException(MODULE_STRING, entry.getKey().toString()));
            final String withRev = "@" + module.getRevision()
                    .orElseThrow(() -> new NotFoundException("Revision of module", module.getName()));
            final String suffix = module.getRevision().isPresent() ? withRev + ".yang" : ".yang";
            final String name = module.getName() + suffix;
            if (!this.sources.contains(
                    new SourceIdentifier(Unqualified.of(module.getName()), module.getRevision().get()))
                    && !this.sources.isEmpty()) {
                continue;
            }
            final ModulePrinter modulePrinter;
            if (this.output == null) {
                LOG.info("\n\nprinting yang module {}\n", name);
                modulePrinter = new ModulePrinter(entry.getValue(), this.schemaContext, entry.getKey(), LOG,
                        this.usedImportedTypeDefs.computeIfAbsent(module.getQNameModule(), k -> new HashSet<>()),
                        this.usedImports.computeIfAbsent(module.getQNameModule(), k -> new HashSet<>()));
                modulePrinter.printYang();
            } else {
                try (OutputStream os = new FileOutputStream(this.output.resolve(name).toFile())) {
                    modulePrinter = new ModulePrinter(entry.getValue(), this.schemaContext, entry.getKey(), os,
                            this.usedImportedTypeDefs.computeIfAbsent(module.getQNameModule(), k -> new HashSet<>()),
                            this.usedImports.computeIfAbsent(module.getQNameModule(), k -> new HashSet<>()));
                    modulePrinter.printYang();
                } catch (final IOException e) {
                    LOG.error("Can not create file {}", this.output.resolve(name).toFile().getAbsolutePath(), e);
                }
            }
        }
    }

    private void resolveAugmentationsImports() {
        for (final Map.Entry<QNameModule, Set<SchemaTree>> entry : subtrees.entrySet()) {
            final Module module = this.schemaContext.findModule(entry.getKey())
                    .orElseThrow(() -> new NotFoundException(MODULE_STRING, entry.getKey().toString()));
            for (final SchemaTree singleEntry : entry.getValue()) {
                gatherUsedTypeDefs(singleEntry, module);
            }
            for (final AugmentationSchemaNode aug : module.getAugmentations()) {
                for (final QName pathQname : aug.getTargetPath().getNodeIdentifiers()) {
                    this.usedImports.computeIfAbsent(module.getQNameModule(), k -> new HashSet<>())
                            .add(this.schemaContext.findModule(pathQname.getModule())
                                    .orElseThrow(
                                            () -> new NotFoundException(MODULE_STRING,
                                                    pathQname.getModule().toString()))
                                    .getName());
                }
            }
        }
    }

    private static TypeDefinition<?> getRootType(final TypeDefinition<?> typeDefinition) {
        TypeDefinition<?> typeDef = typeDefinition;
        while (typeDef.getBaseType() != null) {
            typeDef = typeDef.getBaseType();
        }
        return typeDef;
    }

    private void gatherUsedTypeDefs(final SchemaTree tree, final Module module) {
        if (tree.getSchemaNode() instanceof TypedDataSchemaNode) {
            final TypeDefinition<? extends TypeDefinition<?>> type =
                    ((TypedDataSchemaNode) tree.getSchemaNode()).getType();
            resolveType(type, module);

        }
        for (final SchemaTree child : tree.getChildren()) {
            gatherUsedTypeDefs(child, module);
        }
    }

    private void resolveType(final TypeDefinition<? extends TypeDefinition<?>> type, final Module module) {
        final TypeDefinition<?> rootType = getRootType(type);
        final String rootLocalName = rootType.getQName().getLocalName();
        if (!Objects.equals(rootLocalName, type.getQName().getLocalName()) && !rootLocalName.equals("boolean")) {
            final QNameModule mod = QNameModule.create(type.getQName().getNamespace(), type.getQName().getRevision());
            usedImportedTypeDefs.computeIfAbsent(mod, k -> new TreeSet<>(Comparator.comparing(SchemaNode::getQName)))
                    .add(type);
            usedImports.computeIfAbsent(module.getQNameModule(), k -> new HashSet<>())
                    .add(this.schemaContext.findModule(mod)
                            .orElseThrow(() -> new NotFoundException(MODULE_STRING, mod.toString()))
                            .getName());

        }
        if (type instanceof UnionTypeDefinition) {
            final List<TypeDefinition<?>> types = ((UnionTypeDefinition) type).getTypes();
            for (final TypeDefinition<?> t : types) {
                resolveType(t, module);
            }
        }
    }

    private void splitTree(final SchemaTree tree) {
        for (final SchemaTree child : tree.getChildren()) {
            if (child.isRootNode()) {
                final QNameModule childModule = child.getQname().getModule();
                subtrees.computeIfAbsent(childModule, k -> new HashSet<>()).add(child);
            }
            splitTree(child);
        }
    }

    @Override
    protected Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        return Optional.empty();
    }
}

