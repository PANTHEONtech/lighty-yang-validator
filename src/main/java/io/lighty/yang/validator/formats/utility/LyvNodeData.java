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

    public LyvNodeData(@NonNull final EffectiveModelContext context, @NonNull final SchemaNode node,
            @Nullable final List<QName> keys, final Absolute absolutePath) {
        this.context = context;
        this.absolutePath = absolutePath;
        this.node = node;
        if (keys == null || keys.isEmpty()) {
            this.isKey = false;
        } else {
            this.isKey = keys.contains(node.getQName());
        }
    }

    public EffectiveModelContext getContext() {
        return this.context;
    }

    public SchemaNode getNode() {
        return this.node;
    }

    public Absolute getAbsolutePath() {
        return absolutePath;
    }

    public boolean isNodeMandatory() {
        return (this.node instanceof MandatoryAware && ((MandatoryAware) this.node).isMandatory())
                || this.node instanceof ContainerLike || this.node instanceof CaseSchemaNode
                || this.node instanceof NotificationDefinition || this.node instanceof ActionDefinition
                || this.node instanceof RpcDefinition || this.isKey;
    }
}
