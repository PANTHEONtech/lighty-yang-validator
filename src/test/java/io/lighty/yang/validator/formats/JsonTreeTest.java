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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JsonTreeTest extends FormatTest {

    @Override
    public void setFormat() {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new JsonTree());
        formatter = new Format(formats);
        builder.setFormat("json-tree");
    }

    @Override
    public void runInterfacesTest() throws Exception {
        runJsonTreeTest("interfaces.json");
    }

    @Override
    public void runIpTest() throws Exception {
        runJsonTreeTest("ip.json");
    }

    @Override
    public void runConnectionOrentedOamTest() throws Exception {
        runJsonTreeTest("connectionOrientedOam.json");
    }

    @Override
    public void runRoutingTest() throws Exception {
        runJsonTreeTest("routing.json");
    }

    @Override
    public void runCustomModuleTest() throws Exception {
        runJsonTreeTest("testModel.json");
    }

    @Override
    public void runDeviationTest() throws Exception {
        runJsonTreeTest("ModuleDeviation.json");
    }

    @Test
    public void testMultipleFiles() throws IOException {
        final String module1 = Paths.get(yangPath + "/ietf-connection-oriented-oam@2019-04-16.yang").toString();
        final String module2 = Paths.get(yangPath + "/ietf-routing@2018-03-13.yang").toString();
        builder.setYangModules(ImmutableList.of(module1, module2));
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new JsonTree());
        formatter = new Format(formats);
        builder.setFormat("json-tree");
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        final Path outLog = Paths.get(outPath).resolve("out.log");
        String fileCreated = Files.readString(outLog);
        fileCreated = fileCreated.substring(fileCreated.indexOf("{"));
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve("multipleModules.json"));
        final String compareModel1 = Files.readString(
                outLog.resolveSibling("compare").resolve("connectionOrientedOam.json"))
                .replaceFirst("\\{\\s+\"parsed-models\": \\[\\s+\\{\n", "")
                .replaceFirst("]\n\\s*}$", "").replaceAll("\\n\\s*", "");
        final String compareModel2 = Files.readString(
                outLog.resolveSibling("compare").resolve("routing.json"))
                .replaceFirst("\\{\\s+\"parsed-models\": \\[\\s+\\{\n", "")
                .replaceFirst("]\n\\s*}$", "").replaceAll("\\n\\s*", "");

        Assert.assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
        Assert.assertTrue(fileCreated.replaceAll("\\s+", "").contains(compareModel1.replaceAll("\\s+", "")));
        Assert.assertTrue(fileCreated.replaceAll("\\s+", "").contains(compareModel2.replaceAll("\\s+", "")));
    }

    private void runJsonTreeTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(outPath).resolve("out.log");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve(comapreWithFileName));
        Assert.assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
    }

}