/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.simplify;

import io.lighty.yang.validator.formats.utility.LyvStack;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.meta.DataSchemaCompat;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class SchemaTree implements Comparable<SchemaTree> {

    public static final Absolute ROOT = Absolute.of(QName.create("root", "root"));

    private final Absolute absolutePath;
    private final EffectiveStatement<?, ?> statement;
    private final boolean isRootNode;
    private final boolean isAugmenting;
    private final ActionDefinition actionNode;
    private final boolean isConfig;
    private final Set<SchemaTree> children = new LinkedHashSet<>();

    SchemaTree(final Absolute absolutePath, final EffectiveStatement<?, ?> statement,
            final boolean isRootNode, final boolean isAugmenting,
            final ActionDefinition actionNode) {
        this(absolutePath, statement, isRootNode, isAugmenting, actionNode, defaultConfig(statement));
    }

    SchemaTree(final Absolute absolutePath, final EffectiveStatement<?, ?> statement,
            final boolean isRootNode, final boolean isAugmenting,
            final ActionDefinition actionNode, final boolean isConfig) {
        this.absolutePath = absolutePath;
        this.statement = statement;
        this.isRootNode = isRootNode;
        this.isAugmenting = isAugmenting;
        this.actionNode = actionNode;
        this.isConfig = isConfig;
    }

    /**
     * The default config resolution: the node's own effectiveConfig(), defaulting to {@code true} when absent (no
     * explicit/inherited value, not applicable, or no node at all). Correct as long as {@code statement} is
     * already the real, correctly-positioned node - which does not hold for nodes reached only through an
     * augmentation's own child tree (same as inside a grouping); those must have their isConfig resolved through
     * the real, grafted position and passed in explicitly instead - see {@link SchemaSelector#noXml()}.
     */
    private static boolean defaultConfig(final EffectiveStatement<?, ?> statement) {
        if (statement instanceof DataSchemaCompat<?, ?> compat) {
            return compat.toDataSchemaNode().effectiveConfig().orElse(Boolean.TRUE);
        }
        return true;
    }

    public QName getQname() {
        return absolutePath.lastNodeIdentifier();
    }

    public Absolute getAbsolutePath() {
        return absolutePath;
    }

    public boolean isRootNode() {
        return isRootNode;
    }

    public boolean isAugmenting() {
        return isAugmenting;
    }

    public EffectiveStatement<?, ?> getEffectiveStatement() {
        return statement;
    }

    public boolean isConfig() {
        return isConfig;
    }

    public ActionDefinition getActionNode() {
        return actionNode;
    }

    public SchemaTree addChild(final SchemaTree tree) {
        if (children.add(tree)) {
            return tree;
        } else {
            return children.stream()
                    .filter(schemaTree -> schemaTree.equals(tree))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("Failed to add SchemaNode [%s] to SchemaTree path", absolutePath)));
        }
    }

    public SchemaTree addChild(final EffectiveStatement<?, ?> statementInput, final boolean isRootNodeInput,
            final boolean isAugmentingInput, final Absolute absolute) {
        final SchemaTree tree = new SchemaTree(absolute, statementInput, isRootNodeInput, isAugmentingInput, null);
        return addChild(tree);
    }

    /**
     * Same as {@link #addChild(EffectiveStatement, boolean, boolean, Absolute)}, but with an explicitly resolved
     * {@code isConfig} instead of deriving it from {@code statementInput} itself - see
     * {@link SchemaSelector#noXml()} for why that matters for augmentation content.
     */
    public SchemaTree addChild(final EffectiveStatement<?, ?> statementInput, final boolean isRootNodeInput,
            final boolean isAugmentingInput, final LyvStack stack, final boolean isConfig) {
        final SchemaTree tree = new SchemaTree(stack.toSchemaNodeIdentifier(), statementInput, isRootNodeInput,
            isAugmentingInput, null, isConfig);
        return addChild(tree);
    }

    SchemaTree addChild(final ActionDefinition schemaNodeInput, final boolean isRootNodeInput,
            final boolean augmentation, final LyvStack stack) {
        final SchemaTree tree = new SchemaTree(stack.toSchemaNodeIdentifier(), null, isRootNodeInput, augmentation,
            schemaNodeInput);
        return addChild(tree);
    }

    public Set<SchemaTree> getChildren() {
        return children;
    }

    public Set<SchemaTree> getEffectiveStatementChildren() {
        final Set<SchemaTree> ret = new LinkedHashSet<>();
        for (final SchemaTree child : children) {
            if (child.getEffectiveStatement() != null) {
                ret.add(child);
            }
        }
        return ret;
    }

    public Set<SchemaTree> getActionDefinitionChildren() {
        final Set<SchemaTree> ret = new LinkedHashSet<>();
        for (final SchemaTree child : children) {
            if (child.getActionNode() != null) {
                ret.add(child);
            }
        }
        return ret;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SchemaTree that = (SchemaTree) obj;
        return Objects.equals(absolutePath, that.absolutePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(absolutePath);
    }

    @Override
    public int compareTo(final SchemaTree originalTree) {
        return getQname().compareTo(originalTree.getQname());
    }
}

