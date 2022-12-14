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
import io.lighty.yang.validator.simplify.SchemaSelector;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;

public final class LyvEffectiveModelContextFactory {

    private LyvEffectiveModelContextFactory() {
        // hidden on purpose
    }

    // TODO encapsulate all logic here and remove other 'create' methods
    public static LyvEffectiveModelContext create(final Configuration config) throws LyvApplicationException {
        return create(config.getYang(), config);
    }

    public static LyvEffectiveModelContext create(final String yangFile, final Configuration config)
            throws LyvApplicationException {
        return create(List.of(yangFile), config);
    }

    public static LyvEffectiveModelContext create(final List<String> yangFiles, final Configuration config)
            throws LyvApplicationException {
        if (yangFiles.isEmpty() && config.getTreeConfiguration().isHelp()) {
            return new LyvEffectiveModelContext(null, null);
        }
        final var contextFactory = new YangContextFactory(config.getPath(), yangFiles, config.getSupportedFeatures(),
                config.isRecursive());
        final EffectiveModelContext context;
        try {
            context = contextFactory.createContext(config.getSimplify() != null);
        } catch (final IOException | YangParserException e) {
            throw new LyvApplicationException("Failed to assemble Effective Model Context", e);
        }
        final var schemaTree = resolveSchemaTree(config.getSimplify(), context);
        return new LyvEffectiveModelContext(context, schemaTree, contextFactory.getModulesForTesting());
    }

    private static SchemaTree resolveSchemaTree(final String simplifyDir,
            final EffectiveModelContext effectiveModelContext) throws LyvApplicationException {
        final SchemaSelector schemaSelector = new SchemaSelector(effectiveModelContext);
        if (simplifyDir == null) {
            schemaSelector.noXml();
        } else {
            try (Stream<Path> path = Files.list(Paths.get(simplifyDir))) {
                final List<File> xmlFiles = path
                        .map(Path::toFile)
                        .collect(Collectors.toList());

                addXmlFilesToSchemaSelector(schemaSelector, xmlFiles);
            } catch (final IOException e) {
                throw new LyvApplicationException("Failed to open xml files", e);
            }
        }
        return schemaSelector.getSchemaTree();
    }

    private static void addXmlFilesToSchemaSelector(final SchemaSelector schemaSelector, final List<File> xmlFiles)
            throws LyvApplicationException {
        for (final File xmlFile : xmlFiles) {
            try (FileInputStream fis = new FileInputStream(xmlFile)) {
                schemaSelector.addXml(fis);
            } catch (final IOException | XMLStreamException | URISyntaxException e) {
                throw new LyvApplicationException(
                        String.format("Failed to fill schema from %s", xmlFile), e);
            }
        }
    }
}
