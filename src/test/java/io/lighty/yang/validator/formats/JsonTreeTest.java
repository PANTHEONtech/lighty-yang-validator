/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import io.lighty.yang.validator.FormatTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;

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

    private void runJsonTreeTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(outPath).resolve("out.log");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve(comapreWithFileName));
        Assert.assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
    }

}