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
import io.lighty.yang.validator.exceptions.NotFoundException;
import io.lighty.yang.validator.formats.utility.LyvNodeData;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.util.ArrayList;
import java.util.Collection;
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
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
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

    private Map<XMLNamespace, String> namespacePrefix = new HashMap<>();
    private Module usedModule = null;
    private int treeDepth;
    private int lineLength;

    @Override
    void init(final EffectiveModelContext context, final List<RevisionSourceIdentifier> testFilesSchemaSources,
            final SchemaTree schemaTree, final Configuration config) {
        super.init(context, testFilesSchemaSources, schemaTree, config);
        namespacePrefix = new HashMap<>();
        usedModule = null;
        treeDepth = configuration.getTreeConfiguration().getTreeDepth();
        final int len = configuration.getTreeConfiguration().getLineLength();
        lineLength = len == 0 ? 10000 : len;
    }

    @Override
    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    public void emitFormat() {
        if (configuration.getTreeConfiguration().isHelp()) {
            printHelp();
        }
        for (final RevisionSourceIdentifier source : this.sources) {
            usedModule = schemaContext.findModule(source.getName(), source.getRevision())
                    .orElseThrow(() -> new NotFoundException("Module", source.getName()));
            final String firstLine = MODULE + usedModule.getName();
            LOG.info("{}", firstLine.substring(0, min(firstLine.length(), lineLength)));

            putSchemaContextModuleMatchedWithUsedModuleToNamespacePrefix();

            final AtomicInteger rootNodes = new AtomicInteger(0);
            for (final SchemaTree st : schemaTree.getChildren()) {
                if (st.getQname().getModule().equals(usedModule.getQNameModule()) && !st.isAugmenting()) {
                    rootNodes.incrementAndGet();
                }
            }

            // Nodes
            printLines(getSchemaNodeLines(rootNodes));

            // Augmentations
            final Map<List<QName>, Set<SchemaTree>> augments = getAugmentationMap();
            for (final Map.Entry<List<QName>, Set<SchemaTree>> st : augments.entrySet()) {
                printLines(getAugmentedLines(st));
            }

            // Rpcs
            final Iterator<? extends RpcDefinition> rpcs = usedModule.getRpcs().iterator();
            if (rpcs.hasNext()) {
                LOG.info("{}", RPCS.substring(0, min(RPCS.length(), lineLength)));
            }
            printLines(getRpcsLines(rpcs));

            // Notifications
            final Iterator<? extends NotificationDefinition> notifications = usedModule.getNotifications().iterator();
            if (notifications.hasNext()) {
                LOG.info("{}", NOTIFICATION.substring(0, min(NOTIFICATION.length(), lineLength)));
            }
            printLines(getNotificationLines(notifications));
        }
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    private List<Line> getAugmentedLines(final Entry<List<QName>, Set<SchemaTree>> st) {
        final List<Line> lines = new ArrayList<>();
        final StringBuilder pathBuilder = new StringBuilder();
        for (final QName qname : st.getKey()) {
            pathBuilder.append(SLASH);
            if (configuration.getTreeConfiguration().isPrefixMainModule()
                    || namespacePrefix.containsKey(qname.getNamespace())) {
                pathBuilder.append(namespacePrefix.get(qname.getNamespace()))
                        .append(COLON);
            }
            pathBuilder.append(qname.getLocalName());
        }
        final String augmentText = AUGMENT + pathBuilder.append(COLON);
        LOG.info("{}", augmentText.substring(0, min(augmentText.length(), lineLength)));
        int augmentationNodes = st.getValue().size();
        for (final SchemaTree value : st.getValue()) {
            final DataSchemaNode node = value.getSchemaNode();
            final LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, node, Collections.emptyList(),
                    value.getAbsolutePath());
            final ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), lyvNodeData, RpcInputOutput.OTHER,
                    namespacePrefix);
            lines.add(consoleLine);
            resolveChildNodes(lines, new ArrayList<>(), value, --augmentationNodes > 0,
                    RpcInputOutput.OTHER, Collections.emptyList());
            treeDepth++;
        }
        return lines;
    }

    private List<Line> getSchemaNodeLines(final AtomicInteger rootNodes) {
        final List<Line> lines = new ArrayList<>();
        for (final SchemaTree st : schemaTree.getChildren()) {
            if (st.getQname().getModule().equals(usedModule.getQNameModule()) && !st.isAugmenting()) {
                final DataSchemaNode node = st.getSchemaNode();
                final LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, node, Collections.emptyList(),
                        st.getAbsolutePath());
                final ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), lyvNodeData,
                        RpcInputOutput.OTHER, namespacePrefix);
                lines.add(consoleLine);
                List<QName> keyDefinitions = Collections.emptyList();
                if (node instanceof ListSchemaNode) {
                    keyDefinitions = ((ListSchemaNode) node).getKeyDefinition();
                }
                resolveChildNodes(lines, new ArrayList<>(), st, rootNodes.decrementAndGet() > 0,
                        RpcInputOutput.OTHER, keyDefinitions);
                treeDepth++;
            }
        }
        return lines;
    }

    private void putSchemaContextModuleMatchedWithUsedModuleToNamespacePrefix() {
        for (final Module m : schemaContext.getModules()) {
            if (!m.getPrefix().equals(usedModule.getPrefix())
                    || configuration.getTreeConfiguration().isPrefixMainModule()) {
                if (configuration.getTreeConfiguration().isModulePrefix()) {
                    namespacePrefix.put(m.getNamespace(), m.getName());
                } else {
                    namespacePrefix.put(m.getNamespace(), m.getPrefix());
                }
            }
        }
    }

    private List<Line> getNotificationLines(final Iterator<? extends NotificationDefinition> notifications) {
        final List<Line> lines = new ArrayList<>();
        final SchemaInferenceStack schemaIS = SchemaInferenceStack.of(schemaContext);
        while (notifications.hasNext()) {
            final NotificationDefinition node = notifications.next();
            schemaIS.enterSchemaTree(node.getQName());
            final LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, node, Collections.emptyList(),
                    schemaIS.toSchemaNodeIdentifier());
            final ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), lyvNodeData, RpcInputOutput.OTHER,
                    namespacePrefix);
            lines.add(consoleLine);
            resolveChildNodes(lines, new ArrayList<>(), node, false, RpcInputOutput.OTHER,
                    Collections.emptyList(), schemaIS);
            treeDepth++;
            schemaIS.exit();
        }
        return lines;
    }

    private List<Line> getRpcsLines(final Iterator<? extends RpcDefinition> rpcs) {
        final List<Line> lines = new ArrayList<>();
        final SchemaInferenceStack schemaIS = SchemaInferenceStack.of(schemaContext);
        while (rpcs.hasNext()) {
            final RpcDefinition node = rpcs.next();
            schemaIS.enterSchemaTree(node.getQName());
            LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, node, Collections.emptyList(),
                    schemaIS.toSchemaNodeIdentifier());
            ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), lyvNodeData, RpcInputOutput.OTHER,
                    namespacePrefix);
            lines.add(consoleLine);
            final boolean inputExists = !node.getInput().getChildNodes().isEmpty();
            final boolean outputExists = !node.getOutput().getChildNodes().isEmpty();
            if (inputExists) {
                schemaIS.enterSchemaTree(node.getInput().getQName());
                lyvNodeData = new LyvNodeData(schemaContext, node.getInput(), Collections.emptyList(),
                        schemaIS.toSchemaNodeIdentifier());
                consoleLine = new ConsoleLine(Collections.singletonList(rpcs.hasNext()), lyvNodeData,
                        RpcInputOutput.INPUT, namespacePrefix);
                lines.add(consoleLine);
                final List<Boolean> isNextRpc = new ArrayList<>(Collections.singleton(rpcs.hasNext()));
                resolveChildNodes(lines, isNextRpc, node.getInput(), outputExists, RpcInputOutput.INPUT,
                        Collections.emptyList(), schemaIS);
                schemaIS.exit();
                treeDepth++;
            }
            if (outputExists) {
                schemaIS.enterSchemaTree(node.getOutput().getQName());
                lyvNodeData = new LyvNodeData(schemaContext, node.getOutput(), Collections.emptyList(),
                        schemaIS.toSchemaNodeIdentifier());
                consoleLine = new ConsoleLine(Collections.singletonList(rpcs.hasNext()), lyvNodeData,
                        RpcInputOutput.OUTPUT, namespacePrefix);
                lines.add(consoleLine);
                final List<Boolean> isNextRpc = new ArrayList<>(Collections.singleton(rpcs.hasNext()));
                resolveChildNodes(lines, isNextRpc, node.getOutput(), false, RpcInputOutput.OUTPUT,
                        Collections.emptyList(), schemaIS);
                schemaIS.exit();
                treeDepth++;
            }
            schemaIS.exit();
        }
        return lines;
    }

    private Map<List<QName>, Set<SchemaTree>> getAugmentationMap() {
        final Map<List<QName>, Set<SchemaTree>> augments = new LinkedHashMap<>();
        for (final SchemaTree st : schemaTree.getChildren()) {
            if (st.getQname().getModule().equals(usedModule.getQNameModule()) && st.isAugmenting()) {
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
            final boolean hasNext, final RpcInputOutput inputOutput, final List<QName> keys) {
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
            resolveDataNodeContainer(lines, isConnected, st, inputOutput, keys, actionExists);
            isConnected.remove(isConnected.size() - 1);
        } else if (node instanceof ChoiceSchemaNode) {
            isConnected.add(hasNext);
            resolveChoiceSchemaNode(lines, isConnected, st, inputOutput, actionExists);
            isConnected.remove(isConnected.size() - 1);
        }
        // If action is in container or list
        if (!st.getActionDefinitionChildren().isEmpty()) {
            isConnected.add(hasNext);
            final Iterator<SchemaTree> actions = st.getActionDefinitionChildren().iterator();
            while (actions.hasNext()) {
                resolveActions(lines, isConnected, hasNext, actions);
                isConnected.remove(isConnected.size() - 1);
            }
        }
    }

    private void resolveChildNodes(final List<Line> lines, final List<Boolean> isConnected, final SchemaNode node,
            final boolean hasNext, final RpcInputOutput inputOutput, final List<QName> keys,
            final SchemaInferenceStack schemaIS) {
        if (--treeDepth == 0) {
            return;
        }
        boolean actionExists = false;
        if (node instanceof ActionNodeContainer) {
            actionExists = !((ActionNodeContainer) node).getActions().isEmpty();
        }
        if (node instanceof DataNodeContainer) {
            isConnected.add(hasNext);
            resolveDataNodeContainer(lines, isConnected, node, inputOutput, keys, actionExists, schemaIS);
            // remove last
            isConnected.remove(isConnected.size() - 1);
        } else if (node instanceof ChoiceSchemaNode) {
            isConnected.add(hasNext);
            resolveChoiceSchemaNode(lines, isConnected, node, inputOutput, actionExists, schemaIS);
            // remove last
            isConnected.remove(isConnected.size() - 1);
        }
        // If action is in container or list
        if (node instanceof ActionNodeContainer) {
            final Iterator<? extends ActionDefinition> actions = ((ActionNodeContainer) node).getActions().iterator();
            while (actions.hasNext()) {
                final ActionDefinition action = actions.next();
                isConnected.add(actions.hasNext());
                schemaIS.enterSchemaTree(action.getQName());
                LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, action, Collections.emptyList(),
                        schemaIS.toSchemaNodeIdentifier());
                ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData,
                        RpcInputOutput.OTHER, namespacePrefix);
                lines.add(consoleLine);
                final boolean inputExists = !action.getInput().getChildNodes().isEmpty();
                final boolean outputExists = !action.getOutput().getChildNodes().isEmpty();
                if (inputExists) {
                    isConnected.add(outputExists);
                    schemaIS.enterSchemaTree(action.getInput().getQName());
                    lyvNodeData = new LyvNodeData(schemaContext, action.getInput(), Collections.emptyList(),
                            schemaIS.toSchemaNodeIdentifier());
                    consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, RpcInputOutput.INPUT,
                            namespacePrefix);
                    lines.add(consoleLine);
                    resolveChildNodes(lines, isConnected, action.getInput(), outputExists, RpcInputOutput.INPUT,
                            Collections.emptyList(), schemaIS);
                    treeDepth++;
                    isConnected.remove(isConnected.size() - 1);
                    schemaIS.exit();
                }
                if (outputExists) {
                    isConnected.add(false);
                    schemaIS.enterSchemaTree(action.getOutput().getQName());
                    lyvNodeData = new LyvNodeData(schemaContext, action.getOutput(), Collections.emptyList(),
                            schemaIS.toSchemaNodeIdentifier());
                    consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData,
                            RpcInputOutput.OUTPUT, namespacePrefix);
                    lines.add(consoleLine);
                    resolveChildNodes(lines, isConnected, action.getOutput(), false, RpcInputOutput.OUTPUT,
                            Collections.emptyList(), schemaIS);
                    treeDepth++;
                    isConnected.remove(isConnected.size() - 1);
                    schemaIS.exit();
                }
                isConnected.remove(isConnected.size() - 1);
            }
        }
    }

    private void resolveActions(final List<Line> lines, final List<Boolean> isConnected, final boolean hasNext,
            final Iterator<SchemaTree> actions) {
        final SchemaTree nextST = actions.next();
        if (nextST.getQname().getModule().equals(usedModule.getQNameModule())) {
            resolveActions(lines, isConnected, hasNext, actions, nextST);
        }
    }

    private void resolveActions(final List<Line> lines, final List<Boolean> isConnected, final boolean hasNext,
            final Iterator<SchemaTree> actions, final SchemaTree actionSchemaTree) {
        final ActionDefinition action = actionSchemaTree.getActionNode();
        LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, action, Collections.emptyList(),
                actionSchemaTree.getAbsolutePath());
        ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, RpcInputOutput.OTHER,
                namespacePrefix);
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
            lyvNodeData = new LyvNodeData(schemaContext, action.getInput(), Collections.emptyList(),
                    inValue.getAbsolutePath());
            consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, RpcInputOutput.INPUT,
                    namespacePrefix);
            lines.add(consoleLine);
            resolveChildNodes(lines, isConnected, inValue, outputExists, RpcInputOutput.INPUT,
                    Collections.emptyList());
            treeDepth++;
            isConnected.remove(isConnected.size() - 1);
        }
        if (outputExists) {
            isConnected.add(actions.hasNext() || hasNext);
            lyvNodeData = new LyvNodeData(schemaContext, action.getOutput(), Collections.emptyList(),
                    outValue.getAbsolutePath());
            consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, RpcInputOutput.OUTPUT,
                    namespacePrefix);
            lines.add(consoleLine);
            resolveChildNodes(lines, isConnected, outValue, false, RpcInputOutput.OUTPUT,
                    Collections.emptyList());
            treeDepth++;
            isConnected.remove(isConnected.size() - 1);
        }
    }

    private void resolveChoiceSchemaNode(final List<Line> lines, final List<Boolean> isConnected, final SchemaTree st,
            final RpcInputOutput inputOutput, final boolean actionExists) {
        final Iterator<SchemaTree> caseNodes = st.getDataSchemaNodeChildren().iterator();
        while (caseNodes.hasNext()) {
            final SchemaTree nextST = caseNodes.next();
            if (nextST.getQname().getModule().equals(usedModule.getQNameModule())) {
                final DataSchemaNode child = nextST.getSchemaNode();
                final LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, child, Collections.emptyList(),
                        nextST.getAbsolutePath());
                final ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, inputOutput,
                        namespacePrefix);
                lines.add(consoleLine);
                resolveChildNodes(lines, isConnected, nextST, caseNodes.hasNext()
                        || actionExists, inputOutput, Collections.emptyList());
                treeDepth++;
            }
        }
    }

    private void resolveChoiceSchemaNode(final List<Line> lines, final List<Boolean> isConnected, final SchemaNode node,
            final RpcInputOutput inputOutput, final boolean actionExists, final SchemaInferenceStack schemaIS) {
        final Collection<? extends CaseSchemaNode> cases = ((ChoiceSchemaNode) node).getCases();
        final Iterator<? extends CaseSchemaNode> iterator = cases.iterator();
        while (iterator.hasNext()) {
            final DataSchemaNode child = iterator.next();
            schemaIS.enterSchemaTree(child.getQName());
            final LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, child, Collections.emptyList(),
                    schemaIS.toSchemaNodeIdentifier());
            final ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, inputOutput,
                    namespacePrefix);
            lines.add(consoleLine);
            resolveChildNodes(lines, isConnected, child, iterator.hasNext() || actionExists, inputOutput,
                    Collections.emptyList(), schemaIS);
            schemaIS.exit();
            treeDepth++;
        }
    }


    private void resolveDataNodeContainer(final List<Line> lines, final List<Boolean> isConnected, final SchemaTree st,
            final RpcInputOutput inputOutput, final List<QName> keys,
            final boolean actionExists) {
        final Iterator<SchemaTree> childNodes = st.getDataSchemaNodeChildren().iterator();
        while (childNodes.hasNext()) {
            final SchemaTree nextST = childNodes.next();
            if (nextST.getQname().getModule().equals(usedModule.getQNameModule())) {
                final DataSchemaNode child = nextST.getSchemaNode();
                final LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, child, keys,
                        nextST.getAbsolutePath());
                final ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, inputOutput,
                        namespacePrefix);
                lines.add(consoleLine);
                List<QName> keyDefinitions = Collections.emptyList();
                if (child instanceof ListSchemaNode) {
                    keyDefinitions = ((ListSchemaNode) child).getKeyDefinition();
                }
                resolveChildNodes(lines, isConnected, nextST, childNodes.hasNext()
                        || actionExists, inputOutput, keyDefinitions);
                treeDepth++;
            }
        }
    }

    private void resolveDataNodeContainer(final List<Line> lines, final List<Boolean> isConnected,
            final SchemaNode node, final RpcInputOutput inputOutput, final List<QName> keys,
            final boolean actionExists, final SchemaInferenceStack schemaIS) {
        final Iterator<? extends DataSchemaNode> childNodes = ((DataNodeContainer) node).getChildNodes().iterator();
        while (childNodes.hasNext()) {
            final DataSchemaNode child = childNodes.next();
            schemaIS.enterSchemaTree(child.getQName());
            final LyvNodeData lyvNodeData = new LyvNodeData(schemaContext, child, keys,
                    schemaIS.toSchemaNodeIdentifier());
            final ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), lyvNodeData, inputOutput,
                    namespacePrefix);
            lines.add(consoleLine);
            List<QName> keyDefinitions = Collections.emptyList();
            if (child instanceof ListSchemaNode) {
                keyDefinitions = ((ListSchemaNode) child).getKeyDefinition();
            }
            resolveChildNodes(lines, isConnected, child, childNodes.hasNext() || actionExists, inputOutput,
                    keyDefinitions, schemaIS);
            schemaIS.exit();
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
