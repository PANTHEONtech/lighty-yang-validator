/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.yang.validator.GroupArguments;
import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.exceptions.NotFoundException;
import io.lighty.yang.validator.formats.utility.LyvStack;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.meta.DataSchemaCompat;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ActionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AnydataEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AnyxmlEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ChoiceEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ContainerEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DataTreeAwareEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DataTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DescriptionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.InputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.LeafEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.LeafListEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ListEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.NotificationEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.OutputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.RpcEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeAwareEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.StatusEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonTree extends FormatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(JsonTree.class);
    private static final String HELP_NAME = "json-tree";
    private static final String HELP_DESCRIPTION = "return json tree with module and node metadata";
    private static final String BASETYPENAMESPACE = "urn:ietf:params:xml:ns:yang:1";
    private static final String EARLIEST_REVISION = "1970-01-01";
    private static final String CHILDREN = "children";
    private static final String NAME = "name";
    private static final String REVISION = "revision";
    private static final String NAMESPACE = "namespace";
    private static final String CONFIG = "config";
    private static final String DESCRIPTION = "description";
    private static final String MODULE = "module";
    private static final String MODULE_STRING = "Module";
    private static final String TYPE_INFO = "type_info";
    private static final String STATUS = "status";
    private static final String CLASS = "class";
    private static final String NOTIFICATION = "notification";
    private static final String NOTIFICATIONS = NOTIFICATION + "s";
    private static final String PATH = "path";
    private static final String RPC = "rpc";
    private static final String RPCS = RPC + "s";
    private static final String AUG = "augmentation";
    private static final String AUGMENTS = "augments";
    private static final String ACTION = "action";
    private static final String EMPTY = "";
    private static final String OUTPUT_TEXT = "output";
    private static final String PREFIX = "prefix";
    private static final String CONTACT = "contact";
    private static final String TYPE = "type";
    private static final String DEFAULT = "default";
    private static final String BASE = "base";
    private static final String UNKNOWN = "unknown";
    private static final String SLASH = "/";
    private static final String COLON = ":";

    private JSONArray parsedModels;

    @Override
    void init(final EffectiveModelContext context, final SchemaTree schemaTree, final Configuration config) {
        super.init(context, schemaTree, config);
        this.parsedModels = new JSONArray();
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    @Override
    public void emitFormat(final Module module) {
        if (module != null) {
            final LyvStack stack = new LyvStack();
            final JSONObject moduleMetadata = resolveModuleMetadata(module);
            final JSONObject jsonTree = new JSONObject();

            appendChildNodesToJsonTree(module, jsonTree, stack);
            stack.clear();
            appendNotificationsToJsonTree(module, jsonTree, stack);
            stack.clear();
            appendRpcsToJsonTree(module, jsonTree, stack);
            stack.clear();

            for (final AugmentationSchemaNode augmentation : module.getAugmentations()) {
                stack.enter(augmentation.getTargetPath());
                final JSONObject augmentationJson = new JSONObject();
                final boolean isConfig = isAugmentConfig(augmentation);
                augmentationJson.put(CONFIG, isConfig);
                augmentationJson.put(STATUS, augmentation.getStatus().name());
                augmentationJson.put(DESCRIPTION, augmentation.getDescription().orElse(EMPTY));
                augmentationJson.put(STATUS, augmentation.getStatus().name());
                augmentationJson.put(CLASS, AUG);
                final String path = resolvePath(augmentation.getTargetPath());
                augmentationJson.put(PATH, path);
                augmentationJson.put(NAME, path);
                for (final SchemaTreeEffectiveStatement<?> child : dataChildren(augmentation.asEffectiveStatement())) {
                    if (isConfig) {
                        augmentationJson.append(CHILDREN, resolveChildMetadata(child, stack, null, Boolean.TRUE));
                    } else {
                        augmentationJson.append(CHILDREN,
                                resolveChildMetadata(child, stack, Boolean.FALSE, Boolean.FALSE));
                    }
                }

                appendActionsToAugmentationJson(augmentation, augmentationJson, stack);
                appendNotificationsToAugmentationJson(module, augmentationJson, stack);
                jsonTree.append(AUGMENTS, augmentationJson);
                stack.clear();
            }
            jsonTree.put(MODULE, moduleMetadata);
            parsedModels.put(jsonTree);
        } else {
            LOG.error("{}", EMPTY_MODULE_EXCEPTION);
        }
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    @Override
    public void close(final Collection<Module> modules) {
        LOG.info("{}", new JSONObject().put("parsed-models", parsedModels).toString(4));
        parsedModels.clear();
    }

    private void appendNotificationsToAugmentationJson(final Module module, final JSONObject augmentationJson,
            final LyvStack stack) {
        for (final NotificationDefinition notification : module.getNotifications()) {
            final JSONObject jsonNotification = new JSONObject();
            for (final SchemaTreeEffectiveStatement<?> node : dataChildren(notification.asEffectiveStatement())) {
                jsonNotification.append(CHILDREN, resolveChildMetadata(node, stack, Boolean.FALSE));
            }
            putNotificationDataToJsonNotification(notification, jsonNotification, stack);
            augmentationJson.append(NOTIFICATIONS, jsonNotification);
        }
    }

    private void appendActionsToAugmentationJson(final AugmentationSchemaNode augmentation,
            final JSONObject augmentationJson, final LyvStack stack) {
        for (final ActionEffectiveStatement action : actionChildren(augmentation.asEffectiveStatement())) {
            stack.enter(action);
            final JSONObject jsonModuleChildAction = new JSONObject();
            jsonModuleChildAction.put(NAME, action.argument().getLocalName());
            jsonModuleChildAction.put(DESCRIPTION, description(action).orElse(EMPTY));
            jsonModuleChildAction.put(STATUS, status(action).name());
            jsonModuleChildAction.put(TYPE_INFO, new JSONObject());
            jsonModuleChildAction.put(CLASS, ACTION);
            jsonModuleChildAction.put(PATH, resolvePath(stack));
            jsonModuleChildAction.append(CHILDREN, resolveChildMetadata(action.inputStatement(), stack));
            jsonModuleChildAction.append(CHILDREN, resolveChildMetadata(action.outputStatement(), stack, Boolean.FALSE));
            augmentationJson.append(CHILDREN, jsonModuleChildAction);
            stack.exit();
        }
    }

    private void appendRpcsToJsonTree(final Module module, final JSONObject jsonTree, final LyvStack stack) {
        for (final RpcDefinition rpc : module.getRpcs()) {
            final RpcEffectiveStatement statement = rpc.asEffectiveStatement();
            stack.enter(statement);
            final JSONObject jsonRpc = new JSONObject();
            jsonRpc.put(NAME, statement.argument().getLocalName());
            jsonRpc.put(DESCRIPTION, rpc.getDescription().orElse(EMPTY));
            jsonRpc.put(STATUS, rpc.getStatus().name());
            jsonRpc.put(TYPE_INFO, new JSONObject());
            jsonRpc.put(CLASS, RPC);
            jsonRpc.put(PATH, resolvePath(stack));
            jsonRpc.append(CHILDREN, resolveChildMetadata(statement.inputStatement(), stack));
            jsonRpc.append(CHILDREN, resolveChildMetadata(statement.outputStatement(), stack, Boolean.FALSE));
            jsonTree.append(RPCS, jsonRpc);
            stack.exit();
        }
    }

    private void appendNotificationsToJsonTree(final Module module, final JSONObject jsonTree, final LyvStack stack) {
        for (final NotificationDefinition notification : module.getNotifications()) {
            final NotificationEffectiveStatement statement = notification.asEffectiveStatement();
            stack.enter(statement);
            final JSONObject jsonNotification = new JSONObject();
            for (final SchemaTreeEffectiveStatement<?> node : dataChildren(statement)) {
                jsonNotification.append(CHILDREN, resolveChildMetadata(node, stack, Boolean.FALSE));
            }
            putNotificationDataToJsonNotification(notification, jsonNotification, stack);
            jsonTree.append(NOTIFICATIONS, jsonNotification);
            stack.exit();
        }
    }

    private static void putNotificationDataToJsonNotification(final NotificationDefinition notification,
            final JSONObject jsonNotification, final LyvStack stack) {
        jsonNotification.put(NAME, notification.asEffectiveStatement().argument().getLocalName());
        jsonNotification.put(DESCRIPTION, notification.getDescription().orElse(EMPTY));
        jsonNotification.put(STATUS, notification.getStatus().name());
        jsonNotification.put(TYPE_INFO, new JSONObject());
        jsonNotification.put(CLASS, NOTIFICATION);
        jsonNotification.put(PATH, stack.toSchemaNodeIdentifier());
    }

    private void appendChildNodesToJsonTree(final Module module, final JSONObject jsonTree, final LyvStack stack) {
        for (final SchemaTreeEffectiveStatement<?> node : dataChildren(module.asEffectiveStatement())) {
            jsonTree.append(CHILDREN, resolveChildMetadata(node, stack));
        }
    }

    private boolean isAugmentConfig(final AugmentationSchemaNode augmentation) {
        Collection<ActionEffectiveStatement> actions = List.of();
        boolean isAction = false;
        boolean initialized = false;
        DataTreeAwareEffectiveStatement<?, ?> current = null;
        for (final QName path : augmentation.getTargetPath().getNodeIdentifiers()) {
            if (isAction) {
                return !OUTPUT_TEXT.equals(path.getLocalName());
            }
            if (shouldSkipThisIteration(actions, path)) {
                isAction = true;
                continue;
            }

            if (!initialized) {
                current = modelContext.findModule(path.getModule()).map(Module::asEffectiveStatement).orElse(null);
                initialized = true;
            }
            final Optional<DataTreeEffectiveStatement<?>> child = current == null
                    ? Optional.empty() : current.findDataTreeNode(path);

            if (child.isPresent()) {
                final DataTreeEffectiveStatement<?> dataTreeChild = child.orElseThrow();
                if (dataTreeChild instanceof DataSchemaCompat<?, ?> compat) {
                    final Optional<Boolean> isConfig = compat.toDataSchemaNode().effectiveConfig();
                    if (isConfig.isPresent() && !isConfig.orElseThrow()) {
                        return false;
                    }
                }
                actions = actionChildren(dataTreeChild);
                current = dataTreeChild instanceof DataTreeAwareEffectiveStatement<?, ?> aware ? aware : null;
            }
        }
        return true;
    }

    private static boolean shouldSkipThisIteration(final Collection<ActionEffectiveStatement> actions,
            final QName path) {
        for (final ActionEffectiveStatement action : actions) {
            if (action.argument().getLocalName().equals(path.getLocalName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        return Optional.empty();
    }

    private JSONObject resolveChildMetadata(final SchemaTreeEffectiveStatement<?> statement, final LyvStack stack) {
        return resolveChildMetadata(statement, stack, null);
    }

    private JSONObject resolveChildMetadata(final SchemaTreeEffectiveStatement<?> statement, final LyvStack stack,
            final @Nullable Boolean isConfig) {
        return resolveChildMetadata(statement, stack, isConfig, Boolean.TRUE);
    }

    /**
     * Resolves a single node's JSON metadata. {@code statement} is used for structure/type/name/description (the
     * declared view, deviation-oblivious by design). {@code config} is either {@code isConfig} when forced, or
     * else looked up fresh through the effective model context via the node's own schema-tree position (which
     * includes {@code statement} itself, since {@code stack.enter(statement)} happens first) — a node reached only
     * through an augmentation's own child tree does not have its own effectiveConfig() applicable (same as inside a
     * grouping), so looking it up this way instead of calling {@code node.effectiveConfig()} directly is what
     * makes an explicit {@code config false;} on an augmented descendant visible. {@code ambientConfig} is the
     * fallback used when that lookup is absent (e.g. deviated away) or does not resolve a config value of its own.
     */
    private JSONObject resolveChildMetadata(final SchemaTreeEffectiveStatement<?> statement, final LyvStack stack,
            final @Nullable Boolean isConfig, final boolean ambientConfig) {
        stack.enter(statement);
        final boolean config = isConfig != null ? isConfig : resolveEffectiveConfig(stack).orElse(ambientConfig);
        final JSONObject jsonModuleChild = new JSONObject();
        jsonModuleChild.put(NAME, statement.argument().getLocalName());
        jsonModuleChild.put(CONFIG, config);
        jsonModuleChild.put(DESCRIPTION, description(statement).orElse(EMPTY));
        jsonModuleChild.put(STATUS, status(statement).name());
        jsonModuleChild.put(TYPE_INFO, new JSONObject());
        jsonModuleChild.put(CLASS, resolveNodeClass(statement));
        jsonModuleChild.put(PATH, resolvePath(stack));
        if (statement instanceof SchemaTreeAwareEffectiveStatement<?, ?>) {
            for (final ActionEffectiveStatement action : actionChildren(statement)) {
                stack.enter(action);
                final JSONObject jsonModuleChildAction = new JSONObject();
                jsonModuleChildAction.put(NAME, action.argument().getLocalName());
                jsonModuleChildAction.put(DESCRIPTION, description(action).orElse(EMPTY));
                jsonModuleChildAction.put(STATUS, status(action).name());
                jsonModuleChildAction.put(TYPE_INFO, new JSONObject());
                jsonModuleChildAction.put(PATH, resolvePath(stack));
                jsonModuleChildAction.put(CLASS, ACTION);
                jsonModuleChildAction.append(CHILDREN, resolveChildMetadata(action.inputStatement(), stack, isConfig));
                jsonModuleChildAction.append(CHILDREN, resolveChildMetadata(action.outputStatement(), stack, Boolean.FALSE));
                jsonModuleChild.append(CHILDREN, jsonModuleChildAction);
                stack.exit();
            }
            for (final SchemaTreeEffectiveStatement<?> child : dataChildren(statement)) {
                jsonModuleChild.append(CHILDREN, resolveChildMetadata(child, stack, isConfig, config));
            }
            //Left TypedDataSchemaNode because no EffectiveStatement replacement was found
        } else if (statement instanceof TypedDataSchemaNode typed) {
            jsonModuleChild.put(TYPE_INFO, resolveType(typed.typeDefinition()));
            jsonModuleChild.put(CHILDREN, Collections.emptyList());
        }
        stack.exit();
        return jsonModuleChild;
    }

    private static List<SchemaTreeEffectiveStatement<?>> dataChildren(final EffectiveStatement<?, ?> statement) {
        if (!(statement instanceof SchemaTreeAwareEffectiveStatement<?, ?> aware)) {
            return List.of();
        }
        final List<SchemaTreeEffectiveStatement<?>> children = new ArrayList<>();
        for (final SchemaTreeEffectiveStatement<?> child : aware.schemaTreeNodes()) {
            if (child instanceof DataTreeEffectiveStatement<?> || child instanceof ChoiceEffectiveStatement
                    || child instanceof CaseEffectiveStatement) {
                children.add(child);
            }
        }
        return children;
    }

    private static List<ActionEffectiveStatement> actionChildren(final EffectiveStatement<?, ?> statement) {
        if (!(statement instanceof SchemaTreeAwareEffectiveStatement<?, ?> aware)) {
            return List.of();
        }
        final List<ActionEffectiveStatement> actions = new ArrayList<>();
        for (final SchemaTreeEffectiveStatement<?> child : aware.schemaTreeNodes()) {
            if (child instanceof ActionEffectiveStatement action) {
                actions.add(action);
            }
        }
        return actions;
    }

    private static Optional<String> description(final EffectiveStatement<?, ?> statement) {
        for (final EffectiveStatement<?, ?> sub : statement.effectiveSubstatements()) {
            if (sub instanceof DescriptionEffectiveStatement description) {
                return Optional.of(description.argument());
            }
        }
        return Optional.empty();
    }

    private static Status status(final EffectiveStatement<?, ?> statement) {
        return statement.findFirstEffectiveSubstatement(StatusEffectiveStatement.class)
                .map(StatusEffectiveStatement::argument)
                .orElse(Status.CURRENT);
    }

    private JSONObject resolveType(final TypeDefinition<? extends TypeDefinition<?>> nodeType) {
        final JSONObject jsonLeafType = new JSONObject();
        final QName typeqName = nodeType.getQName();
        final int equals = typeqName.getNamespace().compareTo(XMLNamespace.of(BASETYPENAMESPACE));
        final String type;
        if (equals == 0) {
            type = typeqName.getLocalName();
        } else if (nodeType instanceof IdentityrefTypeDefinition) {
            type = typeqName.getLocalName();
            //Left as is because effectiveStatement does not have getIdentities().
            for (final IdentitySchemaNode base : ((IdentityrefTypeDefinition) nodeType).getIdentities()) {
                jsonLeafType.append(BASE, base.getQName().getLocalName());
            }
        } else {
            final String prefix = modelContext.findModule(typeqName.getNamespace(), typeqName.getRevision())
                    .orElseThrow(() -> new NotFoundException(MODULE_STRING, typeqName.getNamespace().toString()))
                    .getPrefix();
            type = prefix + COLON + typeqName.getLocalName();
        }
        jsonLeafType.put(DESCRIPTION, nodeType.getDescription().orElse(EMPTY));
        jsonLeafType.put(TYPE, type);
        nodeType.getDefaultValue().ifPresent(value -> jsonLeafType.put(DEFAULT, value));
        return jsonLeafType;
    }

    private static String resolveNodeClass(final SchemaTreeEffectiveStatement<?> statement) {
        if (statement instanceof ListEffectiveStatement) {
            return "list";
        } else if (statement instanceof ContainerEffectiveStatement || statement instanceof InputEffectiveStatement
                || statement instanceof OutputEffectiveStatement) {
            // ContainerLike's old-model equivalent: container, and an rpc/action's input/output
            return "container";
        } else if (statement instanceof LeafListEffectiveStatement) {
            return "leaf-list";
        } else if (statement instanceof LeafEffectiveStatement) {
            return "leaf";
        } else if (statement instanceof ChoiceEffectiveStatement) {
            return "choice";
        } else if (statement instanceof CaseEffectiveStatement) {
            return "case";
        } else if (statement instanceof AnyxmlEffectiveStatement) {
            return "anyxml";
        } else if (statement instanceof AnydataEffectiveStatement) {
            return "anydata";
        } else {
            LOG.warn("Node type unknown: {}", statement);
            return UNKNOWN;
        }
    }

    private static JSONObject resolveModuleMetadata(final Module module) {
        final JSONObject jsonModuleMetadata = new JSONObject();
        jsonModuleMetadata.put(NAME, module.getName());
        jsonModuleMetadata.put(REVISION, module.getRevision().orElse(Revision.of(EARLIEST_REVISION)).toString());
        jsonModuleMetadata.put(NAMESPACE, module.getNamespace());
        jsonModuleMetadata.put(PREFIX, module.getPrefix());
        jsonModuleMetadata.put(CONTACT, module.getContact().orElse(EMPTY));
        jsonModuleMetadata.put(DESCRIPTION, module.getDescription().orElse(EMPTY));
        return jsonModuleMetadata;
    }

    /**
     * Looks up {@code stack}'s current position through the effective model context's schema tree (which,
     * unlike {@link EffectiveModelContext#findDataTreeChild}, addresses choice/case directly instead of skipping
     * over them), returning its effectiveConfig() if resolved to a real, correctly-positioned DataSchemaNode.
     */
    //FIXME convert this to EffectiveStatement after YANGTOOLS-1271 is resolved.
    private Optional<Boolean> resolveEffectiveConfig(final LyvStack stack) {
        return modelContext.findSchemaTreeNode(stack.toSchemaNodeIdentifier())
                .filter(DataSchemaCompat.class::isInstance)
                .map(statement -> ((DataSchemaCompat<?, ?>) statement).toDataSchemaNode())
                .flatMap(DataSchemaNode::effectiveConfig);
    }

    private String resolvePath(final LyvStack stack) {
        return resolvePath(stack.toSchemaNodeIdentifier());
    }

    private String resolvePath(final SchemaNodeIdentifier pathFromRoot) {
        final StringBuilder path = new StringBuilder(SLASH);
        for (final QName pathQname : pathFromRoot.getNodeIdentifiers()) {
            modelContext.findModule(pathQname.getModule()).ifPresent(module1 -> path.append(module1.getPrefix()));
            // FIXME: this produces trailing slashes
            path.append(COLON).append(pathQname.getLocalName()).append(SLASH);
        }
        return path.toString();
    }
}
