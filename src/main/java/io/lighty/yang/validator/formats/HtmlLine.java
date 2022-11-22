/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import io.lighty.yang.validator.exceptions.NotFoundException;
import io.lighty.yang.validator.formats.utility.LyvNodeData;
import io.lighty.yang.validator.formats.utility.SchemaHtmlEnum;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.InputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.OutputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.spi.meta.AbstractUndeclaredEffectiveStatement;

public class HtmlLine extends Line {

    private static final String CONFIG = "config";
    private static final String NO_CONFIG = "no config";

    private final String description;
    private final List<Integer> ids;
    private final SchemaHtmlEnum schema;


    HtmlLine(final List<Integer> ids, final LyvNodeData lyvNodeData, final RpcInputOutput inputOutput,
            final List<Integer> removeChoiceQname, final Map<XMLNamespace, String> namespacePrefix) {
        super(lyvNodeData, inputOutput, removeChoiceQname, namespacePrefix);
        this.ids = ids;
        Iterable<QName> pathFromRoot;
        SchemaNode node = lyvNodeData.getNode();
        description = node.getDescription().orElse("");
        pathFromRoot = node.getPath().getPathFromRoot();
        schema = getSchemaBySchemaNode(node);
        path = createPath(pathFromRoot, namespacePrefix, lyvNodeData.getContext());
    }

    HtmlLine(final List<Integer> ids, final LyvNodeData lyvNodeData, final RpcInputOutput inputOutput,
            final List<Integer> removeChoiceQname, final Map<XMLNamespace, String> namespacePrefix,
            final AugmentationSchemaNode augment) {
        super(lyvNodeData, inputOutput, removeChoiceQname, namespacePrefix);
        this.ids = ids;
        Iterable<QName> pathFromRoot;
        description = augment.getDescription().orElse("");
        schema = SchemaHtmlEnum.AUGMENT;
        pathFromRoot = augment.getTargetPath().getNodeIdentifiers();
        nodeName = augment.getTargetPath().asSchemaPath().getLastComponent().getLocalName();
        status = augment.getStatus();
        flag = "";
        path = createPath(pathFromRoot, namespacePrefix, lyvNodeData.getContext());
    }

    private static SchemaHtmlEnum getSchemaBySchemaNode(final SchemaNode node) {
        if (node instanceof EffectiveStatement) {
            if (node instanceof AbstractUndeclaredEffectiveStatement) {
                if (node instanceof CaseEffectiveStatement) {
                    return SchemaHtmlEnum.CASE;
                } else if (node instanceof InputEffectiveStatement) {
                    return SchemaHtmlEnum.INPUT;
                } else if (node instanceof OutputEffectiveStatement) {
                    return SchemaHtmlEnum.OUTPUT;
                } else {
                    return SchemaHtmlEnum.EMPTY;
                }
            } else {
                return SchemaHtmlEnum.getSchemaHtmlEnumByName(
                        Objects.requireNonNull(((EffectiveStatement) node).getDeclared()).statementDefinition()
                                .getStatementName()
                                .getLocalName());
            }
        } else {
            return SchemaHtmlEnum.EMPTY;
        }
    }

    private static String createPath(final Iterable<QName> pathFromRoot,
            final Map<XMLNamespace, String> namespacePrefix, final SchemaContext context) {
        final StringBuilder pathBuilder = new StringBuilder();
        for (QName path : pathFromRoot) {
            final String prefix = namespacePrefix.getOrDefault(path.getNamespace(),
                    context.findModule(path.getModule())
                            .orElseThrow(() -> new NotFoundException("Module", path.getModule().toString()))
                            .getPrefix());

            pathBuilder.append('/')
                    .append(prefix)
                    .append(':')
                    .append(path.getLocalName());
        }
        return pathBuilder.toString();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        final String id = ids.stream().map(String::valueOf).collect(Collectors.joining("."));
        String pid = "";
        if (ids.size() > 1) {
            pid = ids.subList(0, ids.size() - 1)
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("."));
        }
        if (typeName == null) {
            typeName = "";
        }
        String key = "";
        if (!keys.isEmpty()) {
            key = "[" + String.join(",", keys) + "]";
        }
        builder.append("<tr data-node-id=\"")
                .append(id)
                .append("\" data-node-pid=\"")
                .append(pid)
                .append("\">")
                .append("<td title=\"")
                .append(description)
                .append("\">")
                .append(nodeName)
                .append(key);

        builder.append(schema.getHtmlValue());

        final String enclosingTd = "</td>";
        builder.append("<td>")
                .append(schema.getSchemaName())
                .append(enclosingTd)
                .append("<td>")
                .append(typeName)
                .append(enclosingTd)
                .append("<td>")
                .append(flag)
                .append(enclosingTd)
                .append("<td>");
        switch (status) {
            case CURRENT:
                builder.append("current");
                break;
            case OBSOLETE:
                builder.append("obsolete");
                break;
            case DEPRECATED:
                builder.append("deprecated");
                break;
            default:
                break;
        }
        builder.append(enclosingTd)
                .append("<td>")
                .append(path)
                .append(enclosingTd)
                .append("</tr>");

        return builder.toString();
    }

    @Override
    protected void resolveFlag(SchemaNode node, SchemaContext context) {
        if (node instanceof CaseSchemaNode || node instanceof RpcDefinition || node instanceof NotificationDefinition
                || node instanceof ActionDefinition) {
            // do not emit the "config/no config" for rpc/action/notification/case SchemaNode
            this.flag = "";
        } else if (context.findNotification(node.getPath().getPathFromRoot().iterator().next()).isPresent()) {
            this.flag = NO_CONFIG;
        } else if (this.inputOutput == RpcInputOutput.INPUT) {
            this.flag = CONFIG;
        } else if (this.inputOutput == RpcInputOutput.OUTPUT) {
            this.flag = NO_CONFIG;
        } else if (node instanceof DataSchemaNode) {
            resolveFlagForDataSchemaNode(node, context, CONFIG, NO_CONFIG);
        } else {
            this.flag = CONFIG;
        }
    }
}
