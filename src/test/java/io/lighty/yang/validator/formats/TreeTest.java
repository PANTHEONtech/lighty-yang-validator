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
import io.lighty.yang.validator.FormatTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TreeTest extends FormatTest {

    private static final String MODULE_DEVIATION_EXPECT = "module: deviation\n";

    @Override
    public void setFormat() {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new Tree());
        formatter = new Format(formats);
        builder.setFormat("tree");
        builder.setTreeConfiguration(0, 0, false, false, false);
    }

    @Test
    public void treePrefixMainModuleTest() throws Exception {
        setFormat();
        builder.setTreeConfiguration(0, 0, false, false, true);
        final String module = Paths.get(yangPath).resolve("ietf-ip@2018-02-22.yang").toString();
        builder.setYangModules(ImmutableList.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runTreeTest("ip-prefix-main-module.tree");
    }

    @Test
    public void treePrefixModuleTest() throws Exception {
        setFormat();
        builder.setTreeConfiguration(0, 0, false, true, false);
        final String module = Paths.get(yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        builder.setYangModules(ImmutableList.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runTreeTest("interfaces-prefix-module.tree");
    }

    @Test
    public void treeDeviationTest() throws Exception {
        setFormat();
        builder.setTreeConfiguration(0, 0, false, true, false);
        final var deviation = Paths.get(yangPath + "/deviation/deviation.yang").toString();
        final var module = Paths.get(yangPath + "/deviation/model.yang").toString();
        builder.setYangModules(ImmutableList.of(module, deviation));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runTreeTest("module-deviation.tree");
    }

    @Test
    public void treeLineLengthTest() throws Exception {
        setFormat();
        builder.setTreeConfiguration(0, 20, false, false, false);
        final String module = Paths.get(yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        builder.setYangModules(ImmutableList.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runTreeTest("interfaces-line-length.tree");
    }

    @Test
    public void treeHelpTest() throws Exception {
        setFormat();
        builder.setTreeConfiguration(0, 0, true, false, false);
        builder.setYangModules(List.of());
        startLyv(builder.build(), formatter);
        runTreeTest("tree-help");
    }

    @Test
    public void treeDepthTest() throws Exception {
        setFormat();
        builder.setTreeConfiguration(3, 0, false, false, false);
        final String module = Paths.get(yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        builder.setYangModules(ImmutableList.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runTreeTest("interfaces-limited-depth.tree");
    }

    @Override
    public void runInterfacesTest() throws Exception {
        runTreeTest("interfaces.tree");
    }

    @Override
    public void runIpTest() throws Exception {
        runTreeTest("ip.tree");
    }

    @Override
    public void runConnectionOrentedOamTest() throws Exception {
        runTreeTest("connectionOrientedOam.tree");
    }

    @Override
    public void runRoutingTest() throws Exception {
        runTreeTest("routing.tree");
    }

    @Override
    public void runCustomModuleTest() throws Exception {
        runTreeTest("testModel.tree");
    }

    private void runTreeTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(outPath).resolve("out.log");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve(comapreWithFileName));
        Assert.assertEquals(fileCreated, compareWith);
    }

}