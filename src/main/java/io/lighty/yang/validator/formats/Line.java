/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import io.lighty.yang.validator.formats.utility.LyvNodeData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AnydataEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AnyxmlEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.IfFeatureAwareDeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.IfFeatureStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.spi.meta.AbstractDeclaredEffectiveStatement;

abstract class Line {

    private static final String BOOLEAN = "boolean";
    private static final String IDENTITYREF = "identityref";
    private static final String ANYXML = "<anyxml>";
    private static final String ANYDATA = "<anydata>";

    final RpcInputOutput inputOutput;
    final List<IfFeatureStatement> ifFeatures = new ArrayList<>();
    final List<String> keys = new ArrayList<>();
    final boolean isMandatory;
    final boolean isListOrLeafList;
    final boolean isChoice;
    final boolean isCase;
    private final Map<XMLNamespace, String> namespacePrefix;
    Status status;
    String nodeName;
    String flag;
    String path;
    String typeName;

    Line(final LyvNodeData lyvNodeData, final RpcInputOutput inputOutput,
            final Map<XMLNamespace, String> namespacePrefix) {
        final SchemaNode node = lyvNodeData.getNode();
        status = node.getStatus();
        isMandatory = lyvNodeData.isNodeMandatory();
        isListOrLeafList = node instanceof LeafListSchemaNode || node instanceof ListSchemaNode;
        isChoice = node instanceof ChoiceSchemaNode;
        isCase = node instanceof CaseSchemaNode;
        nodeName = node.getQName().getLocalName();
        this.inputOutput = inputOutput;
        this.namespacePrefix = namespacePrefix;
        resolveFlag(node, lyvNodeData.getAbsolutePath(), lyvNodeData.getContext());
        resolvePathAndType(node);
        resolveKeys(node);
        resolveIfFeatures(node);
    }

    protected abstract void resolveFlag(SchemaNode node, Absolute absolutePath, EffectiveModelContext context);

    protected void resolveFlagForDataSchemaNode(final DataSchemaNode dataSchemaNode, final String config,
            final String noConfig) {
        if (dataSchemaNode.isConfiguration()) {
            flag = config;
        } else {
            flag = noConfig;
        }
    }

    private void resolveIfFeatures(final SchemaNode node) {
        final DeclaredStatement<?> declared = getDeclared(node);
        if (declared instanceof IfFeatureAwareDeclaredStatement) {
            final var ifFeature = ((IfFeatureAwareDeclaredStatement<?>) declared).getIfFeatures();
            ifFeatures.addAll(ifFeature);
        }
    }

    private static DeclaredStatement<?> getDeclared(final SchemaNode node) {
        if (node instanceof AbstractDeclaredEffectiveStatement) {
            return ((AbstractDeclaredEffectiveStatement<?, ?>) node).getDeclared();
        }
        return null;
    }

    private void resolveKeys(final SchemaNode node) {
        if (node instanceof ListSchemaNode) {
            for (final QName qname : ((ListSchemaNode) node).getKeyDefinition()) {
                keys.add(qname.getLocalName());
            }
        }
    }

    private void resolvePathAndType(final SchemaNode node) {
        if (node instanceof TypedDataSchemaNode) {
            final TypeDefinition<? extends TypeDefinition<?>> type = ((TypedDataSchemaNode) node).getType();
            resolvePathAndTypeForDataSchemaNode(type);
        } else if (node instanceof AnydataEffectiveStatement) {
            typeName = ANYDATA;
            path = null;
        } else if (node instanceof AnyxmlEffectiveStatement) {
            typeName = ANYXML;
            path = null;
        } else {
            typeName = null;
            path = null;
        }
    }

    private void resolvePathAndTypeForDataSchemaNode(TypeDefinition<? extends TypeDefinition<?>> type) {
        if (type instanceof IdentityrefTypeDefinition) {
            typeName = IDENTITYREF;
        } else if (type instanceof BooleanTypeDefinition) {
            typeName = BOOLEAN;
        } else if (type.getBaseType() == null) {
            typeName = type.getQName().getLocalName();
        } else {
            if (nodeName.equals(type.getQName().getLocalName())) {
                type = type.getBaseType();
            }
            final String prefix = namespacePrefix.get(type.getQName().getNamespace());
            if (prefix == null || isBaseType(type)) {
                typeName = type.getQName().getLocalName();
            } else {
                typeName = prefix + ":" + type.getQName().getLocalName();
            }
        }
        if (type instanceof LeafrefTypeDefinition) {
            path = ((LeafrefTypeDefinition) type).getPathStatement().getOriginalString();
        } else {
            path = null;
        }
    }

    private static boolean isBaseType(final TypeDefinition<? extends TypeDefinition<?>> type) {
        TypeDefinition<?> baseType = type.getBaseType();
        if (baseType == null) {
            return true;
        }
        while (baseType != null) {
            if (!baseType.getQName().getLocalName().equals(type.getQName().getLocalName())) {
                return false;
            }
            baseType = baseType.getBaseType();
        }
        return true;
    }
}
