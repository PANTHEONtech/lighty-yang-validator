/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats.utility;

import java.util.List;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.MandatoryAware;
import org.opendaylight.yangtools.yang.model.api.meta.DataSchemaCompat;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ActionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ContainerEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.InputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.NotificationEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.OutputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.RpcEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class LyvNodeData {

    private final boolean isKey;
    private final EffectiveModelContext context;
    private final EffectiveStatement<?, ?> statement;
    private final Absolute absolutePath;
    private final Boolean resolvedConfig;

    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull EffectiveStatement<?, ?> statement,
            final @NonNull LyvStack stack) {
        this(context, statement, stack.toSchemaNodeIdentifier());
    }

    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull EffectiveStatement<?, ?> statement,
            final @NonNull LyvStack stack, final @Nullable List<QName> keys) {
        this(context, statement, stack.toSchemaNodeIdentifier(), keys);
    }

    /**
     * Same as {@link #LyvNodeData(EffectiveModelContext, EffectiveStatement, LyvStack, List)}, but with an
     * explicitly resolved config value - see
     * {@link #LyvNodeData(EffectiveModelContext, EffectiveStatement, Absolute, List, Boolean)}.
     */
    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull EffectiveStatement<?, ?> statement,
            final @NonNull LyvStack stack, final @Nullable List<QName> keys, final @Nullable Boolean resolvedConfig) {
        this(context, statement, stack.toSchemaNodeIdentifier(), keys, resolvedConfig);
    }

    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull EffectiveStatement<?, ?> statement,
            final @NonNull Absolute absolutePath) {
        this(context, statement, absolutePath, null);
    }

    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull EffectiveStatement<?, ?> statement,
            final @NonNull Absolute absolutePath, final @Nullable List<QName> keys) {
        this(context, statement, absolutePath, keys, null);
    }

    /**
     * Same as {@link #LyvNodeData(EffectiveModelContext, EffectiveStatement, Absolute, List)}, but with an
     * explicitly resolved config value instead of leaving it to be derived from {@code statement} itself - needed
     * when {@code statement} was reached through an augmentation's own child tree, whose own effectiveConfig() is
     * not applicable (same as inside a grouping); see
     * {@link io.lighty.yang.validator.simplify.SchemaTree#isConfig()}.
     */
    public LyvNodeData(final @NonNull EffectiveModelContext context, final @NonNull EffectiveStatement<?, ?> statement,
            final @NonNull Absolute absolutePath, final @Nullable List<QName> keys,
            final @Nullable Boolean resolvedConfig) {
        this.context = context;
        this.absolutePath = absolutePath;
        this.statement = statement;
        this.resolvedConfig = resolvedConfig;
        isKey = keys != null && keys.contains((QName) statement.argument());
    }

    public EffectiveModelContext getContext() {
        return context;
    }

    public EffectiveStatement<?, ?> getStatement() {
        return statement;
    }

    public QName getQName() {
        return (QName) statement.argument();
    }

    public Absolute getAbsolutePath() {
        return absolutePath;
    }

    /**
     * The resolved config value, if explicitly provided at construction time; otherwise empty, meaning callers
     * should derive it from {@link #getStatement()} themselves (safe as long as the node is already at its real,
     * correctly-positioned location).
     */
    public Optional<Boolean> getResolvedConfig() {
        return Optional.ofNullable(resolvedConfig);
    }

    public boolean isNodeMandatory() {
        final boolean mandatoryAware = statement instanceof DataSchemaCompat<?, ?> compat
                && compat.toDataSchemaNode() instanceof MandatoryAware mandatoryNode && mandatoryNode.isMandatory();
        return mandatoryAware
                || statement instanceof ContainerEffectiveStatement || statement instanceof InputEffectiveStatement
                || statement instanceof OutputEffectiveStatement || statement instanceof CaseEffectiveStatement
                || statement instanceof NotificationEffectiveStatement
                || statement instanceof ActionEffectiveStatement || statement instanceof RpcEffectiveStatement
                || isKey;
    }
}
