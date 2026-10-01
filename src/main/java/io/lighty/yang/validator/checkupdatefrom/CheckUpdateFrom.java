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
import org.opendaylight.yangtools.yang.model.api.DocumentedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementEquivalent;
import org.opendaylight.yangtools.yang.model.api.ElementCountConstraintAware;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.MandatoryAware;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.MustDefinition;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypeDefinitionAware;
import org.opendaylight.yangtools.yang.model.api.meta.DataSchemaCompat;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.meta.ElementCountMatcher;
import org.opendaylight.yangtools.yang.model.api.stmt.AugmentEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ChoiceEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DataTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.IdentityEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ModuleEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ReferenceEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.RevisionStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeAwareEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.StatusEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.WhenEffectiveStatement;
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
    private final EffectiveModelContext newContext;
    private final Module oldModule;
    private final Module newModule;
    private final boolean is7950;

    private final Set<CheckUpdateFromErrorRFC6020> errors = new LinkedHashSet<>();

    public CheckUpdateFrom(final EffectiveModelContext newContext, final Module newModule,
            final EffectiveModelContext oldContext, final String oldModule, final int rfcVersion) {
        final String oldModuleName = extractModuleName(oldModule);
        oldSchemaIS = SchemaInferenceStack.of(oldContext);
        newSchemaIS = SchemaInferenceStack.of(newContext);
        this.newContext = newContext;
        this.newModule = newModule;
        this.oldModule = oldContext.findModules(oldModuleName).iterator().next();
        is7950 = rfcVersion == 7950;
    }

    private static String extractModuleName(final String module) {
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
        findNodesRecursively(dataChildren(oldModule.asEffectiveStatement()));

        checkNotifications();
        checkAugmentations();
        checkRPCs();
        checkTypeDefs();

    }

    private void checkIdentities() {
        final Collection<IdentityEffectiveStatement> oldIdentities = oldModule.asEffectiveStatement()
                .collectEffectiveSubstatements(IdentityEffectiveStatement.class);
        final Collection<IdentityEffectiveStatement> newIdentities = newModule.asEffectiveStatement()
                .collectEffectiveSubstatements(IdentityEffectiveStatement.class);
        for (final IdentityEffectiveStatement oldIdentity : oldIdentities) {
            if (isIdentityNotFound(newIdentities, oldIdentity)) {
                errors.add(missingIdentityError().updateInformation(DONT_EXISTS, oldIdentity.argument().toString()));
            }
        }

    }

    private boolean isIdentityNotFound(final Collection<IdentityEffectiveStatement> newIdentities,
            final IdentityEffectiveStatement oldIdentity) {
        boolean identityNotFound = true;
        for (final IdentityEffectiveStatement newIdentity : newIdentities) {
            if (oldIdentity.argument().getLocalName().equals(newIdentity.argument().getLocalName())) {
                identityNotFound = false;
                checkBaseIdentities(oldIdentity, newIdentity);
                break;
            }
        }
        return identityNotFound;
    }

    private void checkBaseIdentities(final IdentityEffectiveStatement oldIdentityStatement,
            final IdentityEffectiveStatement newIdentityStatement) {
        //left as is because no equivalent to getBaseIdentities() exists.
        final var oldBaseIdentities = oldIdentityStatement.toSchemaNode().getBaseIdentities();
        final var newBaseIdentities = newIdentityStatement.toSchemaNode().getBaseIdentities();
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
        final Collection<? extends TypeDefinition<?>> oldTypeDefs = oldModule.getTypeDefinitions();
        final Collection<? extends TypeDefinition<?>> newTypeDefs = newModule.getTypeDefinitions();
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
        final Collection<? extends RpcDefinition> oldRPCs = oldModule.getRpcs();
        final Collection<? extends RpcDefinition> newRPCs = newModule.getRpcs();
        for (final RpcDefinition oldRPC : oldRPCs) {
            oldSchemaIS.enterDataTree(QName.create(oldModule.getQNameModule(),
                    oldRPC.getQName().getLocalName()));
            boolean rpcFound = false;
            for (final RpcDefinition newRPC : newRPCs) {
                newSchemaIS.enterDataTree(QName.create(newModule.getQNameModule(),
                        newRPC.getQName().getLocalName()));
                if (oldRPC.equals(newRPC)) {
                    checkReference(oldRPC.getReference(), newRPC.getReference());
                    checkStatus(oldRPC.getStatus(), newRPC.getStatus(), oldSchemaIS.toSchemaNodeIdentifier(),
                            newSchemaIS.toSchemaNodeIdentifier());
                    findNodesRecursively(Collections.singletonList(oldRPC.getInput().asEffectiveStatement()));
                    findNodesRecursively(Collections.singletonList(oldRPC.getOutput().asEffectiveStatement()));
                    rpcFound = true;
                    break;
                }
                newSchemaIS.exit();
            }
            if (!rpcFound) {
                errors.add(missingNodeError().updateInformation("missing rpc node",
                        oldSchemaIS.toSchemaNodeIdentifier().toString()));
            }
            oldSchemaIS.exit();
        }
    }

    private void checkAugmentations() {
        final Collection<AugmentEffectiveStatement> oldAugmentations = oldModule.asEffectiveStatement()
                .collectEffectiveSubstatements(AugmentEffectiveStatement.class);
        final Collection<AugmentEffectiveStatement> newAugmentations = newModule.asEffectiveStatement()
                .collectEffectiveSubstatements(AugmentEffectiveStatement.class);
        for (final AugmentEffectiveStatement oldAug : oldAugmentations) {
            boolean augFound = false;
            for (final AugmentEffectiveStatement newAug : newAugmentations) {
                if (oldAug.argument().equals(newAug.argument())) {
                    checkReference(reference(oldAug), reference(newAug));
                    checkStatus(status(oldAug), status(newAug), oldAug.argument(), newAug.argument());
                    oldSchemaIS.enterSchemaTree(oldAug.argument());
                    newSchemaIS.enterSchemaTree(newAug.argument());
                    findNodesRecursively(dataChildren(oldAug));
                    oldSchemaIS.clear();
                    newSchemaIS.clear();
                    augFound = true;
                    break;
                }
            }
            if (!augFound) {
                errors.add(missingNodeError().updateInformation("missing augmentation node",
                        oldAug.argument().toString()));
            }
        }
    }

    private static Status status(final EffectiveStatement<?, ?> statement) {
        return statement.findFirstEffectiveSubstatement(StatusEffectiveStatement.class)
                .map(StatusEffectiveStatement::argument)
                .orElse(Status.CURRENT);
    }

    private static Optional<String> reference(final EffectiveStatement<?, ?> statement) {
        return statement.findFirstEffectiveSubstatementArgument(ReferenceEffectiveStatement.class);
    }

    private void checkNotifications() {
        final Collection<? extends NotificationDefinition> oldNotifications = oldModule.getNotifications();
        final Collection<? extends NotificationDefinition> newNotifications = newModule.getNotifications();
        for (final NotificationDefinition oldNotification : oldNotifications) {
            oldSchemaIS.enterDataTree(QName.create(oldModule.getQNameModule(),
                    oldNotification.getQName().getLocalName()));
            boolean notificationFound = false;
            for (final NotificationDefinition newNotification : newNotifications) {
                if (oldNotification.equals(newNotification)) {
                    newSchemaIS.enterDataTree(QName.create(newModule.getQNameModule(),
                            newNotification.getQName().getLocalName()));
                    checkReference(oldNotification.getReference(), newNotification.getReference());
                    checkStatus(oldNotification.getStatus(), newNotification.getStatus(),
                            oldSchemaIS.toSchemaNodeIdentifier(), newSchemaIS.toSchemaNodeIdentifier());
                    findNodesRecursively(dataChildren(oldNotification.asEffectiveStatement()));
                    notificationFound = true;
                    newSchemaIS.exit();
                    break;
                }
            }
            if (!notificationFound) {
                errors.add(missingNodeError().updateInformation("missing notification node",
                        oldSchemaIS.toSchemaNodeIdentifier().toString()));
            }
            oldSchemaIS.exit();
        }
    }

    private void findNodesRecursively(final Collection<? extends DataSchemaCompat<?, ?>> childNodes) {
        for (final DataSchemaCompat<?, ?> oldNode : childNodes) {
            oldSchemaIS.enterDataTree((QName) oldNode.argument());
            final EffectiveStatement<?, ?> newStatement = getNodeFromNewModule(oldSchemaIS.toSchemaNodeIdentifier());
            if (newStatement != null) {
                newSchemaIS.enterDataTree((QName) newStatement.argument());
                checkReference(reference(oldSchemaIS.currentStatement()), reference(newStatement));
                checkMust();
                if (is7950) {
                    checkWhen();
                }
                checkMandatory();
                checkState();
                checkStatus(((DocumentedNode.WithStatus) oldNode).getStatus(),
                        ((DocumentedNode.WithStatus) newStatement).getStatus(),
                        oldSchemaIS.toSchemaNodeIdentifier(), newSchemaIS.toSchemaNodeIdentifier());
                if (oldNode instanceof ElementCountConstraintAware) {
                    checkMinElements();
                    checkMaxElements();
                }

                if (oldNode instanceof TypeDefinitionAware tda) {
                    checkTypeAware(tda.typeDefinition(), ((TypeDefinitionAware) newStatement).typeDefinition());
                }

                findNodesRecursively(dataChildren(oldNode));
                newSchemaIS.exit();
            }
            oldSchemaIS.exit();
        }
    }

    private static List<DataSchemaCompat<?, ?>> dataChildren(final EffectiveStatement<?, ?> statement) {
        if (!(statement instanceof SchemaTreeAwareEffectiveStatement<?, ?> aware)) {
            return List.of();
        }
        final List<DataSchemaCompat<?, ?>> children = new ArrayList<>();
        for (final SchemaTreeEffectiveStatement<?> child : aware.schemaTreeNodes()) {
            if ((child instanceof DataTreeEffectiveStatement<?> || child instanceof ChoiceEffectiveStatement
                    || child instanceof CaseEffectiveStatement) && child instanceof DataSchemaCompat<?, ?> dataCompat) {
                children.add(dataCompat);
            }
        }
        return children;
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

    // left as is because EffectiveStatement does not have getIdentities()
    private void checkIdentityref(final IdentityrefTypeDefinition oldType, final IdentityrefTypeDefinition newType) {
        if (oldType.getIdentities().isEmpty()) {
            errors.add(identityRefBaseError());
        } else if (oldType.getIdentities().size() < newType.getIdentities().size()) {
            errors.add(identityRefBaseError().updateInformation(
                    newType.getIdentities().toString(),
                    oldType.getIdentities().toString()));
        }
    }

    private void checkMaxElements() {
        final ElementCountMatcher oldMatcher =
                ((ElementCountConstraintAware) oldSchemaIS.currentStatement()).elementCountMatcher();
        final ElementCountMatcher newMatcher =
                ((ElementCountConstraintAware) newSchemaIS.currentStatement()).elementCountMatcher();

        final Integer oldMaxElements = extractMaxElements(oldMatcher);
        final Integer newMaxElements = extractMaxElements(newMatcher);

        if (newMaxElements != null && oldMaxElements != null && newMaxElements < oldMaxElements) {
            errors.add(maxElementsError().updateInformation(newSchemaIS.toSchemaNodeIdentifier()
                    + MAX_ELEMENTS + newMaxElements,
                oldSchemaIS.toSchemaNodeIdentifier() + MAX_ELEMENTS + oldMaxElements));
        }
    }

    private void checkMinElements() {
        final ElementCountMatcher oldMatcher =
                ((ElementCountConstraintAware) oldSchemaIS.currentStatement()).elementCountMatcher();
        final ElementCountMatcher newMatcher =
                ((ElementCountConstraintAware) newSchemaIS.currentStatement()).elementCountMatcher();

        final Integer oldMinElements = extractMinElements(oldMatcher);
        final Integer newMinElements = extractMinElements(newMatcher);

        if (newMinElements != null && oldMinElements == null) {
            errors.add(minElementsError().updateInformation(newSchemaIS.toSchemaNodeIdentifier()
                    + MIN_ELEMENTS + newMinElements,
                oldSchemaIS.toSchemaNodeIdentifier() + MIN_ELEMENTS + DONT_EXISTS));
        } else if (newMinElements != null && oldMinElements != null && oldMinElements < newMinElements) {
            errors.add(minElementsError().updateInformation(newSchemaIS.toSchemaNodeIdentifier()
                    + MIN_ELEMENTS + newMinElements,
                oldSchemaIS.toSchemaNodeIdentifier() + MIN_ELEMENTS + oldMinElements));
        }
    }

    private Integer extractMaxElements(final ElementCountMatcher matcher) {
        if (matcher instanceof ElementCountMatcher.AtMost) {
            return ((ElementCountMatcher.AtMost) matcher).asSaturatedInt();
        } else if (matcher instanceof ElementCountMatcher.InRange) {
            return ((ElementCountMatcher.InRange) matcher).atMost().asSaturatedInt();
        }
        return null;
    }

    private Integer extractMinElements(final ElementCountMatcher matcher) {
        // AtLeast.lowerInt() is documented as "the next lower-than-this bound", i.e. min-elements - 1
        // (min-elements == 0 <=> lowerInt() == -1), so the actual min-elements value is lowerInt() + 1.
        if (matcher instanceof ElementCountMatcher.AtLeast atLeast) {
            return atLeast.lowerInt() + 1;
        } else if (matcher instanceof ElementCountMatcher.InRange inRange) {
            return inRange.atLeast().lowerInt() + 1;
        }
        return null;
    }

    private boolean checkType(final TypeDefinition<?> oldNode, final TypeDefinition<?> newNode) {
        final TypeDefinition<?> oldBaseType = baseTypeOf(oldNode);
        final TypeDefinition<?> newBaseType = baseTypeOf(newNode);

        final String oldQname = oldBaseType.getQName().getLocalName();
        final String newQname = newBaseType.getQName().getLocalName();
        if (!oldQname.equals(newQname)) {
            final String oldPath = buildTypeDefinitionPath(oldNode, oldSchemaIS);
            final String newPath = buildTypeDefinitionPath(newNode, newSchemaIS);
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

    private void checkState() {
        final boolean oldIsConfig = effectiveConfig(oldSchemaIS.currentStatement());
        final boolean newIsConfig = effectiveConfig(newSchemaIS.currentStatement());
        final EffectiveStatement<?, ?> newStatement = newSchemaIS.currentStatement();
        if (!oldIsConfig && newIsConfig
                && newStatement instanceof MandatoryAware mandatoryAware && mandatoryAware.isMandatory()) {
            errors.add(illegalConfigStateError().updateInformation(newSchemaIS.toSchemaNodeIdentifier()
                            + CONFIG + "true mandatory true",
                    oldSchemaIS.currentStatement().argument() + CONFIG + FALSE));
        } else if (oldIsConfig && !newIsConfig) {
            errors.add(illegalConfigChangeError().updateInformation(newSchemaIS.toSchemaNodeIdentifier()
                            + CONFIG + FALSE,
                    oldSchemaIS.currentStatement().argument() + CONFIG + "true"));
        }
    }

    // left as is because effectiveConfig() has no EffectiveStatement equivalent
    private static boolean effectiveConfig(final EffectiveStatement<?, ?> statement) {
        if (statement instanceof DataSchemaCompat<?, ?> compat) {
            return compat.toDataSchemaNode().effectiveConfig().orElse(Boolean.TRUE);
        }
        return true;
    }

    private void checkMandatory() {
        final var oldNode = oldSchemaIS.currentStatement();
        final var newNode = newSchemaIS.currentStatement();
        if (oldNode instanceof MandatoryAware && newNode instanceof MandatoryAware) {
            final boolean oldMandatory = ((MandatoryAware) oldNode).isMandatory();
            final boolean newMandatory = ((MandatoryAware) newNode).isMandatory();
            if (!oldMandatory && newMandatory) {
                errors.add(mandatoryError().updateInformation(newSchemaIS.toSchemaNodeIdentifier()
                        + "\nmandatory: true", oldSchemaIS.toSchemaNodeIdentifier()
                        + "\nmandatroy: false"));
            }
        }
    }

    private void checkMust() {
        final Collection<? extends MustDefinition> newMust =
                newSchemaIS.currentStatement().filterEffectiveStatements(MustDefinition.class);
        final Collection<? extends MustDefinition> oldMust =
                oldSchemaIS.currentStatement().filterEffectiveStatements(MustDefinition.class);
        if (oldMust.size() < newMust.size()) {
            errors.add(addedMustError().updateInformation(
                    newSchemaIS.toSchemaNodeIdentifier() + MUST + getXpathStringFromMustCollection(newMust),
                    oldSchemaIS.toSchemaNodeIdentifier() + MUST + getXpathStringFromMustCollection(oldMust)));
        } else {
            for (final MustDefinition newMustDefinition : newMust) {
                if (!oldMust.contains(newMustDefinition)) {
                    errors.add(checkMustWarning().updateInformation(
                            newSchemaIS.toSchemaNodeIdentifier() + MUST + newMustDefinition.getXpath().toString(),
                            oldSchemaIS.toSchemaNodeIdentifier() + MUST + getXpathStringFromMustCollection(oldMust)));
                }
            }
        }
    }

    private static String getXpathStringFromMustCollection(final Collection<? extends MustDefinition> must) {
        return "[" + must.stream()
                .map(t -> t.getXpath().toString())
                .collect(Collectors.joining(",")) + "]";
    }

    private void checkWhen() {
        final Optional<? extends QualifiedBound> newWhen = newSchemaIS.currentStatement()
                .findFirstEffectiveSubstatementArgument(WhenEffectiveStatement.class);
        final Optional<? extends QualifiedBound> oldWhen = oldSchemaIS.currentStatement()
                .findFirstEffectiveSubstatementArgument(WhenEffectiveStatement.class);
        if (oldWhen.isEmpty() && newWhen.isPresent()) {
            errors.add(addedWhenError().updateInformation(
                    newSchemaIS.toSchemaNodeIdentifier() + WHEN + newWhen.get(),
                    oldSchemaIS.toSchemaNodeIdentifier() + WHEN + DONT_EXISTS));
        } else if (oldWhen.isPresent() && newWhen.isPresent()
                && !oldWhen.get().toString().equals(newWhen.get().toString())) {
            errors.add(checkWhenWarning().updateInformation(
                    newSchemaIS.toSchemaNodeIdentifier() + WHEN + newWhen.get(),
                    oldSchemaIS.toSchemaNodeIdentifier() + WHEN + oldWhen.get()));
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
            final String oldPath = buildTypeDefinitionPath(oldNode, oldSchemaIS);
            errors.add(unitsError().updateInformation(DONT_EXISTS,
                    oldPath + "\nunits: " + oldUnit.get()));
        }
    }

    private void checkDefault(final TypeDefinition<?> oldNode, final TypeDefinition<?> newNode) {
        final Optional<?> oldDefault = oldNode.getDefaultValue();
        final Optional<?> newDefault = newNode.getDefaultValue();
        if (oldDefault.isPresent() && (newDefault.isEmpty() || !oldDefault.get().equals(newDefault.get()))) {
            final String oldPath = buildTypeDefinitionPath(oldNode, oldSchemaIS);
            errors.add(defaultError().updateInformation(DONT_EXISTS,
                    oldPath + "\ndefault: " + oldDefault.get()));
        }
    }

    private void checkRange(final TypeDefinition<?> oldNode, final TypeDefinition<?> newNode) {
        final var oldRange = ((RangeRestrictedTypeDefinition<?, ?>) oldNode).getRangeConstraint();
        final var newRange = ((RangeRestrictedTypeDefinition<?, ?>) newNode).getRangeConstraint();
        if (oldRange.isPresent()) {
            final String oldPath = buildTypeDefinitionPath(oldNode, oldSchemaIS);
            final String newPath = buildTypeDefinitionPath(newNode, newSchemaIS);
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
        final String oldPath = buildTypeDefinitionPath(oldNode, oldSchemaIS);
        final String newPath = buildTypeDefinitionPath(newNode, newSchemaIS);

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

    private static boolean isPatternValuesSame(final PatternConstraint newPattern, final PatternConstraint oldPattern) {
        return newPattern.getErrorMessage().equals(oldPattern.getErrorMessage())
                && newPattern.getJavaPatternString().equals(oldPattern.getJavaPatternString())
                && newPattern.getRegularExpressionString().equals(oldPattern.getRegularExpressionString())
                && newPattern.getErrorAppTag().equals(oldPattern.getErrorAppTag())
                && newPattern.getDescription().equals(oldPattern.getDescription())
                && newPattern.getReference().equals(oldPattern.getReference());
    }

    private static String patterConstraintListToString(final List<PatternConstraint> patterns) {
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

    private EffectiveStatement<?, ?> getNodeFromNewModule(final Absolute nodeAbsolutePath) {
        final List<QName> finalList = new LinkedList<>();
        for (final QName qname : nodeAbsolutePath.getNodeIdentifiers()) {
            if (qname.getModule().equals(oldModule.getQNameModule())) {
                finalList.add(QName.create(newModule.getNamespace(), newModule.getRevision(), qname.getLocalName()));
            } else {
                finalList.add(qname);
            }
        }

        final SchemaInferenceStack lookupStack = SchemaInferenceStack.of(newContext);
        try {
            EffectiveStatement<?, ?> result = null;
            for (final QName qname : finalList) {
                result = lookupStack.enterDataTree(qname);
            }
            return result;
        } catch (final IllegalArgumentException e) {
            errors.add(missingNodeError().updateInformation("missing node",
                    oldSchemaIS.toSchemaNodeIdentifier().toString()));
            return null;
        }
    }

    private void checkRevision() {
        final Optional<Revision> newOptionalRevision = newModule.getRevision();
        final Optional<Revision> oldOptionalRevision = oldModule.getRevision();
        if (newOptionalRevision.isEmpty()) {
            errors.add(missingRevision());
        } else {
            final Revision revision = newOptionalRevision.get();
            if (oldOptionalRevision.isPresent() && revision.compareTo(oldOptionalRevision.get()) < 1) {
                errors.add(revisionError().updateInformation(revision.toString(),
                        oldOptionalRevision.get().toString()));
            }

            final Collection<? extends RevisionStatement> revisionsNew = revisionStatements(newModule);
            final Collection<? extends RevisionStatement> revisionsOld = revisionStatements(oldModule);

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

    private static Collection<? extends RevisionStatement> revisionStatements(final Module module) {
        if (module instanceof EffectiveStatementEquivalent<?> equivalent
                && equivalent.asEffectiveStatement() instanceof ModuleEffectiveStatement moduleStatement) {
            return Objects.requireNonNull(moduleStatement.declared()).revisionStatements();
        }
        return List.of();
    }

    private void checkName() {
        if (!newModule.getName().equals(oldModule.getName())) {
            errors.add(nameError().updateInformation(newModule.getName(), oldModule.getName()));
        }
    }

    private void checkNamespace() {
        if (!newModule.getNamespace().equals(oldModule.getNamespace())) {
            errors.add(namespaceError().updateInformation(newModule.getNamespace().toString(),
                    oldModule.getNamespace().toString()));
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

    private static String buildTypeDefinitionPath(final TypeDefinition<?> typeDefinition,
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
