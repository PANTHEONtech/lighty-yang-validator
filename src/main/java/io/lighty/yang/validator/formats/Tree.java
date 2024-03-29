/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import static java.lang.Math.min;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.yang.validator.GroupArguments;
import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.formats.utility.LyvNodeData;
import io.lighty.yang.validator.formats.utility.LyvStack;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.sourceforge.argparse4j.impl.choice.CollectionArgumentChoice;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.ActionNodeContainer;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tree extends FormatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(Tree.class);
    private static final String HELP_NAME = "tree";
    private static final String HELP_DESCRIPTION = "Prints out tree of the modules";
    private static final String MODULE = "module: ";
    private static final String AUGMENT = "augment ";
    private static final String SLASH = "/";
    private static final String COLON = ":";
    private static final String RPCS = "RPCs:";
    private static final String NOTIFICATION = "notifications:";
    private static final Map<XMLNamespace, String> NAMESPACE_PREFIX = new HashMap<>();

    private int treeDepth;
    private int lineLength;

    @Override
    void init(final EffectiveModelContext context, final SchemaTree schemaTree, final Configuration config) {
        super.init(context, schemaTree, config);
        treeDepth = configuration.getTreeConfiguration().getTreeDepth();
        final int len = configuration.getTreeConfiguration().getLineLength();
        lineLength = len == 0 ? 10000 : len;
    }

    @Override
    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    public void emitFormat(final Module module) {
        NAMESPACE_PREFIX.clear();
        if (configuration.getTreeConfiguration().isHelp()) {
            printHelp();
        } else if (module != null) {
            final String firstLine = MODULE + module.getName();
            LOG.info("{}", firstLine.substring(0, min(firstLine.length(), lineLength)));

            putContextModuleMatchedWithUsedModuleToNamespacePrefix(module);

            final AtomicInteger rootNodes = new AtomicInteger(0);
            for (final SchemaTree st : schemaTree.getChildren()) {
                if (st.getQname().getModule().equals(module.getQNameModule()) && !st.isAugmenting()) {
                    rootNodes.incrementAndGet();
                }
            }

            // Nodes
            printLines(getSchemaNodeLines(rootNodes, module));

            // Augmentations
            final Map<List<QName>, Set<SchemaTree>> augments = getAugmentationMap(module);
            for (final Map.Entry<List<QName>, Set<SchemaTree>> st : augments.entrySet()) {
                printLines(getAugmentedLines(st, module));
            }

            // Rpcs
            final Iterator<? extends RpcDefinition> rpcs = module.getRpcs().iterator();
            if (rpcs.hasNext()) {
                LOG.info("{}", RPCS.substring(0, min(RPCS.length(), lineLength)));
            }
            printLines(getRpcsLines(rpcs));

            // Notifications
            final Iterator<? extends NotificationDefinition> notifications = module.getNotifications().iterator();
            if (notifications.hasNext()) {
                LOG.info("{}", NOTIFICATION.substring(0, min(NOTIFICATION.length(), lineLength)));
            }
            printLines(getNotificationLines(notifications));
        } else {
            LOG.error("{}", EMPTY_MODULE_EXCEPTION);
        }
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    private List<Line> getAugmentedLines(final Entry<List<QName>, Set<SchemaTree>> st, final Module module) {
        final List<Line> lines = new ArrayList<>();
        final StringBuilder pathBuilder = new StringBuilder();
        for (final QName qname : st.getKey()) {
            pathBuilder.append(SLASH);
            if (configuration.getTreeConfiguration().isPrefixMainModule()
                    || NAMESPACE_PREFIX.containsKey(qname.getNamespace())) {
                pathBuilder.append(NAMESPACE_PREFIX.get(qname.getNamespace()))
                        .append(COLON);
            }
            pathBuilder.append(qname.getLocalName());
        }
        final String augmentText = AUGMENT + pathBuilder.append(COLON);
        LOG.info("{}", augmentText.substring(0, min(augmentText.length(), lineLength)));
        int augmentationNodes = st.getValue().size();
        for (final SchemaTree value : st.getValue()) {
            final DataSchemaNode node = value.getSchemaNode();
            final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, node, value.getAbsolutePath());
            final ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), lyvNodeData, RpcInputOutput.OTHER,
                NAMESPACE_PREFIX);
            lines.add(consoleLine);
            resolveChildNodes(lines, new ArrayList<>(), value, --augmentationNodes > 0,
                    RpcInputOutput.OTHER, Collections.emptyList(), module);
            treeDepth++;
        }
        return lines;
    }

    private List<Line> getSchemaNodeLines(final AtomicInteger rootNodes, final Module module) {
        final List<Line> lines = new ArrayList<>();
        for (final SchemaTree st : schemaTree.getChildren()) {
            if (st.getQname().getModule().equals(module.getQNameModule()) && !st.isAugmenting()) {
                final DataSchemaNode node = st.getSchemaNode();
                final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, node, st.getAbsolutePath());
                final ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), lyvNodeData,
                        RpcInputOutput.OTHER, NAMESPACE_PREFIX);
                lines.add(consoleLine);
                List<QName> keyDefinitions = Collections.emptyList();
                if (node instanceof ListSchemaNode) {
                    keyDefinitions = ((ListSchemaNode) node).getKeyDefinition();
                }
                resolveChildNodes(lines, new ArrayList<>(), st, rootNodes.decrementAndGet() > 0,
                        RpcInputOutput.OTHER, keyDefinitions, module);
                treeDepth++;
            }
        }
        return lines;
    }

    private void putContextModuleMatchedWithUsedModuleToNamespacePrefix(final Module module) {
        for (final Module m : modelContext.getModules()) {
            if (!m.getPrefix().equals(module.getPrefix())
                    || configuration.getTreeConfiguration().isPrefixMainModule()) {
                if (configuration.getTreeConfiguration().isModulePrefix()) {
                    NAMESPACE_PREFIX.put(m.getNamespace(), m.getName());
                } else {
                    NAMESPACE_PREFIX.put(m.getNamespace(), m.getPrefix());
                }
            }
        }
    }

    private List<Line> getNotificationLines(final Iterator<? extends NotificationDefinition> notifications) {
        final List<Line> lines = new ArrayList<>();
        final LyvStack stack = new LyvStack();
        while (notifications.hasNext()) {
            final NotificationDefinition node = notifications.next();
            stack.enter(node);
            final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, node, stack);
            final ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), lyvNodeData, RpcInputOutput.OTHER,
                NAMESPACE_PREFIX);
            lines.add(consoleLine);
            resolveChildNodes(lines, new ArrayList<>(), node, false, RpcInputOutput.OTHER, Collections.emptyList(),
                stack);
            treeDepth++;
            stack.exit();
        }
        return lines;
    }

    private List<Line> getRpcsLines(final Iterator<? extends RpcDefinition> rpcs) {
        final List<Line> lines = new ArrayList<>();
        final LyvStack stack = new LyvStack();
        while (rpcs.hasNext()) {
            final RpcDefinition node = rpcs.next();
            stack.enter(node);
            LyvNodeData lyvNodeData = new LyvNodeData(modelContext, node, stack);
            ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), lyvNodeData, RpcInputOutput.OTHER,
                NAMESPACE_PREFIX);
            lines.add(consoleLine);
            final boolean inputExists = !node.getInput().getChildNodes().isEmpty();
            final boolean outputExists = !node.getOutput().getChildNodes().isEmpty();
            if (inputExists) {
                stack.enter(node.getInput());
                lyvNodeData = new LyvNodeData(modelContext, node.getInput(), stack);
                consoleLine = new ConsoleLine(Collections.singletonList(rpcs.hasNext()), lyvNodeData,
                    RpcInputOutput.INPUT, NAMESPACE_PREFIX);
                lines.add(consoleLine);
                final List<Boolean> isNextRpc = new ArrayList<>(Collections.singleton(rpcs.hasNext()));
                resolveChildNodes(lines, isNextRpc, node.getInput(), outputExists, RpcInputOutput.INPUT,
                    Collections.emptyList(), stack);
                stack.exit();
                treeDepth++;
            }
            if (outputExists) {
                stack.enter(node.getOutput());
                lyvNodeData = new LyvNodeData(modelContext, node.getOutput(), stack);
                consoleLine = new ConsoleLine(Collections.singletonList(rpcs.hasNext()), lyvNodeData,
                    RpcInputOutput.OUTPUT, NAMESPACE_PREFIX);
                lines.add(consoleLine);
                final List<Boolean> isNextRpc = new ArrayList<>(Collections.singleton(rpcs.hasNext()));
                resolveChildNodes(lines, isNextRpc, node.getOutput(), false, RpcInputOutput.OUTPUT,
                    Collections.emptyList(), stack);
                stack.exit();
                treeDepth++;
            }
            stack.exit();
        }
        return lines;
    }

    private Map<List<QName>, Set<SchemaTree>> getAugmentationMap(final Module module) {
        final Map<List<QName>, Set<SchemaTree>> augments = new LinkedHashMap<>();
        for (final SchemaTree st : schemaTree.getChildren()) {
            if (st.getQname().getModule().equals(module.getQNameModule()) && st.isAugmenting()) {
                final Iterator<QName> iterator = st.getAbsolutePath().getNodeIdentifiers().iterator();
                final List<QName> qnames = new ArrayList<>();
                while (iterator.hasNext()) {
                    final QName next = iterator.next();
                    if (iterator.hasNext()) {
                        qnames.add(next);
                    }
                }
                if (augments.get(qnames) == null || augments.get(qnames).isEmpty()) {
                    augments.put(qnames, new LinkedHashSet<>());
                }
                augments.get(qnames).add(st);
            }
        }
        return augments;
    }

    private void resolveChildNodes(final List<Line> lines, final List<Boolean> isConnected, final SchemaTree st,
            final boolean hasNext, final RpcInputOutput inputOutput, final List<QName> keys, final Module module) {
        if (--treeDepth == 0) {
            return;
        }
        boolean actionExists = false;
        final DataSchemaNode node = st.getSchemaNode();
        if (node instanceof ActionNodeContainer) {
            actionExists = !((ActionNodeContainer) node).getActions().isEmpty();
        }
        if (node instanceof DataNodeContainer) {
            isConnected.add(hasNext);
            resolveDataNodeContainer(lines, isConnected, st, inputOutput, keys, actionExists, module);
            isConnected.remove(isConnected.size() - 1);
        } else if (node instanceof ChoiceSchemaNode) {
            isConnected.add(hasNext);
            resolveChoiceSchemaNode(lines, isConnected, st, inputOutput, actionExists, module);
            isConnected.remove(isConnected.size() - 1);
        }
        // If action is in container or list
        if (!st.getActionDefinitionChildren().isEmpty()) {
            isConnected.add(hasNext);
            final Iterator<SchemaTree> actions = st.getActionDefinitionChildren().iterator();
            while (actions.hasNext()) {
                resolveActions(lines, isConnected, hasNext, actions, module);
                isConnected.remove(isConnected.size() - 1);
            }
        }
    }

    private void resolveChildNodes(final List<Line> lines, final List<Boolean> isConnected, final SchemaNode node,
            final boolean hasNext, final RpcInputOutput inputOutput, final List<QName> keys, final LyvStack stack) {
        if (--treeDepth == 0) {
            return;
        }
        boolean actionExists = false;
        if (node instanceof ActionNodeContainer) {
            actionExists = !((ActionNodeContainer) node).getActions().isEmpty();
        }
        if (node instanceof DataNodeContainer) {
            isConnected.add(hasNext);
            resolveDataNodeContainer(lines, isConnected, node, inputOutput, keys, actionExists, stack);
            // remove last
            isConnected.remove(isConnected.size() - 1);
        } else if (node instanceof ChoiceSchemaNode) {
            isConnected.add(hasNext);
            resolveChoiceSchemaNode(lines, isConnected, node, inputOutput, actionExists, stack);
            // remove last
            isConnected.remove(isConnected.size() - 1);
        }
        // If action is in container or list
        if (node instanceof ActionNodeContainer) {
            final Iterator<? extends ActionDefinition> actions = ((ActionNodeContainer) node).getActions().iterator();
            while (actions.hasNext()) {
                final ActionDefinition action = actions.next();
                isConnected.add(actions.hasNext());
                stack.enter(action);
                LyvNodeData lyvNodeData = new LyvNodeData(modelContext, action, stack);
                ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData,
                        RpcInputOutput.OTHER, NAMESPACE_PREFIX);
                lines.add(consoleLine);
                final boolean inputExists = !action.getInput().getChildNodes().isEmpty();
                final boolean outputExists = !action.getOutput().getChildNodes().isEmpty();
                if (inputExists) {
                    isConnected.add(outputExists);
                    stack.enter(action.getInput());
                    lyvNodeData = new LyvNodeData(modelContext, action.getInput(), stack);
                    consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, RpcInputOutput.INPUT,
                        NAMESPACE_PREFIX);
                    lines.add(consoleLine);
                    resolveChildNodes(lines, isConnected, action.getInput(), outputExists, RpcInputOutput.INPUT,
                            Collections.emptyList(), stack);
                    treeDepth++;
                    isConnected.remove(isConnected.size() - 1);
                    stack.exit();
                }
                if (outputExists) {
                    isConnected.add(false);
                    stack.enter(action.getOutput());
                    lyvNodeData = new LyvNodeData(modelContext, action.getOutput(), stack);
                    consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, RpcInputOutput.OUTPUT,
                        NAMESPACE_PREFIX);
                    lines.add(consoleLine);
                    resolveChildNodes(lines, isConnected, action.getOutput(), false, RpcInputOutput.OUTPUT,
                        Collections.emptyList(), stack);
                    treeDepth++;
                    isConnected.remove(isConnected.size() - 1);
                    stack.exit();
                }
                isConnected.remove(isConnected.size() - 1);
            }
        }
    }

    private void resolveActions(final List<Line> lines, final List<Boolean> isConnected, final boolean hasNext,
            final Iterator<SchemaTree> actions, final Module module) {
        final SchemaTree nextST = actions.next();
        if (nextST.getQname().getModule().equals(module.getQNameModule())) {
            resolveActions(lines, isConnected, hasNext, actions, nextST, module);
        }
    }

    private void resolveActions(final List<Line> lines, final List<Boolean> isConnected, final boolean hasNext,
            final Iterator<SchemaTree> actions, final SchemaTree actionSchemaTree, final Module module) {
        final ActionDefinition action = actionSchemaTree.getActionNode();
        LyvNodeData lyvNodeData = new LyvNodeData(modelContext, action, actionSchemaTree.getAbsolutePath(), null);
        ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, RpcInputOutput.OTHER,
            NAMESPACE_PREFIX);
        lines.add(consoleLine);
        boolean inputExists = false;
        boolean outputExists = false;
        SchemaTree inValue = null;
        SchemaTree outValue = null;
        for (final SchemaTree inOut : actionSchemaTree.getChildren()) {
            if ("input".equals(inOut.getQname().getLocalName()) && !inOut.getChildren().isEmpty()) {
                inputExists = true;
                inValue = inOut;
            } else if ("output".equals(inOut.getQname().getLocalName()) && !inOut.getChildren().isEmpty()) {
                outputExists = true;
                outValue = inOut;
            }
        }
        if (inputExists) {
            isConnected.add(actions.hasNext() || hasNext);
            lyvNodeData = new LyvNodeData(modelContext, action.getInput(), inValue.getAbsolutePath());
            consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, RpcInputOutput.INPUT,
                NAMESPACE_PREFIX);
            lines.add(consoleLine);
            resolveChildNodes(lines, isConnected, inValue, outputExists, RpcInputOutput.INPUT,
                    Collections.emptyList(), module);
            treeDepth++;
            isConnected.remove(isConnected.size() - 1);
        }
        if (outputExists) {
            isConnected.add(actions.hasNext() || hasNext);
            lyvNodeData = new LyvNodeData(modelContext, action.getOutput(), outValue.getAbsolutePath());
            consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, RpcInputOutput.OUTPUT,
                NAMESPACE_PREFIX);
            lines.add(consoleLine);
            resolveChildNodes(lines, isConnected, outValue, false, RpcInputOutput.OUTPUT,
                    Collections.emptyList(), module);
            treeDepth++;
            isConnected.remove(isConnected.size() - 1);
        }
    }

    private void resolveChoiceSchemaNode(final List<Line> lines, final List<Boolean> isConnected, final SchemaTree st,
            final RpcInputOutput inputOutput, final boolean actionExists, final Module module) {
        final Iterator<SchemaTree> caseNodes = st.getDataSchemaNodeChildren().iterator();
        while (caseNodes.hasNext()) {
            final SchemaTree nextST = caseNodes.next();
            if (nextST.getQname().getModule().equals(module.getQNameModule())) {
                final DataSchemaNode child = nextST.getSchemaNode();
                final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, child, nextST.getAbsolutePath());
                final ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, inputOutput,
                    NAMESPACE_PREFIX);
                lines.add(consoleLine);
                resolveChildNodes(lines, isConnected, nextST, caseNodes.hasNext()
                        || actionExists, inputOutput, Collections.emptyList(), module);
                treeDepth++;
            }
        }
    }

    private void resolveChoiceSchemaNode(final List<Line> lines, final List<Boolean> isConnected, final SchemaNode node,
            final RpcInputOutput inputOutput, final boolean actionExists, final LyvStack stack) {
        final Iterator<? extends CaseSchemaNode> iterator = ((ChoiceSchemaNode) node).getCases().iterator();
        while (iterator.hasNext()) {
            final DataSchemaNode child = iterator.next();
            stack.enter(child);
            final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, child, stack);
            final ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, inputOutput,
                NAMESPACE_PREFIX);
            lines.add(consoleLine);
            resolveChildNodes(lines, isConnected, child, iterator.hasNext() || actionExists, inputOutput,
                    Collections.emptyList(), stack);
            stack.exit();
            treeDepth++;
        }
    }


    private void resolveDataNodeContainer(final List<Line> lines, final List<Boolean> isConnected, final SchemaTree st,
            final RpcInputOutput inputOutput, final List<QName> keys,
            final boolean actionExists, final Module module) {
        final Iterator<SchemaTree> childNodes = st.getDataSchemaNodeChildren().iterator();
        while (childNodes.hasNext()) {
            final SchemaTree nextST = childNodes.next();
            if (nextST.getQname().getModule().equals(module.getQNameModule())) {
                final DataSchemaNode child = nextST.getSchemaNode();
                final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, child, nextST.getAbsolutePath(), keys);
                final ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, inputOutput,
                    NAMESPACE_PREFIX);
                lines.add(consoleLine);
                List<QName> keyDefinitions = Collections.emptyList();
                if (child instanceof ListSchemaNode) {
                    keyDefinitions = ((ListSchemaNode) child).getKeyDefinition();
                }
                resolveChildNodes(lines, isConnected, nextST, childNodes.hasNext() || actionExists, inputOutput,
                    keyDefinitions, module);
                treeDepth++;
            }
        }
    }

    private void resolveDataNodeContainer(final List<Line> lines, final List<Boolean> isConnected,
            final SchemaNode node, final RpcInputOutput inputOutput, final List<QName> keys,
            final boolean actionExists, final LyvStack stack) {
        final Iterator<? extends DataSchemaNode> childNodes = ((DataNodeContainer) node).getChildNodes().iterator();
        while (childNodes.hasNext()) {
            final DataSchemaNode child = childNodes.next();
            stack.enter(child);
            final LyvNodeData lyvNodeData = new LyvNodeData(modelContext, child, stack, keys);
            final ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, inputOutput,
                NAMESPACE_PREFIX);
            lines.add(consoleLine);
            List<QName> keyDefinitions = Collections.emptyList();
            if (child instanceof ListSchemaNode) {
                keyDefinitions = ((ListSchemaNode) child).getKeyDefinition();
            }
            resolveChildNodes(lines, isConnected, child, childNodes.hasNext() || actionExists, inputOutput,
                    keyDefinitions, stack);
            stack.exit();
            treeDepth++;
        }
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    private void printLines(final List<Line> lines) {
        for (final Line l : lines) {
            final String linesText = l.toString();
            LOG.info("{}", linesText.substring(0, min(linesText.length(), lineLength)));
        }
    }

    private static void printHelp() {
        LOG.info(
                "tree - tree is printed in following format <status>--<flags> <name><opts> <type> <if-features>\n"
                        + "\n"
                        + " <status> is one of:\n"
                        + "\n"
                        + "    +  for current\n"
                        + "    x  for deprecated\n"
                        + "    o  for obsolete\n"
                        + "\n"
                        + " <flags> is one of:\n"
                        + "\n"
                        + "    rw  for configuration data\n"
                        + "    ro  for non-configuration data, output parameters to rpcs\n"
                        + "       and actions, and notification parameters\n"
                        + "    -w  for input parameters to rpcs and actions\n"
                        + "    -x  for rpcs and actions\n"
                        + "    -n  for notifications\n"
                        + "\n"
                        + " <name> is the name of the node:\n"
                        + "\n"
                        + "    (<name>) means that the node is a choice node\n"
                        + "    :(<name>) means that the node is a case node\n"
                        + "\n"
                        + " <opts> is one of:\n"
                        + "\n"
                        + "    ?  for an optional leaf, choice\n"
                        + "    *  for a leaf-list or list\n"
                        + "    [<keys>] for a list's keys\n"
                        + "\n"
                        + " <type> is the name of the type for leafs and leaf-lists.\n"
                        + "  If the type is a leafref, the type is printed as \"-> TARGET\",\n"
                        + "  whereTARGET is the leafref path, with prefixes removed if possible.\n"
                        + "\n"
                        + " <if-features> is the list of features this node depends on, printed\n"
                        + "     within curly brackets and a question mark \"{...}?\"\n");
    }

    @Override
    public Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        final GroupArguments groupArguments = new GroupArguments(HELP_NAME,
                "Tree format based arguments: ");
        groupArguments.addOption("Number of children to print (0 = all the child nodes).",
                Collections.singletonList("--tree-depth"), false, "?", 0,
                new CollectionArgumentChoice<>(Collections.emptyList()), Integer.TYPE);
        groupArguments.addOption("Number of characters to print for each line (print the whole line).",
                Collections.singletonList("--tree-line-length"), false, "?", 0,
                new CollectionArgumentChoice<>(Collections.emptyList()), Integer.TYPE);
        groupArguments.addOption("Print help information for symbols used in tree format.",
                Collections.singletonList("--tree-help"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("Use the whole module name instead of prefix.",
                Collections.singletonList("--tree-prefix-module"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("Use prefix with used module.",
                Collections.singletonList("--tree-prefix-main-module"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        return Optional.of(groupArguments);
    }
}
