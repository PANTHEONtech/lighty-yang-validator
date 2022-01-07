/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.simplify;

import io.lighty.yang.validator.simplify.stream.TrackingXmlParserStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collection;
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
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

public class SchemaSelector {

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();
    private final EffectiveModelContext effectiveModelContext;
    private final SchemaTree tree;
    @SuppressWarnings("UnstableApiUsage")
    private final XmlCodecFactory codecs;

    @SuppressWarnings("UnstableApiUsage")
    public SchemaSelector(final EffectiveModelContext effectiveModelContext) {
        this.effectiveModelContext = effectiveModelContext;
        this.codecs = XmlCodecFactory.create(effectiveModelContext);
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
        final TrackingXmlParserStream xmlParser =
                new TrackingXmlParserStream(streamWriter, codecs, effectiveModelContext, true, st);
        xmlParser.parse(reader);
        xmlParser.flush();
        xmlParser.close();
    }

    public void noXml() {
        SchemaInferenceStack schemaInferenceStack = SchemaInferenceStack.of(effectiveModelContext);
        for (Module module : effectiveModelContext.getModules()) {
            for (DataSchemaNode node : module.getChildNodes()) {
                resolveChildNodes(tree, node, true, false, schemaInferenceStack);
            }

            for (AugmentationSchemaNode aug : module.getAugmentations()) {
                schemaInferenceStack.enterSchemaTree(aug.getTargetPath());
                for (DataSchemaNode node : aug.getChildNodes()) {
                    resolveChildNodes(tree, node, true, true, schemaInferenceStack);
                }
                schemaInferenceStack.clear();
            }
        }
    }

    private void resolveChildNodes(final SchemaTree schemaTree, final DataSchemaNode node, final boolean rootNode,
            final boolean augNode, final SchemaInferenceStack schemaInferenceStack) {

        schemaInferenceStack.enterSchemaTree(node.getQName());
        SchemaTree childSchemaTree = schemaTree.addChild(node, rootNode, augNode,
                schemaInferenceStack.toSchemaNodeIdentifier());
        if (node instanceof DataNodeContainer) {
            for (final DataSchemaNode schemaNode : ((DataNodeContainer) node).getChildNodes()) {
                resolveChildNodes(childSchemaTree, schemaNode, false, false, schemaInferenceStack);
            }
        } else if (node instanceof ChoiceSchemaNode) {
            final Collection<? extends CaseSchemaNode> cases = ((ChoiceSchemaNode) node).getCases();
            for (final DataSchemaNode singelCase : cases) {
                resolveChildNodes(childSchemaTree, singelCase, false, false, schemaInferenceStack);
            }
        }

        if (node instanceof ActionNodeContainer) {
            final Collection<? extends ActionDefinition> actions = ((ActionNodeContainer) node).getActions();
            for (final ActionDefinition action : actions) {
                schemaInferenceStack.enterSchemaTree(action.getQName());
                childSchemaTree = childSchemaTree.addChild(action, false, false,
                        schemaInferenceStack.toSchemaNodeIdentifier());
                if (action.getInput() != null) {
                    resolveChildNodes(childSchemaTree, action.getInput(), false, false,
                            schemaInferenceStack);
                }
                if (action.getOutput() != null) {
                    resolveChildNodes(childSchemaTree, action.getOutput(), false, false,
                            schemaInferenceStack);
                }
                schemaInferenceStack.exit();
            }
        }
        schemaInferenceStack.exit();
    }
}

