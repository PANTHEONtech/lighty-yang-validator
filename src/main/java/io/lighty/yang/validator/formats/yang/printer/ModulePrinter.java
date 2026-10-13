/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats.yang.printer;

import io.lighty.yang.validator.exceptions.NotFoundException;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementEquivalent;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.MandatoryAware;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.meta.DataSchemaCompat;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AugmentEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ChoiceEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ContainerEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DescriptionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DescriptionStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.InputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.LeafEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.LeafListEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ListEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ModuleEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.OutputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ReferenceEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ReferenceStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.RevisionStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.api.stmt.WhenEffectiveStatement;
import org.slf4j.Logger;

public class ModulePrinter {

    private static final String REFERENCE_STRING = "reference";
    private static final String DESCRIPTION_STRING = "description";

    private final Set<TypeDefinition<?>> usedTypes;
    private final Set<String> usedImports;
    private final StatementPrinter printer;
    private final Set<SchemaTree> schemaTree;
    private final QNameModule moduleName;
    private final SmartTypePrintingStrategy typePrinter;
    private final Module module;
    private final Map<QNameModule, String> moduleToPrefix;
    private final Set<String> usedGroupingNames = new HashSet<>();

    private final HashMap<GroupingDefinition, Set<SchemaTree>> groupingTreesMap = new HashMap<>();

    public ModulePrinter(final Set<SchemaTree> schemaTree, final EffectiveModelContext context,
            final QNameModule moduleName, final OutputStream out, final Set<TypeDefinition<?>> usedTypes,
            final Set<String> usedImports) {
        this(schemaTree, context, moduleName,
                new IndentingPrinter(new PrintStream(out, false, Charset.defaultCharset())),
                usedTypes, usedImports);
    }

    public ModulePrinter(final Set<SchemaTree> schemaTree, final EffectiveModelContext context,
            final QNameModule moduleName, final Logger out, final Set<TypeDefinition<?>> usedTypes,
            final Set<String> usedImports) {
        this(schemaTree, context, moduleName, new IndentingLogger(out), usedTypes, usedImports);
    }

    private ModulePrinter(final Set<SchemaTree> schemaTree, final EffectiveModelContext context,
            final QNameModule moduleName, final Indenting printer, final Set<TypeDefinition<?>> usedTypes,
            final Set<String> usedImports) {
        this.usedImports = usedImports;
        this.usedTypes = usedTypes;
        this.schemaTree = schemaTree;
        this.moduleName = moduleName;
        this.printer = new StatementPrinter(printer);
        module = context.findModule(moduleName)
                .orElseThrow(() -> new NotFoundException("Module ", moduleName.toString()));
        moduleToPrefix = module.getImports().stream()
                .collect(Collectors.toMap(i -> context
                                .findModules(i.getModuleName().getLocalName()).iterator().next().getQNameModule(),
                        ModuleImport::getPrefix));
        typePrinter = new SmartTypePrintingStrategy(module, moduleToPrefix);
    }

    public void printYang() {
        printHeader();
        typePrinter.printTypedefs(printer, usedTypes);
        printAugmentations();
        for (final SchemaTree st : schemaTree) {
            if (!st.isAugmenting()) {
                printSchema(st);
            }
        }
        printGroupings(groupingTreesMap);
        printer.closeStatement();
    }

    private void printAugmentations() {
        for (final AugmentEffectiveStatement augmentation
                : module.asEffectiveStatement().collectEffectiveSubstatements(AugmentEffectiveStatement.class)) {
            boolean printOpeningStatement = true;
            for (final SchemaTree st : schemaTree) {
                if (isStAugmentOrStParentEqualsToAugmPath(st, augmentation)) {
                    printOpeningStatement = isPrintOpeningStatement(augmentation, printOpeningStatement);
                    doPrintSchema(true, st, null, groupingTreesMap);
                }
            }
            if (!printOpeningStatement) {
                printer.closeStatement();
            }
        }
    }

    private boolean isPrintOpeningStatement(final AugmentEffectiveStatement augmentation,
            boolean printOpeningStatement) {
        if (printOpeningStatement) {
            final StringBuilder target = new StringBuilder();
            for (final QName name : augmentation.argument().getNodeIdentifiers()) {
                target.append('/');
                target.append(moduleToPrefix.get(name.getModule()));
                target.append(':');
                target.append(name.getLocalName());
            }
            printer.openStatement(Statement.AUGMENT, target.toString());
            printOpeningStatement = false;
        }
        return printOpeningStatement;
    }

    private void printSchema(final SchemaTree tree) {
        if (module.findDataChildByName(tree.getSchemaNode().getQName()).isPresent()) {
            doPrintSchema(true, tree, null, groupingTreesMap);
        }
    }

    private void printGroupings(final HashMap<GroupingDefinition, Set<SchemaTree>> trees) {
        for (final Map.Entry<GroupingDefinition, Set<SchemaTree>> tree : trees.entrySet()) {
            printer.openStatement(Statement.GROUPING, tree.getKey().getQName().getLocalName());
            for (final SchemaTree entry : tree.getValue()) {
                doPrintSchema(true, entry, tree.getKey().getQName().getLocalName(), new HashMap<>());
            }
            printer.closeStatement();
        }
    }

    private boolean doPrintUses(final EffectiveStatement<?, ?> statement, final boolean isPrintingAllowed,
            final String groupingName,
            final SchemaTree tree, final HashMap<GroupingDefinition, Set<SchemaTree>> groupingTrees) {
        //left untouched because AddedByUsesAware is deprecated
        if (toDataSchemaNode(statement).isAddedByUses()) {
            return printingUses(statement, isPrintingAllowed, groupingName, tree, groupingTrees);
        }
        return isPrintingAllowed;
    }

    private boolean printingUses(final EffectiveStatement<?, ?> statement, final boolean isPrintingAllowed,
            final String groupingName, final SchemaTree tree,
            final HashMap<GroupingDefinition, Set<SchemaTree>> groupingTrees) {
        final QName qname = (QName) statement.argument();
        final List<GroupingDefinition> groupingDefinitions = module.getGroupings().stream()
                .filter(g -> g.findDataChildByName(qname).isPresent())
                .collect(Collectors.toList());
        final Optional<GroupingDefinition> match = findMatchInGroupingDefinitions(groupingDefinitions, statement);
        if (match.isEmpty()) {
            return isPrintingAllowed;
        }

        final GroupingDefinition groupingDefinition = match.get();
        if (groupingDefinition.getQName().getLocalName().equals(groupingName)) {
            return isPrintingAllowed;
        }
        final String uses = groupingDefinition.getQName().getLocalName();
        if (!groupingTrees.containsKey(groupingDefinition)) {
            groupingTrees.put(groupingDefinition, new HashSet<>());
        }

        final Set<SchemaTree> schemaTrees = groupingTrees.get(groupingDefinition);
        if (isChoice(tree.getSchemaNode())) {
            resolveChoiceSchemaNode(schemaTrees, tree);
        } else {
            boolean containsKey = false;
            for (final SchemaTree st : schemaTrees) {
                if (st.getSchemaNode().getQName().equals(tree.getSchemaNode().getQName())) {
                    containsKey = true;
                    break;
                }
            }
            if (!containsKey) {
                schemaTrees.add(tree);
            }
        }
        if (!usedGroupingNames.contains(uses) && isPrintingAllowed) {
            printer.printSimple("uses", uses);
            usedGroupingNames.add(uses);
        }
        return false;
    }

    // left as is because module.getGroupings()/GroupingDefinition.findDataChildByName() have no EffectiveStatement
    // equivalent
    private static Optional<GroupingDefinition> findMatchInGroupingDefinitions(
            final List<GroupingDefinition> groupingDefinitions, final EffectiveStatement<?, ?> statement) {
        final QName qname = (QName) statement.argument();
        for (final GroupingDefinition grouping : groupingDefinitions) {
            final Optional<DataSchemaNode> dataChildByName = grouping.findDataChildByName(qname);
            if (dataChildByName.isEmpty()) {
                continue;
            }
            final DataSchemaNode dataSchemaNode = dataChildByName.get();

            if (!(dataSchemaNode instanceof EffectiveStatementEquivalent<?> dataEquivalent)) {
                continue;
            }
            final Collection<? extends EffectiveStatement<?, ?>> effectiveStatements
                    = dataEquivalent.asEffectiveStatement().effectiveSubstatements();
            if (statement.effectiveSubstatements().size() != effectiveStatements.size()) {
                continue;
            }

            final Optional<GroupingDefinition> match =
                    getGroupingDefinitionMatch(statement, effectiveStatements, grouping);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private static boolean isChoice(final DataSchemaNode node) {
        return node instanceof EffectiveStatementEquivalent<?> equivalent
                && equivalent.asEffectiveStatement() instanceof ChoiceEffectiveStatement;
    }

    private static void resolveChoiceSchemaNode(final Set<SchemaTree> schemaTrees, final SchemaTree tree) {
        boolean extendedTree = false;
        for (final SchemaTree st : schemaTrees) {
            if (isChoice(st.getSchemaNode()) && st.getQname().equals(tree.getQname())) {
                extendedTree = true;
                for (final SchemaTree entry : tree.getChildren()) {
                    if (!st.getChildren().contains(entry)) {
                        st.addChild(entry);
                    }
                }
            }
        }
        if (!extendedTree) {
            schemaTrees.add(tree);
        }
    }

    private static Optional<GroupingDefinition> getGroupingDefinitionMatch(final EffectiveStatement<?, ?> statement,
            final Collection<? extends EffectiveStatement<?, ?>> collection, final GroupingDefinition grouping) {
        boolean allSubstatementFound = true;
        for (final EffectiveStatement<?, ?> compare : statement.effectiveSubstatements()) {
            if (isSubstatementNotFound(collection, compare)) {
                allSubstatementFound = false;
                break;
            }
        }
        if (allSubstatementFound) {
            return Optional.of(grouping);
        }
        return Optional.empty();
    }

    private static boolean isSubstatementNotFound(final Collection<? extends EffectiveStatement<?, ?>> collection,
            final EffectiveStatement<?, ?> compare) {
        final DeclaredStatement<?> compareDeclared = compare.getDeclared();
        for (final EffectiveStatement<?, ?> substatement : collection) {
            final DeclaredStatement<?> substatementDeclared = substatement.getDeclared();
            if (compareDeclared == null) {
                if (compare.equals(substatement)) {
                    return false;
                }
            } else if (compareDeclared.equals(substatementDeclared)) {
                return false;
            }
        }
        return true;
    }

    private void doPrintSchema(boolean isPrintingAllowed, final SchemaTree tree, final String groupingName,
            final HashMap<GroupingDefinition, Set<SchemaTree>> groupingTrees) {
        final DataSchemaNode schemaNode = tree.getSchemaNode();
        if (moduleName.equals(schemaNode.getQName().getModule())) {
            final EffectiveStatement<?, ?> statement = schemaNode instanceof EffectiveStatementEquivalent<?> equivalent
                    ? equivalent.asEffectiveStatement() : null;
            isPrintingAllowed = doPrintUses(statement, isPrintingAllowed, groupingName, tree, groupingTrees);
            doPrintSchema(isPrintingAllowed, tree, groupingName, groupingTrees, statement);
        }
    }

    private void doPrintSchema(final boolean isPrintingAllowed, final SchemaTree tree, final String groupingName,
            final HashMap<GroupingDefinition, Set<SchemaTree>> groupingTrees,
            final EffectiveStatement<?, ?> statement) {
        if (isPrintingAllowed) {
            final QName qname = (QName) statement.argument();
            if (statement instanceof ContainerEffectiveStatement || statement instanceof InputEffectiveStatement
                    || statement instanceof OutputEffectiveStatement) {
                printer.openStatement(Statement.CONTAINER, qname.getLocalName());
                printer.printConfig(toDataSchemaNode(statement).effectiveConfig().orElse(Boolean.TRUE));
            } else if (statement instanceof ListEffectiveStatement listStatement) {
                printer.openStatement(Statement.LIST, qname.getLocalName());
                final StringJoiner keyJoiner = new StringJoiner(" ", "key \"", "\"");
                listStatement.findKeyStatement().map(key -> key.argument().asList()).orElse(List.of()).stream()
                        .map(QName::getLocalName)
                        .forEach(keyJoiner::add);
                printer.printSimple("", keyJoiner.toString());
            } else if (statement instanceof LeafEffectiveStatement) {
                printer.openStatement(Statement.LEAF, qname.getLocalName());
                //needs to be TypedDataSchemaNode because printType() calls typed.typeDefinition(), which has no
                // EffectiveStatement substitute
                typePrinter.printType(printer, (TypedDataSchemaNode) toDataSchemaNode(statement));
            } else if (statement instanceof ChoiceEffectiveStatement) {
                printer.openStatement(Statement.CHOICE, qname.getLocalName());
            } else if (statement instanceof CaseEffectiveStatement) {
                printer.openStatement(Statement.CASE, qname.getLocalName());
            } else if (statement instanceof LeafListEffectiveStatement) {
                printer.openStatement(Statement.LEAF_LIST, qname.getLocalName());
                //same as above
                typePrinter.printType(printer, (TypedDataSchemaNode) toDataSchemaNode(statement));
            } else {
                throw new IllegalStateException("Unknown node " + statement);
            }
            doPrintDescription(statement);
            doPrintMandatory(statement);
            doPrintReference(statement);
            doPrintWhen(statement);
        }
        for (final SchemaTree child : tree.getChildren()) {
            doPrintSchema(isPrintingAllowed, child, groupingName, groupingTrees);
        }
        if (isPrintingAllowed) {
            printer.closeStatement();
        }
    }

    private void doPrintWhen(final EffectiveStatement<?, ?> statement) {
        statement.findFirstEffectiveSubstatement(WhenEffectiveStatement.class).ifPresent(when -> {
            printer.printSimple("when", "\"" + when.argument() + "\"");
            printer.printEmptyLine();
        });
    }

    private void doPrintReference(final EffectiveStatement<?, ?> statement) {
        statement.findFirstEffectiveSubstatement(ReferenceEffectiveStatement.class).ifPresent(reference -> {
            printer.printSimple(REFERENCE_STRING, "\"" + reference.argument() + "\"");
            printer.printEmptyLine();
        });
    }

    private void doPrintDescription(final EffectiveStatement<?, ?> statement) {
        statement.findFirstEffectiveSubstatement(DescriptionEffectiveStatement.class).ifPresent(description -> {
            printer.printSimple(DESCRIPTION_STRING, "\"" + description.argument() + "\"");
            printer.printEmptyLine();
        });
    }

    private void doPrintMandatory(final EffectiveStatement<?, ?> statement) {
        if (toDataSchemaNode(statement) instanceof MandatoryAware mandatoryAware && mandatoryAware.isMandatory()) {
            printer.printSimple("mandatory", "true");
            printer.printEmptyLine();
        }
    }
    private static DataSchemaNode toDataSchemaNode(final EffectiveStatement<?, ?> statement) {
        if (statement instanceof DataSchemaCompat<?, ?> compat) {
            return compat.toDataSchemaNode();
        }
        throw new IllegalStateException("Cannot bridge " + statement + " to DataSchemaNode");
    }

    private void printHeader() {
        printer.openStatement(Statement.MODULE, module.getName());
        printer.printSimple("yang-version", module.getYangVersion().toString());
        printer.printEmptyLine();
        printer.printSimple("namespace", "\"" + module.getNamespace() + "\"");
        printer.printEmptyLine();
        printer.printSimple("prefix", module.getPrefix());
        printer.printEmptyLine();
        printImports();
        final Optional<String> organization = module.getOrganization();
        if (organization.isPresent()) {
            printer.printSimple("organization", "\"" + organization.get() + "\"");
            printer.printEmptyLine();
        }
        final Optional<String> contact = module.getContact();
        if (contact.isPresent()) {
            printer.printSimple("contact", "\"" + contact.get() + "\"");
            printer.printEmptyLine();
        }
        final Optional<String> description = module.getDescription();
        if (description.isPresent()) {
            printer.printSimple(DESCRIPTION_STRING, "\"" + description.get() + "\"");
            printer.printEmptyLine();
        }
        final Optional<String> reference = module.getReference();
        if (reference.isPresent()) {
            printer.printSimple(REFERENCE_STRING, "\"" + reference.get() + "\"");
            printer.printEmptyLine();
        }
        final Optional<Revision> revision = module.getRevision();
        if (revision.isEmpty()) {
            return;
        }

        if (module instanceof EffectiveStatementEquivalent<?> equivalent
                && equivalent.asEffectiveStatement() instanceof ModuleEffectiveStatement moduleStatement) {
            final Collection<? extends RevisionStatement> revisions =
                    Objects.requireNonNull(moduleStatement.declared()).revisionStatements();
            printEachRevision(revisions);
        } else {
            doPrintSimpleRevision(revision.get());
        }
    }

    private void printEachRevision(final Collection<? extends RevisionStatement> revisions) {
        for (final RevisionStatement rev : revisions) {
            final DescriptionStatement desc = rev.descriptionStatement();
            final ReferenceStatement ref = rev.referenceStatement();

            if (desc != null || ref != null) {
                printer.openStatement(Statement.REVISION, rev.argument().toString());
                if (ref != null) {
                    printer.printSimpleSeparately(REFERENCE_STRING, "\"" + ref.argument() + "\"");
                }
                if (desc != null) {
                    printer.printSimpleSeparately(DESCRIPTION_STRING, "\"" + desc.argument() + "\"");
                }
                printer.closeStatement();
                printer.printEmptyLine();
            } else {
                doPrintSimpleRevision(rev.argument());
            }
        }
    }

    private void doPrintSimpleRevision(final Revision revision) {
        printer.printSimple("revision", revision.toString());
        printer.printEmptyLine();
    }

    private void printImports() {
        for (final ModuleImport anImport : module.getImports()) {
            if (usedImports.contains(anImport.getModuleName().getLocalName())) {
                printer.openStatement(Statement.IMPORT, anImport.getModuleName().getLocalName());
                printer.printSimple("prefix", anImport.getPrefix());
                printer.closeStatement();
                printer.printEmptyLine();
            }
        }
    }

    private static boolean isStAugmentOrStParentEqualsToAugmPath(final SchemaTree st,
            final AugmentEffectiveStatement augmSN) {
        if (st.isAugmenting()) {
            final List<QName> qnamePath = st.getAbsolutePath().getNodeIdentifiers();
            if (qnamePath.size() > 1) {
                final Absolute parentPath = Absolute.of(qnamePath.subList(0, qnamePath.size() - 1));
                return parentPath.equals(augmSN.argument());
            }
        }
        return false;
    }

}

