/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.UnresolvedQName.Unqualified;
import org.opendaylight.yangtools.yang.common.YangConstants;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;

final class YangContextFactory {

    private static final Pattern MODULE_PATTERN = Pattern.compile("module(.*?)\\{");
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");
    private static final YangParserFactory PARSER_FACTORY = new DefaultYangParserFactory();

    private final List<File> testFiles = new ArrayList<>();
    private final List<File> libFiles = new ArrayList<>();
    private final Set<QName> supportedFeatures;
    private final List<SourceIdentifier> sourceIdentifiers = new ArrayList<>();

    YangContextFactory(final List<String> yangLibDirs, final List<String> yangTestFiles,
            final Set<QName> supportedFeatures, final boolean recursiveSearch) throws IOException {
        this.supportedFeatures = supportedFeatures;

        final Set<String> yangLibDirsSet = new HashSet<>();
        for (final String yangTestFile : yangTestFiles) {
            final File file;
            if (!yangTestFile.endsWith(YangConstants.RFC6020_YANG_FILE_EXTENSION)) {
                file = findInFiles(libFiles, yangTestFile);
                testFiles.add(file);
            } else {
                file = new File(yangTestFile);
                testFiles.add(file);
            }
            yangLibDirsSet.add(file.getParent());
        }
        yangLibDirsSet.addAll(yangLibDirs);
        for (final String yangLibDir : yangLibDirsSet) {
            libFiles.addAll(getYangFiles(yangLibDir, recursiveSearch));
        }
    }

    static final FileFilter YANG_FILE_FILTER = file -> {
        final String name = file.getName().toLowerCase(Locale.ENGLISH);
        return name.endsWith(YangConstants.RFC6020_YANG_FILE_EXTENSION) && file.isFile();
    };

    @SuppressWarnings("UnstableApiUsage")
    EffectiveModelContext createContext(final boolean useAllFiles) throws IOException, YangParserException {
        final YangParser parser = PARSER_FACTORY.createParser();
        if (supportedFeatures != null && !supportedFeatures.isEmpty()) {
            parser.setSupportedFeatures(supportedFeatures);
        }

        final List<String> names = new ArrayList<>();
        for (final File file : testFiles) {
            final YangTextSchemaSource yangTextSchemaSource = YangTextSchemaSource.forPath(file.toPath());
            names.add(yangTextSchemaSource.getIdentifier().name().getLocalName());
            parser.addSource(yangTextSchemaSource);
        }
        for (final File file : libFiles) {
            if (useAllFiles) {
                final YangTextSchemaSource yangTextSchemaSource = YangTextSchemaSource.forPath(file.toPath());
                final String name = yangTextSchemaSource.getIdentifier().name().getLocalName();

                if (!names.contains(name)) {
                    parser.addSource(yangTextSchemaSource);
                }
            } else {
                parser.addLibSource(YangTextSchemaSource.forPath(file.toPath()));
            }
        }

        final EffectiveModelContext effectiveModelContext = parser.buildEffectiveModel();
        for (final Module next : effectiveModelContext.getModules()) {
            for (final String name : names) {
                if (next.getName().equals(name)) {
                    sourceIdentifiers.add(new SourceIdentifier(Unqualified.of(name), next.getRevision().orElse(null)));
                }
            }
        }
        return effectiveModelContext;
    }

    List<SourceIdentifier> getTestFilesSourceIdentifiers() {
        return sourceIdentifiers;
    }

    private static File findInFiles(final List<File> libFiles, final String yangTestFile) throws IOException {
        for (final File file : libFiles) {
            if (WHITESPACES.matcher(getModelNameFromFile(file)).replaceAll("").equals(yangTestFile)) {
                return file;
            }
        }
        throw new FileNotFoundException("Model with specific module-name does not exist : " + yangTestFile);
    }

    private static String getModelNameFromFile(final File file) throws IOException {
        final String fileAsString = readFile(file.getAbsolutePath());
        final Matcher matcher = MODULE_PATTERN.matcher(fileAsString);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String readFile(final String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static Collection<File> getYangFiles(final String yangSourcesDirectoryPath, final boolean recursiveSearch) {
        final File testSourcesDir = new File(yangSourcesDirectoryPath);

        if (recursiveSearch) {
            return iterateYangFilesRecursively(testSourcesDir);
        } else {
            final File[] files = testSourcesDir.listFiles(YANG_FILE_FILTER);
            if (files == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(files);
        }
    }

    private static List<File> iterateYangFilesRecursively(final File dir) {
        final List<File> yangFiles = new ArrayList<>();
        final File[] files = dir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                yangFiles.addAll(iterateYangFilesRecursively(file));
            } else if (file.isFile()
                    && file.getName().toLowerCase(Locale.ENGLISH).endsWith(YangConstants.RFC6020_YANG_FILE_EXTENSION)) {
                yangFiles.add(file);
            }
        }

        return yangFiles;
    }
}
