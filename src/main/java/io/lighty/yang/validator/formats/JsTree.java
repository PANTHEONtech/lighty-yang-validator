/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.yang.validator.GroupArguments;
import io.lighty.yang.validator.exceptions.NotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.ActionNodeContainer;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsTree extends FormatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(JsTree.class);
    private static final String HELP_NAME = "jstree";
    private static final String HELP_DESCRIPTION = "Prints out html, javascript tree of the modules";
    private static final String INPUT = "input";

    private final Map<URI, String> namespacePrefix = new HashMap<>();

    @Override
    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    public void emitFormat() {
        for (final RevisionSourceIdentifier source : this.sources) {
            final Module module = this.schemaContext.findModule(source.getName(), source.getRevision())
                    .orElseThrow(() -> new NotFoundException("Module", source.getName()));
            final List<Integer> removeChoiceQnames = new ArrayList<>();
            SingletonListInitializer singletonListInitializer = new SingletonListInitializer(1);

            // Nodes
            printLines(getChildNodesLines(singletonListInitializer, module, removeChoiceQnames));

            // Augmentations
            for (AugmentationSchemaNode augNode : module.getAugmentations()) {
                printLines(getAugmentationNodesLines(singletonListInitializer.getSingletonListWithIncreasedValue(),
                        augNode, removeChoiceQnames));
            }

            // Rpcs
            printLines(getRpcsLines(singletonListInitializer, module, removeChoiceQnames));

            // Notifications
            printLines(getNotificationsLines(singletonListInitializer, module, removeChoiceQnames));
        }

        LOG.info("</table>");
        LOG.info("</div>");
        LOG.info("{}", loadJS());
        LOG.info("</body>");
        LOG.info("</html>");
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    private void printLines(final List<Line> lines) {
        for (Line line : lines) {
            LOG.info("{}", line.toString());
        }
    }

    private List<Line> getNotificationsLines(final SingletonListInitializer singletonListInitializer,
            final Module module, final List<Integer> removeChoiceQnames) {
        List<Line> lines = new ArrayList<>();
        for (NotificationDefinition node : module.getNotifications()) {
            final ArrayList<Integer> ids = singletonListInitializer.getSingletonListWithIncreasedValue();
            HtmlLine htmlLine = new HtmlLine(new ArrayList<>(ids), node, RpcInputOutput.OTHER, this.schemaContext,
                    removeChoiceQnames, namespacePrefix, Optional.empty(), false);
            lines.add(htmlLine);
            resolveChildNodes(lines, new ArrayList<>(ids), node, RpcInputOutput.OTHER, removeChoiceQnames,
                    Collections.emptyList());
        }
        return lines;
    }

    private List<Line> getRpcsLines(final SingletonListInitializer singletonListInitializer,
            final Module module, final List<Integer> removeChoiceQnames) {
        List<Line> lines = new ArrayList<>();
        for (RpcDefinition node : module.getRpcs()) {
            final ArrayList<Integer> rpcId = singletonListInitializer.getSingletonListWithIncreasedValue();
            HtmlLine htmlLine = new HtmlLine(rpcId, node, RpcInputOutput.OTHER, this.schemaContext,
                    removeChoiceQnames, namespacePrefix, Optional.empty(), false);
            lines.add(htmlLine);
            final boolean inputExists = !node.getInput().getChildNodes().isEmpty();
            final boolean outputExists = !node.getOutput().getChildNodes().isEmpty();
            ArrayList<Integer> ids = new ArrayList<>(rpcId);
            if (inputExists) {
                ids.add(1);
                htmlLine = new HtmlLine(new ArrayList<>(ids), node.getInput(), RpcInputOutput.INPUT,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(ids), node.getInput(), RpcInputOutput.INPUT,
                        removeChoiceQnames, Collections.emptyList());
            }
            ids = new ArrayList<>(rpcId);
            if (outputExists) {
                if (!inputExists) {
                    ids.add(1);
                } else {
                    ids.add(2);
                }
                htmlLine = new HtmlLine(new ArrayList<>(ids), node.getOutput(), RpcInputOutput.OUTPUT,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(ids), node.getOutput(), RpcInputOutput.OUTPUT,
                        removeChoiceQnames, Collections.emptyList());
            }
        }
        return lines;
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    private List<Line> getChildNodesLines(final SingletonListInitializer singletonListInitializer,
            final Module module, final List<Integer> removeChoiceQnames) {
        List<Line> lines = new ArrayList<>();
        final String headerText = prepareHeader(module);
        LOG.info("{}", headerText);
        for (Module m : this.schemaContext.getModules()) {
            if (!m.getPrefix().equals(module.getPrefix())) {
                namespacePrefix.put(m.getNamespace(), m.getPrefix());
            }
        }
        for (DataSchemaNode node : module.getChildNodes()) {
            final ArrayList<Integer> ids = singletonListInitializer.getSingletonListWithIncreasedValue();
            HtmlLine htmlLine = new HtmlLine(ids, node, RpcInputOutput.OTHER, this.schemaContext, removeChoiceQnames,
                    namespacePrefix, Optional.empty(), false);
            lines.add(htmlLine);
            resolveChildNodes(lines, new ArrayList<>(ids), node, RpcInputOutput.OTHER, removeChoiceQnames,
                    Collections.emptyList());
        }
        return lines;
    }

    private List<Line> getAugmentationNodesLines(final ArrayList<Integer> ids, final AugmentationSchemaNode augNode,
            final List<Integer> removeChoiceQnames) {
        List<Line> lines = new ArrayList<>();
        HtmlLine htmlLine = new HtmlLine(new ArrayList<>(ids), augNode.getChildNodes().iterator().next(),
                RpcInputOutput.OTHER, this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.of(augNode),
                false);
        lines.add(htmlLine);
        final Iterator<? extends DataSchemaNode> nodes = augNode.getChildNodes().iterator();
        int modelAugmentationNumber = 1;
        while (nodes.hasNext()) {
            removeChoiceQnames.clear();
            final DataSchemaNode node = nodes.next();
            final ArrayList<QName> qnames = Lists.newArrayList(node.getPath().getPathFromRoot().iterator());
            RpcInputOutput inputOutputOther = getAugmentationRpcInputOutput(removeChoiceQnames, qnames);
            ids.add(modelAugmentationNumber++);
            HtmlLine line = new HtmlLine(new ArrayList<>(ids), node, inputOutputOther, this.schemaContext,
                    removeChoiceQnames, namespacePrefix, Optional.empty(), false);
            lines.add(line);
            resolveChildNodes(lines, new ArrayList<>(ids), node, RpcInputOutput.OTHER, removeChoiceQnames,
                    Collections.emptyList());
            ids.remove(ids.size() - 1);
        }
        return lines;
    }

    private RpcInputOutput getAugmentationRpcInputOutput(final List<Integer> removeChoiceQnames,
            final ArrayList<QName> qnames) {
        Collection<? extends ActionDefinition> actions = new HashSet<>();
        RpcInputOutput inputOutputOther = RpcInputOutput.OTHER;
        for (int i = 1; i <= qnames.size(); i++) {
            List<QName> qnamesCopy = new ArrayList<>(qnames);
            qnamesCopy = qnamesCopy.subList(0, i);
            inputOutputOther = getRpcInputOutput(qnames, actions, inputOutputOther, i, qnamesCopy);

            final ListIterator<Integer> integerListIterator =
                    removeChoiceQnames.listIterator(removeChoiceQnames.size());
            while (integerListIterator.hasPrevious()) {
                qnamesCopy.remove(integerListIterator.previous().intValue());
                i--;
            }
            if (!this.schemaContext.findDataTreeChild(qnamesCopy).isPresent()) {
                removeChoiceQnames.add(i - 1);
            } else if (this.schemaContext.findDataTreeChild(qnamesCopy).get() instanceof ActionNodeContainer) {
                final ActionNodeContainer actionSchemaNode =
                        (ActionNodeContainer) this.schemaContext.findDataTreeChild(qnamesCopy).get();
                actions = actionSchemaNode.getActions();
            }
        }
        return inputOutputOther;
    }

    private RpcInputOutput getRpcInputOutput(final ArrayList<QName> qnames,
            final Collection<? extends ActionDefinition> actions, final RpcInputOutput inputOutputOther,
            final int iteration, final List<QName> qnamesCopy) {
        if (actions.isEmpty()) {
            return inputOutputOther;
        }
        for (ActionDefinition action : actions) {
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

    private String loadJS() {
        URL url = Resources.getResource("js");
        String text = "";
        try {
            text = Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Can not load text from js file");
        }

        return text;
    }

    private String prepareHeader(final Module module) {
        final StringBuilder nameRevision = new StringBuilder(module.getName());
        module.getRevision().ifPresent(value -> nameRevision.append("@").append(value));
        URL url = Resources.getResource("header");
        String text = "";
        try {
            text = Resources.toString(url, StandardCharsets.UTF_8);
            text = text.replace("<NAME_REVISION>", nameRevision);
            text = text.replace("<NAMESPACE>", module.getNamespace().toString());
            text = text.replace("<PREFIX>", module.getPrefix());
        } catch (IOException e) {
            LOG.error("Can not load text from header file");
        }

        return text;
    }

    private void resolveChildNodes(List<Line> lines, List<Integer> connections, SchemaNode node,
            RpcInputOutput inputOutput, List<Integer> removeChoiceQnames, List<QName> keys) {
        if (node instanceof DataNodeContainer) {
            final Iterator<? extends DataSchemaNode> childNodes = ((DataNodeContainer) node).getChildNodes().iterator();
            resolveDataNodeContainer(childNodes, lines, connections, inputOutput, removeChoiceQnames, keys);

        } else if (node instanceof ChoiceSchemaNode) {
            connections.add(0);
            final Collection<? extends CaseSchemaNode> cases = ((ChoiceSchemaNode) node).getCases();
            final Iterator<? extends CaseSchemaNode> iterator = cases.iterator();
            resolveChoiceSchemaNode(iterator, lines, connections, node, inputOutput, removeChoiceQnames);

        }
        // If action is in container or list
        if (node instanceof ActionNodeContainer) {
            resolveActionNodeContainer(lines, connections, node, removeChoiceQnames);
        }
    }

    private void resolveActionNodeContainer(final List<Line> lines, final List<Integer> connections,
            final SchemaNode node, final List<Integer> removeChoiceQnames) {
        for (ActionDefinition action : ((ActionNodeContainer) node).getActions()) {
            int id = 1;
            connections.add(0);
            connections.set(connections.size() - 1, id);
            HtmlLine htmlLine = new HtmlLine(new ArrayList<>(connections), action, RpcInputOutput.OTHER,
                    this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(), false);
            lines.add(htmlLine);
            final boolean inputExists = !action.getInput().getChildNodes().isEmpty();
            final boolean outputExists = !action.getOutput().getChildNodes().isEmpty();
            if (inputExists) {
                connections.add(1);
                htmlLine = new HtmlLine(new ArrayList<>(connections), action.getInput(), RpcInputOutput.INPUT,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(connections), action.getInput(), RpcInputOutput.INPUT,
                        removeChoiceQnames, Collections.emptyList());
                connections.remove(connections.size() - 1);
            }
            if (outputExists) {
                connections.add(1);
                htmlLine = new HtmlLine(new ArrayList<>(connections), action.getOutput(), RpcInputOutput.OUTPUT,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(connections), action.getOutput(), RpcInputOutput.OUTPUT,
                        removeChoiceQnames, Collections.emptyList());
                connections.remove(connections.size() - 1);
            }
            connections.remove(connections.size() - 1);
        }
    }

    private void resolveChoiceSchemaNode(final Iterator<? extends CaseSchemaNode> iterator, final List<Line> lines,
            final List<Integer> connections, final SchemaNode node, final RpcInputOutput inputOutput,
            final List<Integer> removeChoiceQnames) {
        int id = 1;
        removeChoiceQnames.add(((List) node.getPath().getPathFromRoot()).size() - 1);
        while (iterator.hasNext()) {
            final DataSchemaNode child = iterator.next();
            removeChoiceQnames.add(((List) child.getPath().getPathFromRoot()).size() - 1);
            connections.set(connections.size() - 1, id++);
            HtmlLine htmlLine = new HtmlLine(new ArrayList<>(connections), child, inputOutput, this.schemaContext,
                    removeChoiceQnames, namespacePrefix, Optional.empty(), false);
            lines.add(htmlLine);
            resolveChildNodes(lines, new ArrayList<>(connections), child, inputOutput, removeChoiceQnames,
                    Collections.emptyList());
            removeChoiceQnames.remove(Integer.valueOf(((List) child.getPath().getPathFromRoot()).size() - 1));
        }
        removeChoiceQnames.remove(Integer.valueOf(((List) node.getPath().getPathFromRoot()).size() - 1));
        // remove last
        connections.remove(connections.size() - 1);
    }

    private void resolveDataNodeContainer(final Iterator<? extends DataSchemaNode> childNodes,
            final List<Line> lines, final List<Integer> connections, final RpcInputOutput inputOutput,
            final List<Integer> removeChoiceQnames, final List<QName> keys) {
        int id = 1;
        connections.add(0);
        while (childNodes.hasNext()) {
            final DataSchemaNode child = childNodes.next();
            connections.set(connections.size() - 1, id++);
            HtmlLine htmlLine = new HtmlLine(new ArrayList<>(connections), child, inputOutput, this.schemaContext,
                    removeChoiceQnames, namespacePrefix, Optional.empty(), keys.contains(child.getQName()));
            lines.add(htmlLine);
            List<QName> keyDefinitions = Collections.emptyList();
            if (child instanceof ListSchemaNode) {
                keyDefinitions = ((ListSchemaNode) child).getKeyDefinition();
            }
            resolveChildNodes(lines, new ArrayList<>(connections), child, inputOutput, removeChoiceQnames,
                    keyDefinitions);
        }
        // remove last only if the conatiner is not root container
        if (connections.size() > 1) {
            connections.remove(connections.size() - 1);
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

    private static class SingletonListInitializer {

        private int id;

        SingletonListInitializer(int initialValue) {
            this.id = initialValue;
        }

        ArrayList<Integer> getSingletonListWithIncreasedValue() {
            return new ArrayList<>(Collections.singletonList(this.id++));
        }
    }
}
