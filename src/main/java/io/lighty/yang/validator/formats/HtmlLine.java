/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
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
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ActionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DescriptionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.InputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.NotificationEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.OutputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.RpcEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class HtmlLine extends Line {

    private static final String CONFIG = "config";
    private static final String NO_CONFIG = "no config";

    private final String description;
    private final List<Integer> ids;
    private final SchemaHtmlEnum schema;


    HtmlLine(final List<Integer> ids, final LyvNodeData lyvND, final RpcInputOutput inputOutput,
            final Map<XMLNamespace, String> namespacePrefix) {
        super(lyvND, inputOutput, namespacePrefix);
        this.ids = ids;
        final EffectiveStatement<?, ?> statement = lyvND.getStatement();
        description = description(statement);
        schema = getSchemaByStatement(statement);
        path = createPath(lyvND.getAbsolutePath().getNodeIdentifiers(), namespacePrefix, lyvND.getContext());
    }

    HtmlLine(final List<Integer> ids, final LyvNodeData lyvNodeData, final RpcInputOutput inputOutput,
            final Map<XMLNamespace, String> namespacePrefix, final AugmentationSchemaNode augment) {
        super(lyvNodeData, inputOutput, namespacePrefix);
        this.ids = ids;
        final Iterable<QName> pathFromRoot;
        description = augment.getDescription().orElse("");
        schema = SchemaHtmlEnum.AUGMENT;
        pathFromRoot = augment.getTargetPath().getNodeIdentifiers();
        nodeName = augment.getTargetPath().lastNodeIdentifier().getLocalName();
        status = augment.getStatus();
        flag = "";
        path = createPath(pathFromRoot, namespacePrefix, lyvNodeData.getContext());
    }

    private static String description(final EffectiveStatement<?, ?> statement) {
        for (final EffectiveStatement<?, ?> sub : statement.effectiveSubstatements()) {
            if (sub instanceof DescriptionEffectiveStatement description) {
                return description.argument();
            }
        }
        return "";
    }

    private static SchemaHtmlEnum getSchemaByStatement(final EffectiveStatement<?, ?> statement) {
        final var declared = statement.declared();
        if (declared == null) {
            if (statement instanceof CaseEffectiveStatement) {
                return SchemaHtmlEnum.CASE;
            } else if (statement instanceof InputEffectiveStatement) {
                return SchemaHtmlEnum.INPUT;
            } else if (statement instanceof OutputEffectiveStatement) {
                return SchemaHtmlEnum.OUTPUT;
            } else {
                return SchemaHtmlEnum.EMPTY;
            }
        }
        return SchemaHtmlEnum.getSchemaHtmlEnumByName(
                declared.statementDefinition().getStatementName().getLocalName());
    }

    private static String createPath(final Iterable<QName> pathFromRoot,
            final Map<XMLNamespace, String> namespacePrefix, final EffectiveModelContext context) {
        final StringBuilder pathBuilder = new StringBuilder();
        for (final QName path : pathFromRoot) {
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
    protected void resolveFlag(final EffectiveStatement<?, ?> statement, final Absolute absolutePath,
            final EffectiveModelContext context) {
        if (statement instanceof CaseEffectiveStatement || statement instanceof RpcEffectiveStatement
                || statement instanceof NotificationEffectiveStatement || statement instanceof ActionEffectiveStatement) {
            // do not emit the "config/no config" for rpc/action/notification/case
            flag = "";
        } else if (context.findNotification(absolutePath.firstNodeIdentifier()).isPresent()) {
            flag = NO_CONFIG;
        } else if (inputOutput == RpcInputOutput.INPUT) {
            flag = CONFIG;
        } else if (inputOutput == RpcInputOutput.OUTPUT) {
            flag = NO_CONFIG;
        } else if (!resolveFlagForDataSchemaNode(statement, CONFIG, NO_CONFIG)) {
            flag = CONFIG;
        }
    }
}
