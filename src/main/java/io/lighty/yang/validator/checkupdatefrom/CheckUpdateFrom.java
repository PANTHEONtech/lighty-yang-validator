/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.checkupdatefrom;

import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.addedMustError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.checkMustWarning;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.defaultError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.illegalConfigChangeError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.illegalConfigStateError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.lengthError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.mandatoryError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.maxElementsError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.minElementsError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingBitError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingEnumError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingNodeError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingOldRevision;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingRevision;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.nameError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.namespaceError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.patternError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.rangeError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.referenceError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.revisionError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.statusError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.typeError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.unitsError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC7950.addedWhenError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC7950.baseIdentityError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC7950.checkWhenWarning;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC7950.identityRefBaseError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC7950.missingBaseIdentityError;
import static io.lighty.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC7950.missingIdentityError;
import static org.opendaylight.yangtools.yang.model.ri.type.BaseTypes.baseTypeOf;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import io.lighty.yang.validator.GroupArguments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.argparse4j.impl.choice.CollectionArgumentChoice;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.ElementCountConstraint;
import org.opendaylight.yangtools.yang.model.api.ElementCountConstraintAware;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.MandatoryAware;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.MustConstraintAware;
import org.opendaylight.yangtools.yang.model.api.MustDefinition;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeAware;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.ModuleEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.RevisionStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LengthConstraint;
import org.opendaylight.yangtools.yang.model.api.type.LengthRestrictedTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.PatternConstraint;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;
import org.opendaylight.yangtools.yang.model.api.type.RangeRestrictedTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathExpression.QualifiedBound;

public class CheckUpdateFrom {

    private static final String MIN_ELEMENTS = "\nmin-elements: ";
    private static final String MAX_ELEMENTS = "\nmaX-elements: ";
    private static final String MUST = "\nmust: ";
    private static final String WHEN = "\nwhen: ";
    private static final String CONFIG = "\nconfig: ";
    private static final String FALSE = "false";
    private static final String DONT_EXISTS = "does not exists";
    private static final String RANGES = "\nranges: ";
    private static final String LENGTH = "\nlength: ";
    private static final String STATUS = "\nstatus: ";
    private static final RangeSet<Integer> INTEGER_ALLOWED_RANGES =
            ImmutableRangeSet.of(Range.closed(0, Integer.MAX_VALUE));

    private final SchemaInferenceStack oldSchemaIS;
    private final SchemaInferenceStack newSchemaIS;
    private final Module oldModule;
    private final Module newModule;
    private final boolean is7950;

    private final Set<CheckUpdateFromErrorRFC6020> errors = new LinkedHashSet<>();

    public CheckUpdateFrom(final EffectiveModelContext newContext, final String newModule,
            final EffectiveModelContext oldContext, final String oldModule, final int rfcVersion) {
        final String newModuleName = extractModuleName(newModule);
        final String oldModuleName = extractModuleName(oldModule);
        this.oldSchemaIS = SchemaInferenceStack.of(oldContext);
        this.newSchemaIS = SchemaInferenceStack.of(newContext);
        this.newModule = newContext.findModules(newModuleName).iterator().next();
        this.oldModule = oldContext.findModules(oldModuleName).iterator().next();
        this.is7950 = rfcVersion == 7950;
    }

    private String extractModuleName(final String module) {
        final String[] modRevArray = module.split("/");
        return modRevArray[modRevArray.length - 1].split("@")[0];
    }

    @SuppressWarnings("RegexpSinglelineJava")
    public void printErrors() {
        int order = 1;
        for (final CheckUpdateFromErrorRFC6020 err : errors) {
            err.print(order++);
        }
    }

    public void validate() {
        checkName();
        checkNamespace();
        checkRevision();
        if (is7950) {
            checkIdentities();
        }
        final Collection<? extends DataSchemaNode> childNodes = oldModule.getChildNodes();
        findNodesRecursively(childNodes);

        checkNotifications();
        checkAugmentations();
        checkRPCs();
        checkTypeDefs();

    }

    private void checkIdentities() {
        final Collection<? extends IdentitySchemaNode> oldIdentities = oldModule.getIdentities();
        final Collection<? extends IdentitySchemaNode> newIdentities = newModule.getIdentities();
        for (final IdentitySchemaNode oldIdentity : oldIdentities) {
            if (isIdentityNotFound(newIdentities, oldIdentity)) {
                errors.add(missingIdentityError().updateInformation(DONT_EXISTS, oldIdentity.getQName().toString()));
            }
        }

    }

    private boolean isIdentityNotFound(final Collection<? extends IdentitySchemaNode> newIdentities,
            final IdentitySchemaNode oldIdentity) {
        boolean identityNotFound = true;
        for (final IdentitySchemaNode newIdentity : newIdentities) {
            if (oldIdentity.getQName().getLocalName().equals(newIdentity.getQName().getLocalName())) {
                identityNotFound = false;
                checkBaseIdentities(oldIdentity, newIdentity);
                break;
            }
        }
        return identityNotFound;
    }

    private void checkBaseIdentities(final IdentitySchemaNode oldIdentity, final IdentitySchemaNode newIdentity) {
        final Collection<? extends IdentitySchemaNode> oldBaseIdentities = oldIdentity.getBaseIdentities();
        final Collection<? extends IdentitySchemaNode> newBaseIdentities = newIdentity.getBaseIdentities();
        if (oldBaseIdentities.size() > newBaseIdentities.size()) {
            errors.add(baseIdentityError().updateInformation(newBaseIdentities.toString(),
                    oldBaseIdentities.toString()));
        } else {
            for (final IdentitySchemaNode oldBaseIdentity : oldBaseIdentities) {
                boolean identityBaseNotFound = true;
                for (final IdentitySchemaNode newBaseIdentity : newBaseIdentities) {
                    if (oldBaseIdentity.getQName().getLocalName().equals(newBaseIdentity.getQName().getLocalName())) {
                        identityBaseNotFound = false;
                        break;
                    }
                }
                if (identityBaseNotFound) {
                    errors.add(missingBaseIdentityError()
                            .updateInformation(DONT_EXISTS, oldBaseIdentity.getQName().toString()));
                }
            }
        }
    }

    private void checkTypeDefs() {
        final Collection<? extends TypeDefinition<?>> oldTypeDefs = this.oldModule.getTypeDefinitions();
        final Collection<? extends TypeDefinition<?>> newTypeDefs = this.newModule.getTypeDefinitions();
        for (final TypeDefinition<?> oldTypeDef : oldTypeDefs) {
            for (final TypeDefinition<?> newTypeDef : newTypeDefs) {
                if (oldTypeDef.getQName().getLocalName().equals(newTypeDef.getQName().getLocalName())) {
                    checkTypeAware(oldTypeDef, newTypeDef);
                    checkStatus(oldTypeDef.getStatus(), newTypeDef.getStatus(),
                            Absolute.of(oldTypeDef.getQName()), Absolute.of(newTypeDef.getQName()));
                    break;
                }
            }
        }
    }

    private void checkRPCs() {
        final Collection<? extends RpcDefinition> oldRPCs = this.oldModule.getRpcs();
        final Collection<? extends RpcDefinition> newRPCs = this.newModule.getRpcs();
        for (final RpcDefinition oldRPC : oldRPCs) {
            this.oldSchemaIS.enterDataTree(QName.create(this.oldModule.getQNameModule(),
                    oldRPC.getQName().getLocalName()));
            boolean rpcFound = false;
            for (final RpcDefinition newRPC : newRPCs) {
                this.newSchemaIS.enterDataTree(QName.create(this.newModule.getQNameModule(),
                        newRPC.getQName().getLocalName()));
                if (oldRPC.equals(newRPC)) {
                    checkReference(oldRPC.getReference(), newRPC.getReference());
                    checkStatus(oldRPC.getStatus(), newRPC.getStatus(), this.oldSchemaIS.toSchemaNodeIdentifier(),
                            this.newSchemaIS.toSchemaNodeIdentifier());
                    findNodesRecursively(Collections.singletonList(oldRPC.getInput()));
                    findNodesRecursively(Collections.singletonList(oldRPC.getOutput()));
                    rpcFound = true;
                    break;
                }
                this.newSchemaIS.exit();
            }
            if (!rpcFound) {
                errors.add(missingNodeError().updateInformation("missing rpc node",
                        this.oldSchemaIS.toSchemaNodeIdentifier().toString()));
            }
            this.oldSchemaIS.exit();
        }
    }

    private void checkAugmentations() {
        final Collection<? extends AugmentationSchemaNode> oldAugmentations = this.oldModule.getAugmentations();
        final Collection<? extends AugmentationSchemaNode> newAugmentations = this.newModule.getAugmentations();
        for (final AugmentationSchemaNode oldAug : oldAugmentations) {
            boolean augFound = false;
            for (final AugmentationSchemaNode newAug : newAugmentations) {
                if (oldAug.getTargetPath().equals(newAug.getTargetPath())) {
                    checkReference(oldAug.getReference(), newAug.getReference());
                    checkStatus(oldAug.getStatus(), newAug.getStatus(), oldAug.getTargetPath(),
                            newAug.getTargetPath());
                    findNodesRecursively(oldAug.getChildNodes());
                    augFound = true;
                    break;
                }
            }
            if (!augFound) {
                errors.add(missingNodeError().updateInformation("missing augmentation node",
                        oldAug.getTargetPath().toString()));
            }
        }
    }

    private void checkNotifications() {
        final Collection<? extends NotificationDefinition> oldNotifications = this.oldModule.getNotifications();
        final Collection<? extends NotificationDefinition> newNotifications = this.newModule.getNotifications();
        for (final NotificationDefinition oldNotification : oldNotifications) {
            this.oldSchemaIS.enterDataTree(QName.create(this.oldModule.getQNameModule(),
                    oldNotification.getQName().getLocalName()));
            boolean notificationFound = false;
            for (final NotificationDefinition newNotification : newNotifications) {
                if (oldNotification.equals(newNotification)) {
                    this.newSchemaIS.enterDataTree(QName.create(this.newModule.getQNameModule(),
                            newNotification.getQName().getLocalName()));
                    checkReference(oldNotification.getReference(), newNotification.getReference());
                    checkStatus(oldNotification.getStatus(), newNotification.getStatus(),
                            this.oldSchemaIS.toSchemaNodeIdentifier(), this.newSchemaIS.toSchemaNodeIdentifier());
                    findNodesRecursively(oldNotification.getChildNodes());
                    notificationFound = true;
                    this.newSchemaIS.exit();
                    break;
                }
            }
            if (!notificationFound) {
                errors.add(missingNodeError().updateInformation("missing notification node",
                        this.oldSchemaIS.toSchemaNodeIdentifier().toString()));
            }
            this.oldSchemaIS.exit();
        }
    }

    private void findNodesRecursively(final Collection<? extends DataSchemaNode> childNodes) {
        for (final DataSchemaNode oldNode : childNodes) {
            this.oldSchemaIS.enterDataTree(oldNode.getQName());
            final DataSchemaNode newNode = getNodeFromNewModule(oldSchemaIS.toSchemaNodeIdentifier());
            if (newNode != null) {
                this.newSchemaIS.enterDataTree(newNode.getQName());
                checkReference(oldNode.getReference(), newNode.getReference());
                checkMust(oldNode, newNode);
                if (is7950) {
                    checkWhen(oldNode, newNode);
                }
                checkMandatory(oldNode, newNode);
                checkState(oldNode, newNode);
                checkStatus(oldNode.getStatus(), newNode.getStatus(), this.oldSchemaIS.toSchemaNodeIdentifier(),
                        this.newSchemaIS.toSchemaNodeIdentifier());
                if (oldNode instanceof ElementCountConstraintAware) {
                    checkMinElements(oldNode, newNode);
                    checkMaxElements(oldNode, newNode);
                }
                //     oldNode.getMi

                if (oldNode instanceof TypeAware) {
                    checkTypeAware(((TypeAware) oldNode).getType(), ((TypeAware) newNode).getType());
                }

                if (oldNode instanceof DataNodeContainer) {
                    findNodesRecursively(((DataNodeContainer) oldNode).getChildNodes());
                }
                this.newSchemaIS.exit();
            }
            this.oldSchemaIS.exit();
        }
    }

    private void checkTypeAware(final TypeDefinition<? extends TypeDefinition<?>> oldType,
            final TypeDefinition<? extends TypeDefinition<?>> newType) {
        final boolean isTypeError = checkType(oldType, newType);
        checkReference(oldType.getReference(), newType.getReference());
        checkDefault(oldType, newType);
        checkUnits(oldType, newType);
        if (!isTypeError) {
            if (is7950 && oldType instanceof IdentityrefTypeDefinition) {
                checkIdentityref((IdentityrefTypeDefinition) oldType, (IdentityrefTypeDefinition) newType);
            }
            if (oldType instanceof LengthRestrictedTypeDefinition) {
                checkLength((LengthRestrictedTypeDefinition<?>) oldType, (LengthRestrictedTypeDefinition<?>) newType);
            }
            if (oldType instanceof RangeRestrictedTypeDefinition) {
                checkRange(oldType, newType);
            }

            if (oldType instanceof EnumTypeDefinition) {
                checkEnumeration((EnumTypeDefinition) oldType, (EnumTypeDefinition) newType);
            } else if (oldType instanceof BitsTypeDefinition) {
                checkBits((BitsTypeDefinition) oldType, (BitsTypeDefinition) newType);
            } else if (oldType instanceof StringTypeDefinition) {
                checkPattern((StringTypeDefinition) oldType, (StringTypeDefinition) newType);
            }
        }
    }

    private void checkIdentityref(final IdentityrefTypeDefinition oldType, final IdentityrefTypeDefinition newType) {
        if (oldType.getIdentities().isEmpty()) {
            errors.add(identityRefBaseError());
        } else if (oldType.getIdentities().size() < newType.getIdentities().size()) {
            errors.add(identityRefBaseError().updateInformation(
                    newType.getIdentities().toString(),
                    oldType.getIdentities().toString()));
        }
    }

    private void checkMaxElements(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        final Optional<ElementCountConstraint> oldElementCountConstraint =
                ((ElementCountConstraintAware) oldNode).getElementCountConstraint();
        final Optional<ElementCountConstraint> newElementCountConstraint =
                ((ElementCountConstraintAware) newNode).getElementCountConstraint();
        if (newElementCountConstraint.isPresent() && oldElementCountConstraint.isPresent()) {
            final Integer newMaxElements = newElementCountConstraint.get().getMaxElements();
            final Integer oldMaxElements = oldElementCountConstraint.get().getMaxElements();
            if (newMaxElements != null && oldMaxElements != null && newMaxElements < oldMaxElements) {
                errors.add(maxElementsError().updateInformation(this.newSchemaIS.toSchemaNodeIdentifier()
                                + MAX_ELEMENTS + newMaxElements,
                        this.oldSchemaIS.toSchemaNodeIdentifier() + MAX_ELEMENTS + oldMaxElements));
            }
        }
    }

    private void checkMinElements(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        final Optional<ElementCountConstraint> oldElementCountConstraint =
                ((ElementCountConstraintAware) oldNode).getElementCountConstraint();
        final Optional<ElementCountConstraint> newElementCountConstraint =
                ((ElementCountConstraintAware) newNode).getElementCountConstraint();
        if (newElementCountConstraint.isPresent() && oldElementCountConstraint.isEmpty()) {
            if (newElementCountConstraint.get().getMinElements() != null) {
                errors.add(minElementsError().updateInformation(this.newSchemaIS.toSchemaNodeIdentifier()
                                + MIN_ELEMENTS + newElementCountConstraint.get().getMinElements(),
                        this.oldSchemaIS.toSchemaNodeIdentifier() + MIN_ELEMENTS + DONT_EXISTS));
            }
        } else if (newElementCountConstraint.isPresent()) {
            final Integer newMinElements = newElementCountConstraint.get().getMinElements();
            final Integer oldMinElements = oldElementCountConstraint.get().getMinElements();
            if (newMinElements != null && oldMinElements == null) {
                errors.add(minElementsError().updateInformation(this.newSchemaIS.toSchemaNodeIdentifier()
                                + MIN_ELEMENTS + newElementCountConstraint.get().getMinElements(),
                        this.oldSchemaIS.toSchemaNodeIdentifier() + MIN_ELEMENTS + DONT_EXISTS));
            } else if (newMinElements != null && oldMinElements < newMinElements) {
                errors.add(minElementsError().updateInformation(this.newSchemaIS.toSchemaNodeIdentifier()
                                + MIN_ELEMENTS + newElementCountConstraint.get().getMinElements(),
                        this.oldSchemaIS.toSchemaNodeIdentifier() + MIN_ELEMENTS
                                + oldElementCountConstraint.get().getMinElements()));
            }

        }
    }

    private boolean checkType(final TypeDefinition<?> oldNode, final TypeDefinition<?> newNode) {
        final TypeDefinition<?> oldBaseType = baseTypeOf(oldNode);
        final TypeDefinition<?> newBaseType = baseTypeOf(newNode);

        final String oldQname = oldBaseType.getQName().getLocalName();
        final String newQname = newBaseType.getQName().getLocalName();
        if (!oldQname.equals(newQname)) {
            final String oldPath = buildTypeDefinitionPath(oldNode, this.oldSchemaIS);
            final String newPath = buildTypeDefinitionPath(newNode, this.newSchemaIS);
            errors.add(typeError().updateInformation(newPath + "\ntype: " + newQname,
                    oldPath + "\ntype: " + oldQname));
            return true;
        }
        return false;
    }

    private void checkStatus(final Status oldStatus, final Status newStatus, final SchemaNodeIdentifier oldPath,
            final SchemaNodeIdentifier newPath) {
        if (oldStatus.compareTo(newStatus) > 0) {
            errors.add(statusError().updateInformation(newPath + STATUS + newStatus,
                    oldPath + STATUS + oldStatus));
        }
    }

    private void checkState(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        final boolean oldIsConfig = oldNode.isConfiguration();
        final boolean newIsConfig = newNode.isConfiguration();
        if ((!oldIsConfig && newIsConfig)
                && (newNode instanceof MandatoryAware && ((MandatoryAware) newNode).isMandatory())) {
            errors.add(illegalConfigStateError().updateInformation(this.newSchemaIS.toSchemaNodeIdentifier()
                            + CONFIG + "true mandatory true",
                    oldNode.getQName() + CONFIG + FALSE));
        } else if (oldIsConfig && !newIsConfig) {
            errors.add(illegalConfigChangeError().updateInformation(this.newSchemaIS.toSchemaNodeIdentifier()
                            + CONFIG + FALSE,
                    oldNode.getQName() + CONFIG + "true"));
        }
    }

    private void checkMandatory(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        if (oldNode instanceof MandatoryAware && newNode instanceof MandatoryAware) {
            final boolean oldMandatory = ((MandatoryAware) oldNode).isMandatory();
            final boolean newMandatory = ((MandatoryAware) newNode).isMandatory();
            if (!oldMandatory && newMandatory) {
                errors.add(mandatoryError().updateInformation(this.newSchemaIS.toSchemaNodeIdentifier()
                        + "\nmandatory: true", this.oldSchemaIS.toSchemaNodeIdentifier()
                        + "\nmandatroy: false"));
            }
        }
    }

    private void checkMust(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        if (newNode instanceof MustConstraintAware) {
            final Collection<? extends MustDefinition> newMust = ((MustConstraintAware) newNode).getMustConstraints();
            if (oldNode instanceof MustConstraintAware) {
                checkOldAndNewMust(oldNode, newMust);
            } else {
                errors.add(addedMustError().updateInformation(
                        this.newSchemaIS.toSchemaNodeIdentifier() + MUST + new ArrayList<>(newMust),
                        DONT_EXISTS));
            }
        }
    }

    private void checkOldAndNewMust(final DataSchemaNode oldNode, final Collection<? extends MustDefinition> newMust) {
        final Collection<? extends MustDefinition> oldMust = ((MustConstraintAware) oldNode).getMustConstraints();
        if (oldMust.size() < newMust.size()) {
            errors.add(addedMustError().updateInformation(
                    this.newSchemaIS.toSchemaNodeIdentifier() + MUST + getXpathStringFromMustCollection(newMust),
                    this.oldSchemaIS.toSchemaNodeIdentifier() + MUST + getXpathStringFromMustCollection(oldMust)));
        } else {
            for (final MustDefinition newMustDefinition : newMust) {
                if (!oldMust.contains(newMustDefinition)) {
                    errors.add(checkMustWarning().updateInformation(
                            this.newSchemaIS.toSchemaNodeIdentifier() + MUST + newMustDefinition.getXpath().toString(),
                            this.oldSchemaIS.toSchemaNodeIdentifier() + MUST + getXpathStringFromMustCollection(
                                    oldMust)));
                }
            }
        }
    }

    private String getXpathStringFromMustCollection(final Collection<? extends MustDefinition> must) {
        return "[" + must.stream()
                .map(t -> t.getXpath().toString())
                .collect(Collectors.joining(",")) + "]";
    }

    private void checkWhen(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        final Optional<? extends QualifiedBound> newWhen = newNode.getWhenCondition();
        final Optional<? extends QualifiedBound> oldWhen = oldNode.getWhenCondition();
        if (oldWhen.isEmpty() && newWhen.isPresent()) {
            errors.add(addedWhenError().updateInformation(
                    this.newSchemaIS.toSchemaNodeIdentifier() + WHEN + newWhen.get(),
                    this.oldSchemaIS.toSchemaNodeIdentifier() + WHEN + DONT_EXISTS));
        } else if (oldWhen.isPresent() && newWhen.isPresent()
                && (!oldWhen.get().toString().equals(newWhen.get().toString()))) {
            errors.add(checkWhenWarning().updateInformation(
                    this.newSchemaIS.toSchemaNodeIdentifier() + WHEN + newWhen.get(),
                    this.oldSchemaIS.toSchemaNodeIdentifier() + WHEN + oldWhen.get()));
        }
    }

    private void checkReference(final Optional<String> oldReference, final Optional<String> newReference) {
        if (oldReference.isPresent() && newReference.isEmpty()) {
            errors.add(referenceError().updateInformation(DONT_EXISTS, oldReference.get()));
        }
    }

    private void checkUnits(final TypeDefinition<?> oldNode, final TypeDefinition<?> newNode) {
        final Optional<String> oldUnit = oldNode.getUnits();
        final Optional<String> newUnit = newNode.getUnits();
        if (oldUnit.isPresent() && newUnit.isEmpty()) {
            final String oldPath = buildTypeDefinitionPath(oldNode, this.oldSchemaIS);
            errors.add(unitsError().updateInformation(DONT_EXISTS,
                    oldPath + "\nunits: " + oldUnit.get()));
        }
    }

    private void checkDefault(final TypeDefinition<?> oldNode, final TypeDefinition<?> newNode) {
        final Optional<?> oldDefault = oldNode.getDefaultValue();
        final Optional<?> newDefault = newNode.getDefaultValue();
        if (oldDefault.isPresent() && (newDefault.isEmpty() || !oldDefault.get().equals(newDefault.get()))) {
            final String oldPath = buildTypeDefinitionPath(oldNode, this.oldSchemaIS);
            errors.add(defaultError().updateInformation(DONT_EXISTS,
                    oldPath + "\ndefault: " + oldDefault.get()));
        }
    }

    private void checkRange(final TypeDefinition<?> oldNode, final TypeDefinition<?> newNode) {
        final Optional<RangeConstraint<?>> oldRange =
                ((RangeRestrictedTypeDefinition) oldNode).getRangeConstraint();
        final Optional<RangeConstraint<?>> newRange =
                ((RangeRestrictedTypeDefinition) newNode).getRangeConstraint();
        if (oldRange.isPresent()) {
            final String oldPath = buildTypeDefinitionPath(oldNode, this.oldSchemaIS);
            final String newPath = buildTypeDefinitionPath(newNode, this.newSchemaIS);
            if (newRange.isEmpty()) {
                errors.add(rangeError().updateInformation(DONT_EXISTS, oldPath + RANGES + oldRange.get()));
            } else {
                final RangeConstraint<?> oldRangeConstraint = oldRange.get();
                final RangeConstraint<?> newRangeConstraint = newRange.get();
                final Set<? extends Range<?>> newRangeSet = newRangeConstraint.getAllowedRanges().asRanges();
                final Set<? extends Range<?>> oldRangeSet = oldRangeConstraint.getAllowedRanges().asRanges();
                if (newRangeSet.containsAll(oldRangeSet)) {
                    checkReference(oldRangeConstraint.getReference(), newRangeConstraint.getReference());
                } else {
                    errors.add(rangeError().updateInformation(newPath + RANGES + newRangeSet,
                            oldPath + RANGES + oldRangeSet));
                }
            }
        }
    }

    private void checkLength(final LengthRestrictedTypeDefinition<?> oldNode,
            final LengthRestrictedTypeDefinition<?> newNode) {
        final Optional<LengthConstraint> oldLengths = oldNode.getLengthConstraint();
        final Optional<LengthConstraint> newLengths = newNode.getLengthConstraint();
        final String oldPath = buildTypeDefinitionPath(oldNode, this.oldSchemaIS);
        final String newPath = buildTypeDefinitionPath(newNode, this.newSchemaIS);

        if (oldLengths.isPresent()) {
            if (newLengths.isEmpty()) {
                if (!oldLengths.get().getAllowedRanges().equals(INTEGER_ALLOWED_RANGES)) {
                    errors.add(lengthError().updateInformation(DONT_EXISTS, oldPath + LENGTH
                                    + oldLengths.get().getAllowedRanges().toString()));
                }
            } else {
                final LengthConstraint oldLengthConstraint = oldLengths.get();
                final LengthConstraint newLengthConstraint = newLengths.get();
                final Set<Range<Integer>> newRangeSet = newLengthConstraint.getAllowedRanges().asRanges();
                final Set<Range<Integer>> oldRangeSet = oldLengthConstraint.getAllowedRanges().asRanges();
                if (newRangeSet.containsAll(oldRangeSet)) {
                    checkReference(oldLengthConstraint.getReference(), newLengthConstraint.getReference());
                } else {
                    errors.add(lengthError().updateInformation(newPath + LENGTH + newRangeSet,
                            oldPath + LENGTH + oldRangeSet));
                }
            }
        }
    }

    private void checkPattern(final StringTypeDefinition oldNode, final StringTypeDefinition newNode) {
        final List<PatternConstraint> oldPatterns = oldNode.getPatternConstraints();
        final List<PatternConstraint> newPatterns = newNode.getPatternConstraints();
        if (isPatternConstraintListSame(newPatterns, oldPatterns)) {
            for (int i = 0; i < oldPatterns.size(); i++) {
                checkReference(oldPatterns.get(i).getReference(), newPatterns.get(i).getReference());
            }
        } else {
            errors.add(patternError().updateInformation(patterConstraintListToString(newPatterns),
                    patterConstraintListToString(oldPatterns)));
        }
    }

    public boolean isPatternConstraintListSame(final List<PatternConstraint> oldPatterns,
            final List<PatternConstraint> newPatterns) {
        if (oldPatterns.size() != newPatterns.size()) {
            return false;
        }
        for (int i = 0; i < oldPatterns.size(); i++) {
            if (isPatternValuesSame(newPatterns.get(i), oldPatterns.get(i))) {
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean isPatternValuesSame(final PatternConstraint newPattern, final PatternConstraint oldPattern) {
        return newPattern.getErrorMessage().equals(oldPattern.getErrorMessage())
                && newPattern.getJavaPatternString().equals(oldPattern.getJavaPatternString())
                && newPattern.getRegularExpressionString().equals(oldPattern.getRegularExpressionString())
                && newPattern.getErrorAppTag().equals(oldPattern.getErrorAppTag())
                && newPattern.getDescription().equals(oldPattern.getDescription())
                && newPattern.getReference().equals(oldPattern.getReference());
    }

    private String patterConstraintListToString(final List<PatternConstraint> patterns) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (int i = 0; i < patterns.size(); i++) {
            final PatternConstraint pattern = patterns.get(i);
            stringBuilder.append("{regex=")
                    .append(pattern.getJavaPatternString());
            pattern.getErrorMessage().ifPresent(s -> stringBuilder.append(",")
                    .append("errorMessage=")
                    .append(s));
            pattern.getErrorAppTag().ifPresent(s -> stringBuilder.append(",")
                    .append("errorAppTag=")
                    .append(s));
            stringBuilder.append("}");

            if (i != patterns.size() - 1) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private void checkBits(final BitsTypeDefinition oldNode, final BitsTypeDefinition newNode) {
        final Collection<? extends BitsTypeDefinition.Bit> oldBits = oldNode.getBits();
        final Collection<? extends BitsTypeDefinition.Bit> newBits = newNode.getBits();
        for (final BitsTypeDefinition.Bit oldBit : oldBits) {
            boolean sameBit = false;
            for (final BitsTypeDefinition.Bit newBit : newBits) {
                if (oldBit.getName().equals(newBit.getName()) && oldBit.getPosition().equals(newBit.getPosition())) {
                    checkReference(oldBit.getReference(), newBit.getReference());
                    sameBit = true;
                }
            }
            if (!sameBit) {
                errors.add(missingBitError().updateInformation(newBits.toString(), oldBits.toString()));
            }
        }
    }

    private void checkEnumeration(final EnumTypeDefinition oldNode, final EnumTypeDefinition newNode) {
        final List<EnumTypeDefinition.EnumPair> oldValues = oldNode.getValues();
        final List<EnumTypeDefinition.EnumPair> newValues = newNode.getValues();
        if (newValues.containsAll(oldValues)) {
            for (final EnumTypeDefinition.EnumPair oldEnum : oldValues) {
                checkReference(oldEnum.getReference(), newValues.get(newValues.indexOf(oldEnum)).getReference());
            }
        } else {
            errors.add(missingEnumError().updateInformation(newValues.toString(), oldValues.toString()));
        }
    }

    private DataSchemaNode getNodeFromNewModule(final Absolute nodeAbsolutePath) {
        final List<QName> finalList = new LinkedList<>();
        for (final QName qname : nodeAbsolutePath.getNodeIdentifiers()) {
            finalList.add(QName.create(newModule.getNamespace(), newModule.getRevision(), qname.getLocalName()));
        }

        final Optional<DataSchemaNode> dataChildByName = newModule.findDataTreeChild(finalList);
        if (dataChildByName.isPresent()) {
            return dataChildByName.get();
        } else {
            errors.add(missingNodeError().updateInformation("missing node",
                    this.oldSchemaIS.toSchemaNodeIdentifier().toString()));
            return null;
        }
    }

    private void checkRevision() {
        final Optional<Revision> newOptionalRevision = this.newModule.getRevision();
        final Optional<Revision> oldOptionalRevision = this.oldModule.getRevision();
        if (newOptionalRevision.isEmpty()) {
            errors.add(missingRevision());
        } else {
            final Revision revision = newOptionalRevision.get();
            if (oldOptionalRevision.isPresent() && revision.compareTo(oldOptionalRevision.get()) < 1) {
                errors.add(revisionError().updateInformation(revision.toString(),
                        oldOptionalRevision.get().toString()));
            }

            final Collection<? extends RevisionStatement> revisionsNew =
                    Objects.requireNonNull(((ModuleEffectiveStatement) newModule).getDeclared()).getRevisions();
            final Collection<? extends RevisionStatement> revisionsOld =
                    Objects.requireNonNull(((ModuleEffectiveStatement) oldModule).getDeclared()).getRevisions();

            final List<Revision> newDates = revisionsNew
                    .stream()
                    .map(RevisionStatement::argument)
                    .collect(Collectors.toList());
            for (final RevisionStatement oldRev : revisionsOld) {
                if (!newDates.contains(oldRev.argument())) {
                    errors.add(missingOldRevision().updateInformation(DONT_EXISTS, oldRev.argument().toString()));
                }
            }
        }
    }

    private void checkName() {
        if (!this.newModule.getName().equals(this.oldModule.getName())) {
            errors.add(nameError().updateInformation(this.newModule.getName(), this.oldModule.getName()));
        }
    }

    private void checkNamespace() {
        if (!this.newModule.getNamespace().equals(this.oldModule.getNamespace())) {
            errors.add(namespaceError().updateInformation(this.newModule.getNamespace().toString(),
                    this.oldModule.getNamespace().toString()));
        }
    }

    public static GroupArguments getGroupArguments() {
        final GroupArguments groupArguments = new GroupArguments("check-update-from",
                "Check-update-from based arguments: ");
        groupArguments.addOption("check update from path is a colon (:)"
                        + " separated list of directories to search"
                        + " for yang modules used on \"old module\".",
                Arrays.asList("-P", "--check-update-from-path"), false, "*", Collections.emptyList(),
                new CollectionArgumentChoice<>(Collections.emptyList()), List.class);
        groupArguments.addOption("Choose the RFC (7950 or 6020)"
                        + " according to which check will be processed.",
                Collections.singletonList("--rfc-version"), false, "?", 6020,
                new CollectionArgumentChoice<>(Arrays.asList(6020, 7950)), Integer.TYPE);
        return groupArguments;
    }

    private String buildTypeDefinitionPath(final TypeDefinition<?> typeDefinition,
            final SchemaInferenceStack schemaIS) {
        if (schemaIS.isEmpty()) {
            return String.format("TypeDefinition: [%s]", typeDefinition.getQName());
        }
        if (schemaIS.toSchemaNodeIdentifier().lastNodeIdentifier().equals(typeDefinition.getQName())) {
            return schemaIS.toSchemaNodeIdentifier().toString();
        }
        return String.format("%s TypeDefinition: [%s]", schemaIS.toSchemaNodeIdentifier(),
                typeDefinition.getQName());
    }
}
