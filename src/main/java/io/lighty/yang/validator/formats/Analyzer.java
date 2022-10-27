/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.yang.validator.GroupArguments;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.opendaylight.yangtools.yang.model.api.ModuleLike;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Analyzer extends FormatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(Analyzer.class);
    private static final String HELP_NAME = "analyze";
    private static final String HELP_DESCRIPTION = "return count of each keyword";
    private final Map<String, Integer> counter = new HashMap<>();

    @Override
    void emitFormat() {
        final Set<DeclaredStatement<?>> statements = getRecursivelyDeclaredStatements(schemaContext.getModules());
        for (final DeclaredStatement<?> declaredStatement : statements) {
            analyzeSubstatement(declaredStatement);
        }
        printOut();
    }

    private Set<DeclaredStatement<?>> getRecursivelyDeclaredStatements(final Collection<? extends ModuleLike> modules) {
        final Set<DeclaredStatement<?>> declaredStatements = new HashSet<>();
        for (final ModuleLike module : modules) {
            declaredStatements.add(((EffectiveStatement<?, ?>) module).getDeclared());

            final Collection<? extends ModuleLike> submodules = module.getSubmodules();
            if (submodulesAreNotEmpty(submodules)) {
                declaredStatements.addAll(getRecursivelyDeclaredStatements(submodules));
            }
        }
        return declaredStatements;
    }

    private static boolean submodulesAreNotEmpty(final Collection<? extends ModuleLike> submodules) {
        return submodules != null && !submodules.isEmpty();
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
                        justification = "Valid output from LYV is dependent on Logback output")
    private void printOut() {
        for (final Map.Entry<String, Integer> entry : new TreeMap<>(counter).entrySet()) {
            LOG.info("{}: {}", entry.getKey(), entry.getValue());
        }
    }

    private void analyzeSubstatement(final DeclaredStatement<?> subStatement) {
        final String name = subStatement.statementDefinition().getStatementName().getLocalName();
        counter.compute(name, (key, val) -> val == null ? 1 : val + 1);
        final Collection<? extends DeclaredStatement<?>> substatements = subStatement.declaredSubstatements();
        for (final DeclaredStatement<?> nextSubstatement : substatements) {
            analyzeSubstatement(nextSubstatement);
        }
    }

    @Override
    Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        // TODO make option to iterate through multiple modules and give output only from those modules not overlaping
        // TODO make option print as html table
        // TODO make option to sort output alphabetically or by number of occurrences
        // TODO make option to ignore some specific keywords
        // TODO make option to search only for some specific keywords
        return Optional.empty();
    }
}
