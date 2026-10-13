/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.yang.validator.GroupArguments;
import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.formats.utility.LyvNodeData;
import io.lighty.yang.validator.formats.utility.LyvStack;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.meta.DataSchemaCompat;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ActionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AugmentEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ChoiceEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DataTreeAwareEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DataTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ListEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeAwareEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeEffectiveStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsTree extends FormatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(JsTree.class);
    private static final String HELP_NAME = "jstree";
    private static final String HELP_DESCRIPTION = "Prints out html, javascript tree of the modules";
    private static final String INPUT = "input";

    private Map<XMLNamespace, String> namespacePrefix = new HashMap<>();

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    @Override
    public void emitFormat(final Module module) {
        if (module != null) {
            namespacePrefix = new HashMap<>();
            final SingletonListInitializer singletonListInitializer = new SingletonListInitializer(1);

            // Nodes
            printLines(getChildNodesLines(singletonListInitializer, module));

            // Augmentations
            for (final AugmentEffectiveStatement augNode
                    : module.asEffectiveStatement().collectEffectiveSubstatements(AugmentEffectiveStatement.class)) {
                printLines(getAugmentationNodesLines(singletonListInitializer.getSingletonListWithIncreasedValue(),
                        augNode));
            }

            // Rpcs
            printLines(getRpcsLines(singletonListInitializer, module));

            // Notifications
            printLines(getNotificationsLines(singletonListInitializer, module));
            LOG.info("</table>");
            LOG.info("</div>");
        } else {
            LOG.error(EMPTY_MODULE_EXCEPTION);
        }
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    private static void printLines(final List<Line> lines) {
        for (final Line line : lines) {
            LOG.info("{}", line);
        }
    }

    private List<Line> getNotificationsLines(final SingletonListInitializer singletonListInitializer,
            final Module module) {
        final List<Line> lines = new ArrayList<>();
        final LyvStack stack = new LyvStack();
        for (final NotificationDefinition node : module.getNotifications()) {
            final var statement = node.asEffectiveStatement();
            stack.enter(statement);
            final List<Integer> ids = singletonListInitializer.getSingletonListWithIncreasedValue();
            final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, statement, stack);
            final HtmlLine htmlLine = new HtmlLine(new ArrayList<>(ids), lyvNodeData, RpcInputOutput.OTHER,
                    namespacePrefix);
            lines.add(htmlLine);
            resolveChildNodes(lines, new ArrayList<>(ids), statement, RpcInputOutput.OTHER, Collections.emptyList(),
                    stack, true);
            stack.exit();
        }
        return lines;
    }

    private List<Line> getRpcsLines(final SingletonListInitializer singletonListInitializer, final Module module) {
        final List<Line> lines = new ArrayList<>();
        final LyvStack stack = new LyvStack();
        for (final RpcDefinition node : module.getRpcs()) {
            stack.enter(node.asEffectiveStatement());
            final List<Integer> rpcId = singletonListInitializer.getSingletonListWithIncreasedValue();
            LyvNodeData lyvNodeData = new LyvNodeData(modelContext, node.asEffectiveStatement(), stack);
            HtmlLine htmlLine = new HtmlLine(rpcId, lyvNodeData, RpcInputOutput.OTHER, namespacePrefix);
            lines.add(htmlLine);
            final boolean inputExists = !node.getInput().getChildNodes().isEmpty();
            final boolean outputExists = !node.getOutput().getChildNodes().isEmpty();
            List<Integer> ids = new ArrayList<>(rpcId);
            if (inputExists) {
                ids.add(1);
                stack.enter(node.getInput().asEffectiveStatement());
                lyvNodeData = new LyvNodeData(modelContext, node.getInput().asEffectiveStatement(), stack);
                htmlLine = new HtmlLine(new ArrayList<>(ids), lyvNodeData, RpcInputOutput.INPUT, namespacePrefix);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(ids), node.getInput().asEffectiveStatement(),
                        RpcInputOutput.INPUT, Collections.emptyList(), stack, true);
                stack.exit();
            }
            ids = new ArrayList<>(rpcId);
            if (outputExists) {
                if (!inputExists) {
                    ids.add(1);
                } else {
                    ids.add(2);
                }
                stack.enter(node.getOutput().asEffectiveStatement());
                lyvNodeData = new LyvNodeData(modelContext, node.getOutput().asEffectiveStatement(), stack);
                htmlLine = new HtmlLine(new ArrayList<>(ids), lyvNodeData, RpcInputOutput.OUTPUT, namespacePrefix);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(ids), node.getOutput().asEffectiveStatement(),
                        RpcInputOutput.OUTPUT, Collections.emptyList(), stack, true);
                stack.exit();
            }
            stack.exit();
        }
        return lines;
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    private List<Line> getChildNodesLines(final SingletonListInitializer singletonListInitializer,
            final Module module) {
        final List<Line> lines = new ArrayList<>();
        final String headerText = prepareModule(module);
        LOG.info("{}", headerText);
        for (final Module m : modelContext.getModules()) {
            if (!m.getPrefix().equals(module.getPrefix())) {
                namespacePrefix.put(m.getNamespace(), m.getPrefix());
            }
        }

        final LyvStack stack = new LyvStack();
        for (final SchemaTreeEffectiveStatement<?> statement : dataChildren(module.asEffectiveStatement())) {
            stack.enter(statement);
            final List<Integer> ids = singletonListInitializer.getSingletonListWithIncreasedValue();
            final boolean isConfig = resolveEffectiveConfig(stack).orElse(Boolean.TRUE);
            final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, statement, stack, null, isConfig);
            final HtmlLine htmlLine = new HtmlLine(ids, lyvNodeData, RpcInputOutput.OTHER, namespacePrefix);
            lines.add(htmlLine);
            resolveChildNodes(lines, new ArrayList<>(ids), statement, RpcInputOutput.OTHER, Collections.emptyList(),
                    stack, isConfig);
            stack.exit();
        }
        return lines;
    }

    private List<Line> getAugmentationNodesLines(final List<Integer> ids, final AugmentEffectiveStatement augNode) {
        final List<Line> lines = new ArrayList<>();
        final LyvStack stack = new LyvStack();
        stack.enter(augNode.argument());
        final List<SchemaTreeEffectiveStatement<?>> augChildNodes = dataChildren(augNode);
        final SchemaTreeEffectiveStatement<?> firstStatement = augChildNodes.iterator().next();
        stack.enter(firstStatement);
        LyvNodeData lyvNodeData = new LyvNodeData(modelContext, firstStatement, stack);
        final HtmlLine htmlLine = new HtmlLine(new ArrayList<>(ids), lyvNodeData, RpcInputOutput.OTHER, namespacePrefix,
                augNode);
        lines.add(htmlLine);
        stack.exit();
        int modelAugmentationNumber = 1;
        for (final SchemaTreeEffectiveStatement<?> statement : augChildNodes) {
            stack.enter(statement);
            final RpcInputOutput inputOutputOther = getAugmentationRpcInputOutput(stack);
            ids.add(modelAugmentationNumber++);
            final boolean isConfig = resolveEffectiveConfig(stack).orElse(Boolean.TRUE);
            lyvNodeData = new LyvNodeData(modelContext, statement, stack, null, isConfig);
            final HtmlLine line = new HtmlLine(new ArrayList<>(ids), lyvNodeData, inputOutputOther, namespacePrefix);
            lines.add(line);
            resolveChildNodes(lines, new ArrayList<>(ids), statement, RpcInputOutput.OTHER, Collections.emptyList(),
                    stack, isConfig);
            ids.remove(ids.size() - 1);
            stack.exit();
        }
        return lines;
    }

    /**
     * Looks up {@code stack}'s current position through the effective model context's schema tree (which,
     * unlike {@link EffectiveModelContext#findDataTreeChild}, addresses choice/case directly instead of skipping
     * over them), returning its effectiveConfig() if resolved to a real, correctly-positioned DataSchemaNode.
     */
    private Optional<Boolean> resolveEffectiveConfig(final LyvStack stack) {
        return modelContext.findSchemaTreeNode(stack.toSchemaNodeIdentifier())
                .filter(DataSchemaCompat.class::isInstance)
                .map(statement -> ((DataSchemaCompat<?, ?>) statement).toDataSchemaNode())
                .flatMap(DataSchemaNode::effectiveConfig);
    }

    private RpcInputOutput getAugmentationRpcInputOutput(final LyvStack stack) {
        final List<QName> qnames = stack.toSchemaNodeIdentifier().getNodeIdentifiers();

        Collection<? extends ActionDefinition> actions = List.of();
        RpcInputOutput inputOutputOther = RpcInputOutput.OTHER;
        boolean initialized = false;
        DataTreeAwareEffectiveStatement<?, ?> current = null;
        for (int i = 1; i <= qnames.size(); i++) {
            final List<QName> qnamesCopy = qnames.subList(0, i);
            inputOutputOther = getRpcInputOutput(qnames, actions, inputOutputOther, i, qnamesCopy);
            final QName path = qnames.get(i - 1);
            if (!initialized) {
                current = modelContext.findModule(path.getModule()).map(Module::asEffectiveStatement).orElse(null);
                initialized = true;
            }
            final Optional<DataTreeEffectiveStatement<?>> dataTreeChild = current == null
                    ? Optional.empty() : current.findDataTreeNode(path);
            if (dataTreeChild.isPresent()) {
                final DataTreeEffectiveStatement<?> child = dataTreeChild.orElseThrow();
                actions = actionChildren(child);
                current = child instanceof DataTreeAwareEffectiveStatement<?, ?> aware ? aware : null;
            }
        }
        return inputOutputOther;
    }

    private static RpcInputOutput getRpcInputOutput(final List<QName> qnames,
            final Collection<? extends ActionDefinition> actions, final RpcInputOutput inputOutputOther,
            final int iteration, final List<QName> qnamesCopy) {
        if (actions.isEmpty()) {
            return inputOutputOther;
        }
        for (final ActionDefinition action : actions) {
            if (action.getQName().getLocalName().equals(qnamesCopy.get(qnamesCopy.size() - 1).getLocalName())) {
                if (INPUT.equals(qnames.get(iteration).getLocalName())) {
                    return RpcInputOutput.INPUT;
                } else {
                    return RpcInputOutput.OUTPUT;
                }
            }
        }
        return inputOutputOther;
    }

    private static String loadJS(final Collection<Module> modules) {
        final var url = Resources.getResource("js");
        var text = "";
        try {
            text = Resources.toString(url, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOG.error("Can not load text from js file");
        }

        // Find the index of the placeholder comment
        final var placeholderComment = "//AddTableLogic";
        final var commentIndex = text.indexOf(placeholderComment);

        if (commentIndex != -1) {
            // Insert the encapsulated code after the comment
            final var modifiedText = new StringBuilder(text);
            final var simpleTreeTableJQuery = getSimpleTreeTableJQuery(modules);
            modifiedText.insert(commentIndex + placeholderComment.length(), simpleTreeTableJQuery);
            text = modifiedText.toString();
        }

        return text;
    }

    private static String getSimpleTreeTableJQuery(final Collection<Module> modules) {
        final var moduleCode = new StringBuilder();
        for (final var module : modules) {
            // Define the multi-line string to add
            final var multiLineStringToAdd = """
                  $('#basic-%1$s').simpleTreeTable({
                    expander: $('#expander-%1$s'),
                    collapser: $('#collapser-%1$s')
                  });
                """.formatted(module.getName());
            moduleCode.append(multiLineStringToAdd);
        }

        // Encapsulate module code within $(document).ready(function () {...});
        return "\n$(document).ready(function () {\n" + moduleCode + "});\n";
    }

    private static String prepareHeader() {
        final var url = Resources.getResource("header");
        var text = "";
        try {
            text = Resources.toString(url, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOG.warn("Can not load text from header file. Ignored.", e);
        }

        return text;
    }

    private static String prepareModule(final Module module) {
        final var nameRevision = new StringBuilder(module.getName());
        module.getRevision().ifPresent(value -> nameRevision.append("@").append(value));
        final var url = Resources.getResource("module");
        var text = "";
        try {
            text = Resources.toString(url, StandardCharsets.UTF_8);
            text = text.replace("id=\"expander\"", "id=\"expander-" + module.getName() + "\"");
            text = text.replace("id=\"collapser\"", "id=\"collapser-" + module.getName() + "\"");
            text = text.replace("id=\"basic\"", "id=\"basic-" + module.getName() + "\"");
            text = text.replace("<NAME_REVISION>", nameRevision);
            text = text.replace("<NAMESPACE>", module.getNamespace().toString());
            text = text.replace("<PREFIX>", module.getPrefix());
        } catch (final IOException e) {
            LOG.warn("Can not load text from module file. Ignored.", e);
        }

        return text;
    }

    private void resolveChildNodes(final List<Line> lines, final List<Integer> connections,
            final SchemaTreeEffectiveStatement<?> statement, final RpcInputOutput inputOutput,
            final List<QName> keys, final LyvStack stack, final boolean ambientConfig) {
        if (statement instanceof ChoiceEffectiveStatement) {
            connections.add(0);
            resolveChoiceSchemaNode(dataChildren(statement).iterator(), lines, connections, inputOutput, stack,
                    ambientConfig);
        } else if (statement instanceof SchemaTreeAwareEffectiveStatement<?, ?>) {
            resolveDataNodeContainer(dataChildren(statement).iterator(), lines, connections, inputOutput, keys,
                    stack, ambientConfig);
        }
        final List<ActionDefinition> actions = actionChildren(statement);
        if (!actions.isEmpty()) {
            resolveActionNodeContainer(lines, connections, actions, stack);
        }
    }

    private void resolveActionNodeContainer(final List<Line> lines, final List<Integer> connections,
            final List<ActionDefinition> actions, final LyvStack stack) {
        for (final ActionDefinition action : actions) {
            final int id = 1;
            connections.add(0);
            connections.set(connections.size() - 1, id);
            stack.enter(action.asEffectiveStatement());
            LyvNodeData lyvNodeData = new LyvNodeData(modelContext, action.asEffectiveStatement(), stack);
            HtmlLine htmlLine = new HtmlLine(new ArrayList<>(connections), lyvNodeData, RpcInputOutput.OTHER,
                    namespacePrefix);
            lines.add(htmlLine);
            final boolean inputExists = !action.getInput().getChildNodes().isEmpty();
            final boolean outputExists = !action.getOutput().getChildNodes().isEmpty();
            if (inputExists) {
                connections.add(1);
                stack.enter(action.getInput().asEffectiveStatement());
                lyvNodeData = new LyvNodeData(modelContext, action.getInput().asEffectiveStatement(), stack);
                htmlLine = new HtmlLine(new ArrayList<>(connections), lyvNodeData, RpcInputOutput.INPUT,
                        namespacePrefix);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(connections), action.getInput().asEffectiveStatement(),
                        RpcInputOutput.INPUT, Collections.emptyList(), stack, true);
                connections.remove(connections.size() - 1);
                stack.exit();
            }
            if (outputExists) {
                connections.add(1);
                stack.enter(action.getOutput().asEffectiveStatement());
                lyvNodeData = new LyvNodeData(modelContext, action.getOutput().asEffectiveStatement(), stack);
                htmlLine = new HtmlLine(new ArrayList<>(connections), lyvNodeData, RpcInputOutput.OUTPUT,
                        namespacePrefix);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(connections), action.getOutput().asEffectiveStatement(),
                        RpcInputOutput.OUTPUT, Collections.emptyList(), stack, true);
                connections.remove(connections.size() - 1);
                stack.exit();
            }
            connections.remove(connections.size() - 1);
            stack.exit();
        }
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

    private static List<QName> keyDefinition(final EffectiveStatement<?, ?> statement) {
        if (statement instanceof ListEffectiveStatement listStatement) {
            return listStatement.findKeyStatement().map(key -> key.argument().asList()).orElse(List.of());
        }
        return List.of();
    }

    private void resolveChoiceSchemaNode(final Iterator<SchemaTreeEffectiveStatement<?>> iterator,
            final List<Line> lines, final List<Integer> connections, final RpcInputOutput inputOutput,
            final LyvStack stack, final boolean ambientConfig) {
        int id = 1;
        while (iterator.hasNext()) {
            final SchemaTreeEffectiveStatement<?> child = iterator.next();
            stack.enter(child);
            connections.set(connections.size() - 1, id++);
            final boolean childConfig = resolveEffectiveConfig(stack).orElse(ambientConfig);
            final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, child, stack, null, childConfig);
            final HtmlLine htmlLine = new HtmlLine(new ArrayList<>(connections), lyvNodeData, inputOutput,
                    namespacePrefix);
            lines.add(htmlLine);
            resolveChildNodes(lines, new ArrayList<>(connections), child, inputOutput, Collections.emptyList(), stack,
                    childConfig);
            stack.exit();
        }
        // remove last
        connections.remove(connections.size() - 1);
    }

    private void resolveDataNodeContainer(final Iterator<SchemaTreeEffectiveStatement<?>> childNodes,
            final List<Line> lines, final List<Integer> connections, final RpcInputOutput inputOutput,
            final List<QName> keys, final LyvStack stack, final boolean ambientConfig) {
        int id = 1;
        connections.add(0);
        while (childNodes.hasNext()) {
            final SchemaTreeEffectiveStatement<?> child = childNodes.next();
            stack.enter(child);
            connections.set(connections.size() - 1, id++);
            final boolean childConfig = resolveEffectiveConfig(stack).orElse(ambientConfig);
            final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, child, stack, keys, childConfig);
            final HtmlLine htmlLine = new HtmlLine(new ArrayList<>(connections), lyvNodeData, inputOutput,
                    namespacePrefix);
            lines.add(htmlLine);
            resolveChildNodes(lines, new ArrayList<>(connections), child, inputOutput, keyDefinition(child), stack,
                    childConfig);
            stack.exit();
        }
        // remove last only if the conatiner is not root container
        if (connections.size() > 1) {
            connections.remove(connections.size() - 1);
        }
    }


    @Override
    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
            justification = "Valid output from LYV is dependent on Logback output")
    void init(final EffectiveModelContext context, final SchemaTree tree, final Configuration config) {
        super.init(context, tree, config);
        LOG.info("{}", prepareHeader());
    }

    @Override
    public Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        return Optional.empty();
    }

    @Override
    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
            justification = "Valid output from LYV is dependent on Logback output")
    public void close(final Collection<Module> modules) {
        LOG.info("{}", loadJS(modules));
        LOG.info("</body>");
        LOG.info("</html>");
    }

    private static class SingletonListInitializer {

        private int id;

        SingletonListInitializer(final int initialValue) {
            id = initialValue;
        }

        List<Integer> getSingletonListWithIncreasedValue() {
            return new ArrayList<>(Collections.singletonList(id++));
        }
    }
}
