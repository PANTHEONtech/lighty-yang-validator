/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.simplify;

import io.lighty.yang.validator.formats.utility.LyvStack;
import io.lighty.yang.validator.simplify.stream.TrackingXmlParserStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlCodecFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.ActionNodeContainer;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;

public class SchemaSelector {

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();
    private final EffectiveModelContext effectiveModelContext;
    private final SchemaTree tree;
    @SuppressWarnings("UnstableApiUsage")
    private final XmlCodecFactory codecs;

    @SuppressWarnings("UnstableApiUsage")
    public SchemaSelector(final EffectiveModelContext effectiveModelContext) {
        this.effectiveModelContext = effectiveModelContext;
        codecs = XmlCodecFactory.create(effectiveModelContext);
        tree = new SchemaTree(SchemaTree.ROOT, null,
                false, false, null);
    }

    public void addXml(final InputStream xml) throws XMLStreamException, IOException, URISyntaxException {
        fillUsedSchema(xml, tree);
    }

    public SchemaTree getSchemaTree() {
        return tree;
    }

    private void fillUsedSchema(final InputStream input, final SchemaTree st)
            throws XMLStreamException, IOException, URISyntaxException {
        final XMLStreamReader reader = FACTORY.createXMLStreamReader(input);
        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        try (var xmlParser = new TrackingXmlParserStream(streamWriter, codecs, effectiveModelContext, true, st)) {
            xmlParser.parse(reader);
        }
    }

    public void noXml() {
        final var stack = new LyvStack();

        for (final Module module : effectiveModelContext.getModules()) {
            for (final DataSchemaNode node : module.getChildNodes()) {
                resolveChildNodes(tree, node, true, false, stack);
                stack.clear();
            }

            for (final AugmentationSchemaNode aug : module.getAugmentations()) {
                stack.enter(aug.getTargetPath());
                for (final DataSchemaNode node : aug.getChildNodes()) {
                    resolveChildNodes(tree, node, true, true, stack);
                }
                stack.clear();
            }
        }
    }

    private void resolveChildNodes(final SchemaTree schemaTree, final DataSchemaNode node, final boolean rootNode,
            final boolean augNode, final LyvStack stack) {
        stack.enter(node);
        SchemaTree childSchemaTree = schemaTree.addChild(node, rootNode, augNode, stack);
        if (node instanceof DataNodeContainer) {
            for (final DataSchemaNode schemaNode : ((DataNodeContainer) node).getChildNodes()) {
                resolveChildNodes(childSchemaTree, schemaNode, false, false, stack);
            }
        } else if (node instanceof ChoiceSchemaNode) {
            for (final DataSchemaNode singleCase : ((ChoiceSchemaNode) node).getCases()) {
                resolveChildNodes(childSchemaTree, singleCase, false, false, stack);
            }
        }

        if (node instanceof ActionNodeContainer) {
            for (final ActionDefinition action : ((ActionNodeContainer) node).getActions()) {
                stack.enter(action);
                childSchemaTree = childSchemaTree.addChild(action, false, false, stack);
                resolveChildNodes(childSchemaTree, action.getInput(), false, false, stack);
                resolveChildNodes(childSchemaTree, action.getOutput(), false, false, stack);
                stack.exit();
            }
        }
        stack.exit();
    }
}

