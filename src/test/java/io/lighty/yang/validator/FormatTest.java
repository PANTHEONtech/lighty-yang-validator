/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator;

import static io.lighty.yang.validator.Main.startLyv;

import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.config.ConfigurationBuilder;
import io.lighty.yang.validator.formats.Format;
import io.lighty.yang.validator.formats.TreeTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public abstract class FormatTest implements Cleanable {

    protected String yangPath;
    protected Format formatter;
    protected ConfigurationBuilder builder;
    protected String outPath;

    private Method method;
    private Constructor<Main> constructor;

    @BeforeClass
    public void init() {
        outPath = TreeTest.class.getResource("/out").getFile();
        yangPath = MainTest.class.getResource("/yang").getFile();

        this.builder = new ConfigurationBuilder()
                .setRecursive(false)
                .setOutput(this.outPath);
    }

    @BeforeMethod
    public void setUpOutput() throws Exception {
        this.constructor = (Constructor<Main>) Main.class.getDeclaredConstructors()[0];
        this.constructor.setAccessible(true);
        final Main mainClass = this.constructor.newInstance();

        this.method = Main.class.getDeclaredMethod("setMainLoggerOutput", Configuration.class);
        this.method.setAccessible(true);
        this.method.invoke(mainClass, this.builder.build());
    }

    @AfterMethod
    public void removeOuptut() throws Exception {
        tearDown();
        this.method.setAccessible(false);
        this.constructor.setAccessible(false);
    }

    @Test
    public void testIetfInterfacesAllFormats() throws Exception {
        //only root tree
        setFormat();
        final String module = Paths.get(this.yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runInterfacesTest();
    }

    @Test
    public void testIpAllFormats() throws Exception {
        //contains augmentations
        setFormat();
        final String module = Paths.get(this.yangPath).resolve("ietf-ip@2018-02-22.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runIpTest();
    }

    @Test
    public void testConnectionOrientedOamAllFormats() throws Exception {
        //contains notifications and rpcs
        setFormat();
        final String module =
                Paths.get(this.yangPath).resolve("ietf-connection-oriented-oam@2019-04-16.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runConnectionOrentedOamTest();
    }

    @Test
    public void testRoutingFormats() throws Exception {
        //contains actions
        setFormat();
        final String module = Paths.get(this.yangPath).resolve("ietf-routing@2018-03-13.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runRoutingTest();
    }

    @Test
    public void testCustomTestModelFormats() throws Exception {
        /*
         The test model contains notification with/without leaf data, rpc, list with action
         */
        setFormat();
        final String module = Paths.get(this.yangPath).resolve("test_model@2020-12-03.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runCustomModuleTest();
    }

    @Test
    public void testDeviations() throws Exception {
        setFormat();
        final var deviation = Paths.get(yangPath + "/deviation/deviation@2022-11-30.yang").toString();
        final var module = Paths.get(yangPath + "/deviation/model@2022-11-30.yang").toString();
        builder.setYangModules(List.of(module, deviation));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runDeviationTest();
    }

    public abstract void setFormat();

    public abstract void runInterfacesTest() throws Exception;

    public abstract void runIpTest() throws Exception;

    public abstract void runConnectionOrentedOamTest() throws Exception;

    public abstract void runRoutingTest() throws Exception;

    public abstract void runCustomModuleTest() throws Exception;

    public  abstract void runDeviationTest() throws Exception;

}
