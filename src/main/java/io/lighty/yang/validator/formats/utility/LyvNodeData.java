/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats.utility;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerLike;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.MandatoryAware;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class LyvNodeData {

    private final boolean isKey;
    private final EffectiveModelContext context;
    private final SchemaNode node;
    private final Absolute absolutePath;

    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull SchemaNode node,
            final @NonNull LyvStack stack) {
        this(context, node, stack.toSchemaNodeIdentifier());
    }

    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull SchemaNode node,
            final @NonNull LyvStack stack, final @Nullable List<QName> keys) {
        this(context, node, stack.toSchemaNodeIdentifier(), keys);
    }

    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull SchemaNode node,
            final @NonNull Absolute absolutePath) {
        this(context, node, absolutePath, null);
    }

    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull SchemaNode node,
            final @NonNull Absolute absolutePath, final @Nullable List<QName> keys) {
        this.context = context;
        this.absolutePath = absolutePath;
        this.node = node;
        isKey = keys != null && keys.contains(node.getQName());
    }

    public EffectiveModelContext getContext() {
        return context;
    }

    public SchemaNode getNode() {
        return node;
    }

    public Absolute getAbsolutePath() {
        return absolutePath;
    }

    public boolean isNodeMandatory() {
        return node instanceof MandatoryAware && ((MandatoryAware) node).isMandatory()
                || node instanceof ContainerLike || node instanceof CaseSchemaNode
                || node instanceof NotificationDefinition || node instanceof ActionDefinition
                || node instanceof RpcDefinition || isKey;
    }
}
