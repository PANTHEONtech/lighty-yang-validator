/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import static io.lighty.yang.validator.Main.startLyv;
import static org.testng.Assert.assertEquals;

import io.lighty.yang.validator.FormatTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.testng.annotations.Test;

public class DependsTest extends FormatTest {

    @Override
    public void setFormat() {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new Depends());
        formatter = new Format(formats);
        builder.setFormat("depend");
        builder.setDependConfiguration(false, false, false,
                new HashSet<>());
    }

    @Test
    public void onlySubmodulesTest() throws Exception {
        setFormat();
        builder.setDependConfiguration(false, false, true,
                new HashSet<>());
        final String module = Paths.get(yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_submodule-dependencies");
    }

    @Test
    public void onlyImportsTest() throws Exception {
        setFormat();
        builder.setDependConfiguration(false, true, false,
                new HashSet<>());
        final String module = Paths.get(yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_import-dependencies");
    }

    @Test
    public void dependsTestNotRecursive() throws Exception {
        setFormat();
        builder.setDependConfiguration(true, false, false,
                new HashSet<>());
        final String module = Paths.get(yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_non-recursive-dependencies");
    }

    @Test
    public void dependsTestNotRecursiveSubmodulesOnly() throws Exception {
        setFormat();
        builder.setDependConfiguration(true, false, true,
                new HashSet<>());
        final String module = Paths.get(yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_non-recursive-only-submodules-dependencies");
    }

    @Test
    public void dependsTestNotRecursiveModulesOnly() throws Exception {
        setFormat();
        builder.setDependConfiguration(true, true, false,
                new HashSet<>());
        final String module = Paths.get(yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_non-recursive-only-imports-dependencies");
    }

    @Test
    public void dependsTestExcludeModule() throws Exception {
        setFormat();
        builder.setDependConfiguration(false, false, false,
                new HashSet<>(Collections.singleton("ietf-ipv6-router-advertisements")));
        final String module = Paths.get(yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_exclude-module-dependencies");
    }

    @Override
    public void runInterfacesTest() throws Exception {
        runDependendsTest("interfaces-dependencies");
    }

    @Override
    public void runIpTest() throws Exception {
        runDependendsTest("ip-dependencies");
    }

    @Override
    public void runConnectionOrentedOamTest() throws Exception {
        runDependendsTest("connectionOrientedOam-dependencies");
    }

    @Override
    public void runRoutingTest() throws Exception {
        runDependendsTest("routing-dependencies");
    }

    @Override
    public void runCustomModuleTest() throws Exception {
        runDependendsTest("testModel-dependencies");
    }

    @Override
    public void runDeviationTest() throws Exception {
        runDependendsTest("moduleDeviation-dependencies");
    }

    private void runDependendsTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(outPath).resolve("out.log");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve(comapreWithFileName));
        assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
    }

}
