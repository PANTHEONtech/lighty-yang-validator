/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator;

import static io.lighty.yang.validator.Main.runLYV;
import static org.testng.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.config.ConfigurationBuilder;
import io.lighty.yang.validator.formats.Format;
import io.lighty.yang.validator.formats.FormatPlugin;
import io.lighty.yang.validator.formats.MultiModulePrinter;
import io.lighty.yang.validator.formats.Tree;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TreeSimplifiedTest implements Cleanable {

    private String yangPath;
    private Format formatter;
    private ConfigurationBuilder builder;
    private String outPath;

    private Method method;
    private Constructor<Main> constructor;

    @BeforeClass
    public void init() {
        outPath = TreeSimplifiedTest.class.getResource("/out").getFile();
        yangPath = TreeSimplifiedTest.class.getResource("/yang").getFile();
    }

    @BeforeMethod
    public void setUpOutput() throws Exception {
        constructor = (Constructor<Main>) Main.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        method = Main.class.getDeclaredMethod("setMainLoggerOutput", Configuration.class);
        final Main mainClass = constructor.newInstance();
        method.setAccessible(true);
        builder = new ConfigurationBuilder()
                .setRecursive(false)
                .setOutput(outPath)
                .setPath(Collections.singletonList(yangPath));
        method.invoke(mainClass, builder.build());
    }

    @AfterMethod
    public void removeOuptut() throws Exception {
        tearDown();
        method.setAccessible(false);
        constructor.setAccessible(false);
    }

    @Test
    public void runTreeSimplifiedTest() throws Exception {
        prepare("tree", new Tree());
        final String module = Paths.get(yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        final var configuration = builder.build();
        final var lyvContext = Main.getLyvContext(ImmutableList.of(module), configuration);
        final var modules = lyvContext.testedModules();
        assertEquals(modules.size(), 1);
        runLYV(modules.iterator().next(), configuration, formatter, lyvContext.context());
        final Path outLog = Paths.get(outPath).resolve("out.log");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(
            outLog.resolveSibling("compare").resolve("interfacesSimplified.tree"));
        assertEquals(fileCreated, compareWith);
    }

    @Test
    public void runYangSimplifiedTest() throws Exception {
        prepare("yang", new MultiModulePrinter());
        final String module = Paths.get(yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        final var configuration = builder.build();
        final var lyvContext = Main.getLyvContext(ImmutableList.of(module), configuration);
        final var modules = lyvContext.testedModules();
        assertEquals(modules.size(), 1);
        runLYV(modules.iterator().next(), configuration, formatter, lyvContext.context());
        final Path outLog = Paths.get(outPath).resolve("ietf-interfaces@2018-02-20.yang");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(
            outLog.resolveSibling("compare").resolve("interfaces-simplified.yang"));
        assertEquals(fileCreated, compareWith);
    }

    private void prepare(final String format, final FormatPlugin plugin) {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(plugin);
        final String xmlPath = TreeSimplifiedTest.class.getResource("/xml").getFile();

        formatter = new Format(formats);
        builder.setFormat(format)
                .setSimplify(xmlPath)
                .setTreeConfiguration(0, 0, false, false, false)
                .build();
    }

}
