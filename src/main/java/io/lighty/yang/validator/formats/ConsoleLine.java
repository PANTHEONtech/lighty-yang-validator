/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import io.lighty.yang.validator.formats.utility.LyvNodeData;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.stmt.IfFeatureStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class ConsoleLine extends Line {

    private static final String RW = "rw";
    private static final String RO = "ro";
    private final List<Boolean> isConnected;

    ConsoleLine(final List<Boolean> isConnected, final LyvNodeData lyvNodeData, RpcInputOutput inputOutput,
            final Map<XMLNamespace, String> namespacePrefix) {
        super(lyvNodeData, inputOutput, namespacePrefix);
        this.isConnected = isConnected;
    }

    protected void resolveFlag(SchemaNode node, final Absolute absolutePath, EffectiveModelContext context) {
        if (node instanceof CaseSchemaNode) {
            this.flag = "";
        } else if (node instanceof NotificationDefinition) {
            this.flag = "-n";
        } else if (context.findNotification(absolutePath.firstNodeIdentifier()).isPresent()) {
            this.flag = RO;
        } else if (this.inputOutput == RpcInputOutput.INPUT) {
            this.flag = "-w";
        } else if (this.inputOutput == RpcInputOutput.OUTPUT) {
            this.flag = RO;
        } else if (node instanceof DataSchemaNode) {
            resolveFlagForDataSchemaNode((DataSchemaNode) node, RW, RO);
        } else {
            this.flag = "-x";
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("  ");
        builder.append(getConnectionStringBuilder());
        builder.append(getStatusStringBuilder());
        builder.append("--").append(flag);
        builder.append(" ");
        if (isChoice) {
            builder.append('(').append(nodeName).append(')');
        } else if (isCase) {
            builder.append(":(").append(nodeName).append(')');
        } else {
            builder.append(nodeName);
        }
        if (isListOrLeafList) {
            builder.append(getStringBuilderWithListOrLeafList());
        } else if (!isMandatory) {
            builder.append('?');
        }
        if (path != null) {
            builder.append("    -> ").append(path);
        } else if (typeName != null) {
            builder.append("       ").append(typeName);
        }
        final Iterator<IfFeatureStatement> ifFeaturesIterator = ifFeatures.iterator();
        if (ifFeaturesIterator.hasNext()) {
            builder.append(getBuilderWithRestOfTheFeature(ifFeaturesIterator));
        }
        return builder.toString();
    }

    private StringBuilder getBuilderWithRestOfTheFeature(final Iterator<IfFeatureStatement> ifFeaturesIterator) {
        final StringBuilder builder = new StringBuilder();
        builder.append(" {");
        while (ifFeaturesIterator.hasNext()) {
            builder.append(ifFeaturesIterator.next().rawArgument());
            if (ifFeaturesIterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("}?");
        return builder;
    }

    private StringBuilder getStringBuilderWithListOrLeafList() {
        final StringBuilder builder = new StringBuilder();
        builder.append('*');
        if (!keys.isEmpty()) {
            builder.append(" [");
            final Iterator<String> iterator = keys.iterator();
            while (iterator.hasNext()) {
                builder.append(iterator.next());
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(']');
        }
        return builder;
    }

    private StringBuilder getStatusStringBuilder() {
        final StringBuilder builder = new StringBuilder();
        switch (status) {
            case CURRENT:
                builder.append('+');
                break;
            case OBSOLETE:
                builder.append('o');
                break;
            case DEPRECATED:
                builder.append('x');
                break;
            default:
                break;
        }
        return builder;
    }

    private StringBuilder getConnectionStringBuilder() {
        final StringBuilder builder = new StringBuilder();
        for (boolean connection : isConnected) {
            if (connection) {
                builder.append('|');
            } else {
                builder.append(" ");
            }
            builder.append("  ");
        }
        return builder;
    }
}