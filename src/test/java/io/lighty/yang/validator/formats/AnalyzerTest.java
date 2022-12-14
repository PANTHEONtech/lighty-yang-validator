/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import static io.lighty.yang.validator.Main.startLyv;

import com.google.common.collect.ImmutableList;
import io.lighty.yang.validator.Cleanable;
import io.lighty.yang.validator.Main;
import io.lighty.yang.validator.MainTest;
import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.config.ConfigurationBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AnalyzerTest implements Cleanable {

    private String yangPath;
    private Format formatter;
    private ConfigurationBuilder builder;
    private String outPath;
    private Method method;
    private Constructor<Main> constructor;

    @BeforeClass
    public void init() {
        outPath = TreeTest.class.getResource("/out").getFile();
        yangPath = MainTest.class.getResource("/yang").getFile();

        builder = new ConfigurationBuilder()
                .setRecursive(false)
                .setOutput(outPath);
    }

    @BeforeMethod
    public void setUpOutput() throws Exception {
        constructor = (Constructor<Main>) Main.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        final Main mainClass = constructor.newInstance();

        method = Main.class.getDeclaredMethod("setMainLoggerOutput", Configuration.class);
        method.setAccessible(true);
        method.invoke(mainClass, builder.build());
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new Analyzer());
        formatter = new Format(formats);
        builder.setFormat("analyze");
        builder.setTreeConfiguration(0, 0, false, false, false);
    }

    @AfterMethod
    public void removeOuptut() throws Exception {
        tearDown();
        method.setAccessible(false);
        constructor.setAccessible(false);
    }


    @Test
    public void analyzeTest() throws Exception {
        final String module = Paths.get(yangPath).resolve("ietf-netconf-common@2013-10-21.yang").toString();
        builder.setYangModules(ImmutableList.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runAnalyzeTest("ietf-netconf-common-analyzed");
    }


    private void runAnalyzeTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(outPath).resolve("out.log");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve(comapreWithFileName));
        Assert.assertEquals(fileCreated, compareWith);
    }

}