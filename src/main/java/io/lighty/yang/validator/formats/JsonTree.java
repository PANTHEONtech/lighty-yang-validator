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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.ActionNodeContainer;
import org.opendaylight.yangtools.yang.model.api.AnydataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AnyxmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerLike;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
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
                for (final DataSchemaNode child : augmentation.getChildNodes()) {
                    if (isConfig) {
                        augmentationJson.append(CHILDREN, resolveChildMetadata(child, stack));
                    } else {
                        augmentationJson.append(CHILDREN, resolveChildMetadata(child, stack, Boolean.FALSE));
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
    public void close(Collection<Module> modules) {
        LOG.info("{}", new JSONObject().put("parsed-models", parsedModels).toString(4));
        parsedModels.clear();
    }

    private void appendNotificationsToAugmentationJson(final Module module, final JSONObject augmentationJson,
            final LyvStack stack) {
        for (final NotificationDefinition notification : module.getNotifications()) {
            final JSONObject jsonNotification = new JSONObject();
            for (final DataSchemaNode node : notification.getChildNodes()) {
                jsonNotification.append(CHILDREN, resolveChildMetadata(node, stack, Boolean.FALSE));
            }
            putNotificationDataToJsonNotification(notification, jsonNotification, stack);
            augmentationJson.append(NOTIFICATIONS, jsonNotification);
        }
    }

    private void appendActionsToAugmentationJson(final AugmentationSchemaNode augmentation,
            final JSONObject augmentationJson, final LyvStack stack) {
        for (final ActionDefinition child : augmentation.getActions()) {
            stack.enter(child);
            final JSONObject jsonModuleChildAction = new JSONObject();
            jsonModuleChildAction.put(NAME, child.getQName().getLocalName());
            jsonModuleChildAction.put(DESCRIPTION, child.getDescription().orElse(EMPTY));
            jsonModuleChildAction.put(STATUS, child.getStatus().name());
            jsonModuleChildAction.put(TYPE_INFO, new JSONObject());
            jsonModuleChildAction.put(CLASS, ACTION);
            jsonModuleChildAction.put(PATH, resolvePath(stack));
            jsonModuleChildAction.append(CHILDREN, resolveChildMetadata(child.getInput(), stack));
            jsonModuleChildAction.append(CHILDREN, resolveChildMetadata(child.getOutput(), stack, Boolean.FALSE));
            augmentationJson.append(CHILDREN, jsonModuleChildAction);
            stack.exit();
        }
    }

    private void appendRpcsToJsonTree(final Module module, final JSONObject jsonTree, final LyvStack stack) {
        for (final RpcDefinition rpc : module.getRpcs()) {
            stack.enter(rpc);
            final JSONObject jsonRpc = new JSONObject();
            jsonRpc.put(NAME, rpc.getQName().getLocalName());
            jsonRpc.put(DESCRIPTION, rpc.getDescription().orElse(EMPTY));
            jsonRpc.put(STATUS, rpc.getStatus().name());
            jsonRpc.put(TYPE_INFO, new JSONObject());
            jsonRpc.put(CLASS, RPC);
            jsonRpc.put(PATH, resolvePath(stack));
            jsonRpc.append(CHILDREN, resolveChildMetadata(rpc.getInput(), stack));
            jsonRpc.append(CHILDREN, resolveChildMetadata(rpc.getOutput(), stack, Boolean.FALSE));
            jsonTree.append(RPCS, jsonRpc);
            stack.exit();
        }
    }

    private void appendNotificationsToJsonTree(final Module module, final JSONObject jsonTree, final LyvStack stack) {
        for (final NotificationDefinition notification : module.getNotifications()) {
            stack.enter(notification);
            final JSONObject jsonNotification = new JSONObject();
            for (final DataSchemaNode node : notification.getChildNodes()) {
                jsonNotification.append(CHILDREN, resolveChildMetadata(node, stack, Boolean.FALSE));
            }
            putNotificationDataToJsonNotification(notification, jsonNotification, stack);
            jsonTree.append(NOTIFICATIONS, jsonNotification);
            stack.exit();
        }
    }

    private static void putNotificationDataToJsonNotification(final NotificationDefinition notification,
            final JSONObject jsonNotification, final LyvStack stack) {
        jsonNotification.put(NAME, notification.getQName().getLocalName());
        jsonNotification.put(DESCRIPTION, notification.getDescription().orElse(EMPTY));
        jsonNotification.put(STATUS, notification.getStatus().name());
        jsonNotification.put(TYPE_INFO, new JSONObject());
        jsonNotification.put(CLASS, NOTIFICATION);
        jsonNotification.put(PATH, stack.toSchemaNodeIdentifier());
    }

    private void appendChildNodesToJsonTree(final Module module, final JSONObject jsonTree, final LyvStack stack) {
        for (final DataSchemaNode node : module.getChildNodes()) {
            jsonTree.append(CHILDREN, resolveChildMetadata(node, stack));
        }
    }

    private boolean isAugmentConfig(final AugmentationSchemaNode augmentation) {
        final List<QName> qNames = new ArrayList<>();
        Collection<? extends ActionDefinition> actions = new HashSet<>();
        boolean isAction = false;
        for (final QName path : augmentation.getTargetPath().getNodeIdentifiers()) {
            if (isAction) {
                return !OUTPUT_TEXT.equals(path.getLocalName());
            }
            if (shouldSkipThisIteration(actions, path)) {
                isAction = true;
                continue;
            }

            // FIXME: This is inefficient: we end up re-looking up each previous QName, i.e. this has O(N!) complexity.
            //        We should use DataNodeContainer.findDataTreeChild(path) and iteratively move parent, i.e. we do
            //        not need qNames at all!
            qNames.add(path);
            final Optional<DataSchemaNode> optDataTreeChild = modelContext.findDataTreeChild(qNames);

            if (optDataTreeChild.isPresent()) {
                final DataSchemaNode dataTreeChild = optDataTreeChild.orElseThrow();
                final Optional<Boolean> isConfig = dataTreeChild.effectiveConfig();
                if (isConfig.isPresent() && !isConfig.orElseThrow()) {
                    return false;
                }
                if (dataTreeChild instanceof ActionNodeContainer) {
                    actions = ((ActionNodeContainer) dataTreeChild).getActions();
                }
            } else {
                qNames.remove(path);
            }
        }
        return true;
    }

    private static boolean shouldSkipThisIteration(final Collection<? extends ActionDefinition> actions,
            final QName path) {
        for (final ActionDefinition action : actions) {
            if (action.getQName().getLocalName().equals(path.getLocalName())) {
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

    private JSONObject resolveChildMetadata(final DataSchemaNode node, final LyvStack stack) {
        return resolveChildMetadata(node, stack, null);
    }

    private JSONObject resolveChildMetadata(final DataSchemaNode node, final LyvStack stack,
            final @Nullable Boolean isConfig) {
        final Boolean config = isConfig != null ? isConfig : node.isConfiguration();
        final JSONObject jsonModuleChild = new JSONObject();
        jsonModuleChild.put(NAME, node.getQName().getLocalName());
        jsonModuleChild.put(CONFIG, config);
        jsonModuleChild.put(DESCRIPTION, node.getDescription().orElse(EMPTY));
        jsonModuleChild.put(STATUS, node.getStatus().name());
        jsonModuleChild.put(TYPE_INFO, new JSONObject());
        jsonModuleChild.put(CLASS, resolveNodeClass(node));
        stack.enter(node);
        jsonModuleChild.put(PATH, resolvePath(stack));
        if (node instanceof ActionNodeContainer) {
            for (final ActionDefinition child : ((ActionNodeContainer) node).getActions()) {
                stack.enter(child);
                final JSONObject jsonModuleChildAction = new JSONObject();
                jsonModuleChildAction.put(NAME, child.getQName().getLocalName());
                jsonModuleChildAction.put(DESCRIPTION, child.getDescription().orElse(EMPTY));
                jsonModuleChildAction.put(STATUS, child.getStatus().name());
                jsonModuleChildAction.put(TYPE_INFO, new JSONObject());
                jsonModuleChildAction.put(PATH, resolvePath(stack));
                jsonModuleChildAction.put(CLASS, ACTION);
                jsonModuleChildAction.append(CHILDREN, resolveChildMetadata(child.getInput(), stack, isConfig));
                jsonModuleChildAction.append(CHILDREN, resolveChildMetadata(child.getOutput(), stack, Boolean.FALSE));
                jsonModuleChild.append(CHILDREN, jsonModuleChildAction);
                stack.exit();
            }
        }
        if (node instanceof DataNodeContainer) {
            for (final DataSchemaNode child : ((DataNodeContainer) node).getChildNodes()) {
                jsonModuleChild.append(CHILDREN, resolveChildMetadata(child, stack, isConfig));
            }
        } else if (node instanceof ChoiceSchemaNode) {
            for (final CaseSchemaNode caseNode : ((ChoiceSchemaNode) node).getCases()) {
                jsonModuleChild.append(CHILDREN, resolveChildMetadata(caseNode, stack, isConfig));
            }
        } else if (node instanceof TypedDataSchemaNode) {
            jsonModuleChild.put(TYPE_INFO, resolveType(((TypedDataSchemaNode) node).getType()));
            jsonModuleChild.put(CHILDREN, Collections.emptyList());
        }
        stack.exit();
        return jsonModuleChild;
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

    private static String resolveNodeClass(final DataSchemaNode node) {
        if (node instanceof ListSchemaNode) {
            return "list";
        } else if (node instanceof ContainerLike) {
            return "container";
        } else if (node instanceof LeafListSchemaNode) {
            return "leaf-list";
        } else if (node instanceof LeafSchemaNode) {
            return "leaf";
        } else if (node instanceof ChoiceSchemaNode) {
            return "choice";
        } else if (node instanceof CaseSchemaNode) {
            return "case";
        } else if (node instanceof AnyxmlSchemaNode) {
            return "anyxml";
        } else if (node instanceof AnydataSchemaNode) {
            return "anydata";
        } else {
            LOG.warn("Node type unknown: {}", node);
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
