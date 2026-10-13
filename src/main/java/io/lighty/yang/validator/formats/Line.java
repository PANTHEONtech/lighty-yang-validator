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
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.meta.DataSchemaCompat;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AnydataEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AnyxmlEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ChoiceEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DataTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.IfFeatureStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.LeafListEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ListEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.api.stmt.StatusEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;

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
    private final Optional<Boolean> resolvedConfig;
    Status status;
    String nodeName;
    String flag;
    String path;
    String typeName;

    Line(final LyvNodeData lyvNodeData, final RpcInputOutput inputOutput,
            final Map<XMLNamespace, String> namespacePrefix) {
        final EffectiveStatement<?, ?> statement = lyvNodeData.getStatement();
        status = status(statement);
        isMandatory = lyvNodeData.isNodeMandatory();
        isListOrLeafList = statement instanceof LeafListEffectiveStatement
                || statement instanceof ListEffectiveStatement;
        isChoice = statement instanceof ChoiceEffectiveStatement;
        isCase = statement instanceof CaseEffectiveStatement;
        nodeName = lyvNodeData.getQName().getLocalName();
        this.inputOutput = inputOutput;
        this.namespacePrefix = namespacePrefix;
        this.resolvedConfig = lyvNodeData.getResolvedConfig();
        resolveFlag(statement, lyvNodeData.getAbsolutePath(), lyvNodeData.getContext());
        resolvePathAndType(statement);
        resolveKeys(statement);
        resolveIfFeatures(statement);
    }

    static Status status(final EffectiveStatement<?, ?> statement) {
        return statement.findFirstEffectiveSubstatement(StatusEffectiveStatement.class)
                .map(StatusEffectiveStatement::argument)
                .orElse(Status.CURRENT);
    }

    protected abstract void resolveFlag(EffectiveStatement<?, ?> statement, Absolute absolutePath,
            EffectiveModelContext context);

    /**
     * Resolves the rw/ro-style flag from config for a plain data-tree node, returning whether {@code statement}
     * was one. RpcEffectiveStatement/ActionEffectiveStatement also implement DataSchemaCompat (via DataCompat),
     * so this excludes them explicitly rather than testing DataSchemaCompat alone (same pitfall as dataChildren()
     * elsewhere in this migration). Prefers the resolved config passed in via {@code LyvNodeData} (needed when
     * the node was reached through an augmentation's own child tree, whose own effectiveConfig() is not
     * applicable - same as inside a grouping), falling back to the node's own effectiveConfig() otherwise.
     */
    protected boolean resolveFlagForDataSchemaNode(final EffectiveStatement<?, ?> statement, final String config,
            final String noConfig) {
        if (!((statement instanceof DataTreeEffectiveStatement<?> || statement instanceof ChoiceEffectiveStatement)
                && statement instanceof DataSchemaCompat<?, ?> compat)) {
            return false;
        }
        if (resolvedConfig.orElseGet(() -> compat.toDataSchemaNode().effectiveConfig().orElse(Boolean.TRUE))) {
            flag = config;
        } else {
            flag = noConfig;
        }
        return true;
    }

    private void resolveIfFeatures(final EffectiveStatement<?, ?> statement) {
        final DeclaredStatement<?> declared = statement.declared();
        if (declared instanceof IfFeatureStatement.MultipleIn) {
            final var ifFeature = ((IfFeatureStatement.MultipleIn<?>) declared).ifFeatureStatements();
            ifFeatures.addAll(ifFeature);
        }
    }

    private void resolveKeys(final EffectiveStatement<?, ?> statement) {
        if (statement instanceof ListEffectiveStatement listStatement) {
            for (final QName qname : listStatement.findKeyStatement()
                    .map(key -> key.argument().asList()).orElse(List.of())) {
                keys.add(qname.getLocalName());
            }
        }
    }

    private void resolvePathAndType(final EffectiveStatement<?, ?> statement) {
        // TypeEffectiveStatement.typeDefinition() is the bare `type` statement's own definition; it does not
        // reflect a leaf's own `default`, which TypedDataSchemaNode.typeDefinition() layers on top of it (see
        // JsonTree's resolveChildMetadata() for the same finding).
        if (statement instanceof TypedDataSchemaNode typed) {
            final TypeDefinition<? extends TypeDefinition<?>> type = typed.typeDefinition();
            resolvePathAndTypeForDataSchemaNode(type);
        } else if (statement instanceof AnydataEffectiveStatement) {
            typeName = ANYDATA;
            path = null;
        } else if (statement instanceof AnyxmlEffectiveStatement) {
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
