/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlCodecFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.meta.DataSchemaCompat;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ActionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AugmentEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ChoiceEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DataTreeAwareEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DataTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeAwareEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeEffectiveStatement;

public class SchemaSelector {

    private static final String OUTPUT_TEXT = "output";
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
        final NormalizationResultHolder result = new NormalizationResultHolder();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        try (var xmlParser = new TrackingXmlParserStream(streamWriter, codecs, effectiveModelContext, true, st)) {
            xmlParser.parse(reader);
        }
    }

    public void noXml() {
        final var stack = new LyvStack();

        for (final Module module : effectiveModelContext.getModules()) {
            for (final SchemaTreeEffectiveStatement<?> statement : dataChildren(module.asEffectiveStatement())) {
                resolveChildNodes(tree, statement, true, false, stack, true);
                stack.clear();
            }

            for (final AugmentEffectiveStatement aug
                    : module.asEffectiveStatement().collectEffectiveSubstatements(AugmentEffectiveStatement.class)) {
                stack.enter(aug.argument());
                final boolean augmentConfig = isAugmentConfig(aug);
                for (final SchemaTreeEffectiveStatement<?> statement : dataChildren(aug)) {
                    resolveChildNodes(tree, statement, true, true, stack, augmentConfig);
                }
                stack.clear();
            }
        }
    }

    /**
     * Resolves {@code node}'s config, and adds it to {@code schemaTree}, then recurses into its children.
     * {@code node} itself is used for structure (type/name/description), while config is looked up fresh through
     * the effective model context via the node's own schema-tree position (which includes {@code node} itself,
     * since {@code stack.enter(node)} happens first) - a node reached only through an augmentation's own child
     * tree does not have its own effectiveConfig() applicable (same as inside a grouping), so looking it up this
     * way instead of calling {@code node.effectiveConfig()} directly is what makes an explicit {@code config
     * false;} on an augmented descendant visible. {@code ambientConfig} is the fallback used when that lookup is
     * absent (e.g. deviated away) or does not resolve a config value of its own.
     */
    private void resolveChildNodes(final SchemaTree schemaTree, final SchemaTreeEffectiveStatement<?> statement,
            final boolean rootNode, final boolean augNode, final LyvStack stack, final boolean ambientConfig) {
        stack.enter(statement);
        final boolean isConfig = resolveEffectiveConfig(stack).orElse(ambientConfig);
        SchemaTree childSchemaTree = schemaTree.addChild(statement, rootNode, augNode, stack, isConfig);
        if (statement instanceof SchemaTreeAwareEffectiveStatement<?, ?>) {
            for (final SchemaTreeEffectiveStatement<?> child : dataChildren(statement)) {
                resolveChildNodes(childSchemaTree, child, false, false, stack, isConfig);
            }
        }

        for (final ActionDefinition action : actionChildren(statement)) {
            stack.enter(action.asEffectiveStatement());
            childSchemaTree = childSchemaTree.addChild(action, false, false, stack);
            resolveChildNodes(childSchemaTree, action.getInput().asEffectiveStatement(), false, false, stack,
                    true);
            resolveChildNodes(childSchemaTree, action.getOutput().asEffectiveStatement(), false, false, stack,
                    true);
            stack.exit();
        }
        stack.exit();
    }

    // Excludes ActionEffectiveStatement (also a SchemaTreeEffectiveStatement) - handled by actionChildren().
    private static List<SchemaTreeEffectiveStatement<?>> dataChildren(final EffectiveStatement<?, ?> statement) {
        if (!(statement instanceof SchemaTreeAwareEffectiveStatement<?, ?> aware)) {
            return List.of();
        }
        final List<SchemaTreeEffectiveStatement<?>> children = new ArrayList<>();
        for (final SchemaTreeEffectiveStatement<?> child : aware.schemaTreeNodes()) {
            if (child instanceof DataTreeEffectiveStatement<?> || child instanceof ChoiceEffectiveStatement
                    || child instanceof CaseEffectiveStatement) {
                children.add(child);
            }
        }
        return children;
    }

    private static List<ActionDefinition> actionChildren(final EffectiveStatement<?, ?> statement) {
        if (!(statement instanceof SchemaTreeAwareEffectiveStatement<?, ?> aware)) {
            return List.of();
        }
        final List<ActionDefinition> actions = new ArrayList<>();
        for (final SchemaTreeEffectiveStatement<?> child : aware.schemaTreeNodes()) {
            if (child instanceof ActionEffectiveStatement && child instanceof ActionDefinition actionDefinition) {
                actions.add(actionDefinition);
            }
        }
        return actions;
    }

    /**
     * Looks up {@code stack}'s current position through the effective model context's schema tree (which,
     * unlike {@link EffectiveModelContext#findDataTreeChild}, addresses choice/case directly instead of skipping
     * over them), returning its effectiveConfig() if resolved to a real, correctly-positioned DataSchemaNode.
     */
    private Optional<Boolean> resolveEffectiveConfig(final LyvStack stack) {
        return effectiveModelContext.findSchemaTreeNode(stack.toSchemaNodeIdentifier())
                .filter(DataSchemaCompat.class::isInstance)
                .map(statement -> ((DataSchemaCompat<?, ?>) statement).toDataSchemaNode())
                .flatMap(DataSchemaNode::effectiveConfig);
    }

    private boolean isAugmentConfig(final AugmentEffectiveStatement augmentation) {
        Collection<? extends ActionDefinition> actions = List.of();
        boolean isAction = false;
        boolean initialized = false;
        DataTreeAwareEffectiveStatement<?, ?> current = null;
        for (final QName path : augmentation.argument().getNodeIdentifiers()) {
            if (isAction) {
                return !OUTPUT_TEXT.equals(path.getLocalName());
            }
            if (shouldSkipThisIteration(actions, path)) {
                isAction = true;
                continue;
            }

            if (!initialized) {
                current = effectiveModelContext.findModule(path.getModule()).map(Module::asEffectiveStatement)
                        .orElse(null);
                initialized = true;
            }
            final Optional<DataTreeEffectiveStatement<?>> child = current == null
                    ? Optional.empty() : current.findDataTreeNode(path);

            if (child.isPresent()) {
                final DataTreeEffectiveStatement<?> dataTreeChild = child.orElseThrow();
                if (dataTreeChild instanceof DataSchemaCompat<?, ?> compat) {
                    final Optional<Boolean> isConfig = compat.toDataSchemaNode().effectiveConfig();
                    if (isConfig.isPresent() && !isConfig.orElseThrow()) {
                        return false;
                    }
                }
                actions = actionChildren(dataTreeChild);
                current = dataTreeChild instanceof DataTreeAwareEffectiveStatement<?, ?> aware ? aware : null;
            }
        }
        return true;
    }

    private static boolean shouldSkipThisIteration(final Collection<? extends ActionDefinition> actions,
            final QName path) {
        for (final ActionDefinition action : actions) {
            if (action.getQName().getLocalName().equals(path.getLocalName())) {
                return true;
            }
        }
        return false;
    }
}

