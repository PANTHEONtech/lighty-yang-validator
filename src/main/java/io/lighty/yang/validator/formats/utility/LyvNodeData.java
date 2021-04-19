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
import org.opendaylight.yangtools.yang.model.api.MandatoryAware;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public class LyvNodeData {

    private final boolean isKey;
    private final SchemaContext context;
    private final SchemaNode node;

    public LyvNodeData(@NonNull final SchemaContext context, @NonNull final SchemaNode node,
            @Nullable final List<QName> keys) {
        this.context = context;
        this.node = node;
        if (keys == null || keys.isEmpty()) {
            this.isKey = false;
        } else {
            this.isKey = keys.contains(node.getQName());
        }
    }

    public SchemaContext getContext() {
        return this.context;
    }

    public SchemaNode getNode() {
        return this.node;
    }

    public boolean isNodeMandatory() {
        return (this.node instanceof MandatoryAware && ((MandatoryAware) this.node).isMandatory())
                || this.node instanceof ContainerLike || this.node instanceof CaseSchemaNode
                || this.node instanceof NotificationDefinition || this.node instanceof ActionDefinition
                || this.node instanceof RpcDefinition || this.isKey;
    }
}
