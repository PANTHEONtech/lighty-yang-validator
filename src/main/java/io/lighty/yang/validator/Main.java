/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator;

import static ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import com.google.common.base.Stopwatch;
import io.lighty.yang.validator.checkupdatefrom.CheckUpdateFrom;
import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.config.ConfigurationBuilder;
import io.lighty.yang.validator.exceptions.LyvApplicationException;
import io.lighty.yang.validator.formats.Analyzer;
import io.lighty.yang.validator.formats.Depends;
import io.lighty.yang.validator.formats.Emitter;
import io.lighty.yang.validator.formats.Format;
import io.lighty.yang.validator.formats.FormatPlugin;
import io.lighty.yang.validator.formats.JsTree;
import io.lighty.yang.validator.formats.JsonTree;
import io.lighty.yang.validator.formats.MultiModulePrinter;
import io.lighty.yang.validator.formats.NameRevision;
import io.lighty.yang.validator.formats.Tree;
import io.lighty.yang.validator.simplify.SchemaSelector;
import io.lighty.yang.validator.simplify.SchemaTree;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of Yang parser system test.
 *
 * <p>
 * yang-model-validator [-f features] [-h help] [-p path] yangFiles...
 * -f,--features &lt;arg&gt;   features is a string in the form
 * [feature(,feature)*] and feature is a string in the form
 * [($namespace?revision=$revision)$local_name].
 * This option is used to prune the data model by removing
 * all nodes that are defined with a "if-feature".
 * -h,--help            print help message and exit.
 * -p,--path &lt;arg&gt;       path is a colon (:) separated list of directories
 * to search for yang modules.
 * -r, --recursive      recursive search of directories specified by -p option
 * -o, --output         path to output file for logs. Output file will be overwritten.
 * -m, --module-name    validate yang by module name.
 * -f, --format         output format of the yang. Supported formats: tree, name-revision.
 * -v, --version        output release version and contact.
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final ch.qos.logback.classic.Logger MAIN_LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);

    private Main() {
        // Hidden on purpose
    }

    public static void main(final String[] args) {
        final Format format = getFormat();
        final Configuration configuration;
        try {
            configuration = getConfiguration(format, args);
        } catch (final IllegalArgumentException e) {
            LOG.error("Exception while setting configurationBuilder", e);
            return;
        }
        setMainLoggerOutput(configuration);
        final List<String> yangFiles = new ArrayList<>();
        final List<String> moduleNameValues = configuration.getModuleNames();

        final List<String> parseAllDir = configuration.getParseAll();
        if (parseAllDir.isEmpty()) {
            if (moduleNameValues != null) {
                yangFiles.addAll(moduleNameValues);
            }
            yangFiles.addAll(configuration.getYang());
            try {
                runLYV(yangFiles, configuration, format);
            } catch (final LyvApplicationException e) {
                LOG.error("Exception in LYV application: {}", formatLyvExceptionMessage(e));
                return;
            }
        } else {
            for (final String dir : parseAllDir) {
                try (Stream<Path> path = Files.list(Paths.get(dir))) {
                    final List<String> collect = path
                            .map(Path::toString)
                            .collect(Collectors.toList());
                    yangFiles.addAll(collect);
                } catch (final IOException e) {
                    LOG.error("Could not Collect files from provided ({}) directory",
                            String.join(",", parseAllDir), e);
                    return;
                }
            }

            final String yangtoolsVersion;
            try {
                yangtoolsVersion = getYangtoolsVersion(EffectiveModelContext.class);
            } catch (final LyvApplicationException e) {
                LOG.error("Exception in LYV application", e);
                return;
            }
            final CompilationTable table =
                    new CompilationTable(configuration.getOutput(), parseAllDir, yangtoolsVersion);
            final CompilationTableAppender newAppender = new CompilationTableAppender();
            newAppender.setContext(MAIN_LOGGER.getLoggerContext());
            newAppender.start();
            newAppender.setCompilationTable(table);

            MAIN_LOGGER.addAppender(newAppender);
            runLywForeachYangFile(yangFiles, configuration, newAppender, table, format);
        }
        MAIN_LOGGER.getLoggerContext().reset();
    }

    private static Configuration getConfiguration(final Format format, final String[] args) {
        final LyvParameters lyvParameters = new LyvParameters(format, args);
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder = configurationBuilder.from(lyvParameters);
        Configuration configuration = configurationBuilder.build();

        if (configuration.getTreeConfiguration().isHelp()) {
            configurationBuilder.setFormat("tree");
            configuration = configurationBuilder.build();
        }
        return configuration;
    }

    private static Format getFormat() {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new Depends());
        formats.add(new NameRevision());
        formats.add(new JsonTree());
        formats.add(new Tree());
        formats.add(new MultiModulePrinter());
        formats.add(new JsTree());
        formats.add(new Analyzer());
        return new Format(formats);
    }

    private static void runLywForeachYangFile(final List<String> yangFiles, final Configuration configuration,
            final CompilationTableAppender newAppender, final CompilationTable table,
            final Format formatter) {
        for (final String yangFile : yangFiles) {
            final String name = yangFile.split("/")[yangFile.split("/").length - 1];
            try {
                newAppender.setYangName(name);
                runLYV(Collections.singletonList(yangFile), configuration, formatter);
                table.addRow(name, null, CompilationStatus.PASSED);
            } catch (final LyvApplicationException e) {
                final String message = formatLyvExceptionMessage(e);
                table.addRow(name, message, CompilationStatus.FAILED);
                LOG.error("name : {}, message: {}", name, message);
            }
        }
        table.buildHtml();
    }

    private static Throwable getSourceException(final LyvApplicationException exception) {
        Throwable throwable = exception;
        while (throwable != null && !(throwable instanceof SourceException)) {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    private static String formatLyvExceptionMessage(final LyvApplicationException exception) {
        if (exception == null) {
            return "Received empty exception object";
        }
        Throwable throwable = getSourceException(exception);
        if (throwable == null) {
            throwable = exception;
        }

        final StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(throwable.getMessage()).append("\n");
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
            messageBuilder.append(throwable.getMessage()).append("\n");
        }
        return messageBuilder.toString();
    }

    private static String getYangtoolsVersion(final Class<?> clazz) throws LyvApplicationException {
        final String className = clazz.getSimpleName() + ".class";
        final String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            // Class not from JAR
            throw new LyvApplicationException("Class is not from jar file");
        }
        final String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
                + "/META-INF/MANIFEST.MF";
        final Manifest manifest;
        try {
            manifest = new Manifest(new URL(manifestPath).openStream());
        } catch (final IOException e) {
            throw new LyvApplicationException("Could not read manifest", e);
        }
        final Attributes attr = manifest.getMainAttributes();

        String version = attr.getValue("Bundle-Version");
        if (version == null) {
            final String propertiesPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
                    + "/META-INF/maven/org.opendaylight.yangtools/yang-model-api/pom.properties";
            final Properties properties = new Properties();
            try (InputStream is = new URL(propertiesPath).openStream()) {
                if (is != null) {
                    properties.load(is);
                    version = properties.getProperty("version", "");
                }
            } catch (final IOException e) {
                throw new LyvApplicationException("Could not read properties file", e);
            }
        }
        return version;
    }

    private static void setMainLoggerOutput(final Configuration config) {
        MAIN_LOGGER.getLoggerContext().reset();

        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        if (config.isDebug()) {
            encoder.setPattern("%d{HH:mm:ss.SSS} %-5level [%thread]: %5file:%-4line | %msg%n");
        } else {
            encoder.setPattern("%msg%n");
        }
        encoder.setContext(MAIN_LOGGER.getLoggerContext());
        encoder.start();

        // create FileAppender
        final String path = config.getOutput();
        if (path != null) {
            final FileAppender<ILoggingEvent> logFile = new FileAppender<>();
            logFile.setAppend(false);
            logFile.setFile(path + "/out.log");
            logFile.setContext(MAIN_LOGGER.getLoggerContext());
            logFile.setEncoder(encoder);
            logFile.start();
            MAIN_LOGGER.addAppender(logFile);
        } else {
            final ConsoleAppender<ILoggingEvent> logConsole = new ConsoleAppender<>();
            logConsole.setContext(MAIN_LOGGER.getLoggerContext());
            logConsole.setEncoder(encoder);
            logConsole.start();
            MAIN_LOGGER.addAppender(logConsole);
        }
        if (config.isDebug()) {
            MAIN_LOGGER.setLevel(Level.DEBUG);
        } else if (config.isQuiet()) {
            MAIN_LOGGER.detachAndStopAllAppenders();
        } else {
            MAIN_LOGGER.setLevel(Level.INFO);
        }
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    public static void runLYV(final List<String> yangFiles, final Configuration config,
            final Emitter format) throws LyvApplicationException {
        final List<String> yangLibDirs = initYangDirsPath(config.getPath());
        LOG.debug("Yang models dirs: {} ", yangLibDirs);
        LOG.debug("Yang models files: {} ", yangFiles);
        LOG.debug("Supported features: {} ", config.getSupportedFeatures());

        final boolean yangFileIsNotEmptyAndHelpIsNotSet
                = !(yangFiles.isEmpty() && config.getTreeConfiguration().isHelp());
        final Stopwatch stopWatch = Stopwatch.createStarted();
        final YangContextFactory contextFactory;
        try {
            contextFactory =
                    new YangContextFactory(yangLibDirs, yangFiles, config.getSupportedFeatures(), config.isRecursive());
        } catch (final IOException e) {
            throw new LyvApplicationException("Failed to create YangContextFactory", e);
        }

        EffectiveModelContext effectiveModelContext = null;
        if (yangFileIsNotEmptyAndHelpIsNotSet) {
            try {
                effectiveModelContext = contextFactory.createContext(config.getSimplify() != null);
            } catch (final IOException | YangParserException e) {
                throw new LyvApplicationException("Failed to create SchemaContext", e);
            }
        }
        SchemaTree schemaTree = null;
        if (config.getCheckUpdateFrom() == null) {
            if (yangFileIsNotEmptyAndHelpIsNotSet) {
                schemaTree = resolveSchemaTree(config.getSimplify(), effectiveModelContext);
            }
            if (config.getFormat() != null) {
                format.init(config, effectiveModelContext, contextFactory.getTestFilesSourceIdentifiers(),
                        schemaTree);
                format.emit();
            }
        } else {
            if (yangFiles.size() != 1) {
                throw new LyvApplicationException("Check-update-from option may be used with single module only");
            }
            final EffectiveModelContext contextFrom;
            try {
                final YangContextFactory contextFactoryFrom =
                        new YangContextFactory(initYangDirsPath(
                                config.getCheckUpdateFromConfiguration().getCheckUpdateFromPath()),
                                Collections.singletonList(config.getCheckUpdateFrom()), config.getSupportedFeatures(),
                                config.isRecursive());
                contextFrom = contextFactoryFrom.createContext(config.getSimplify() != null);
            } catch (final IOException | YangParserException e) {
                throw new LyvApplicationException("Failed to create SchemaContext", e);
            }
            final CheckUpdateFrom checkUpdateFrom = new CheckUpdateFrom(effectiveModelContext,
                    yangFiles.iterator().next(), contextFrom, config.getCheckUpdateFrom(),
                    config.getCheckUpdateFromConfiguration().getRfcVersion());
            checkUpdateFrom.validate();
            checkUpdateFrom.printErrors();
        }

        stopWatch.stop();
        LOG.debug("Elapsed time: {}", stopWatch);
    }

    private static SchemaTree resolveSchemaTree(final String simplifyDir,
            final EffectiveModelContext effectiveModelContext) throws LyvApplicationException {
        final SchemaSelector schemaSelector = new SchemaSelector(effectiveModelContext);
        if (simplifyDir == null) {
            schemaSelector.noXml();
        } else {
            try (Stream<Path> path = Files.list(Paths.get(simplifyDir))) {
                final List<File> xmlFiles = path
                        .map(Path::toFile)
                        .collect(Collectors.toList());

                addXmlFilesToSchemaSelector(schemaSelector, xmlFiles);
            } catch (final IOException e) {
                throw new LyvApplicationException("Failed to open xml files", e);
            }
        }
        return schemaSelector.getSchemaTree();
    }

    private static void addXmlFilesToSchemaSelector(final SchemaSelector schemaSelector, final List<File> xmlFiles)
            throws LyvApplicationException {
        for (final File xmlFile : xmlFiles) {
            try (FileInputStream fis = new FileInputStream(xmlFile)) {
                schemaSelector.addXml(fis);
            } catch (final IOException | XMLStreamException | URISyntaxException e) {
                throw new LyvApplicationException(
                        String.format("Failed to fill schema from %s", xmlFile), e);
            }
        }
    }

    private static List<String> initYangDirsPath(final List<String> paths) {
        final List<String> yangDirs = new ArrayList<>();
        if (paths != null) {
            for (final String pathArg : paths) {
                yangDirs.addAll(Arrays.asList(pathArg.split(":")));
            }
        }
        return yangDirs;
    }

    private static class CompilationTableAppender extends AppenderBase<ILoggingEvent> {

        private CompilationTable compilationTable = null;
        private String yangName;

        @Override
        protected void append(final ILoggingEvent loggingEvent) {
            if (loggingEvent.getLevel().equals(Level.WARN)) {
                String proxyMessage = "";
                if (loggingEvent.getThrowableProxy() != null) {
                    proxyMessage = "\n" + loggingEvent.getThrowableProxy().getMessage();
                }
                final String mainMessage = loggingEvent.getFormattedMessage();
                final String message = mainMessage + proxyMessage + "\n";
                compilationTable.addRow(yangName, message, CompilationStatus.PASSED_WITH_WARNINGS);
            }
        }

        void setCompilationTable(final CompilationTable table) {
            this.compilationTable = table;
        }

        void setYangName(final String name) {
            this.yangName = name;
        }

    }
}
