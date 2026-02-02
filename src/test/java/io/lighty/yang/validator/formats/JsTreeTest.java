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
import static org.testng.Assert.assertTrue;

import io.lighty.yang.validator.FormatTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

public class JsTreeTest extends FormatTest {

    @Override
    public void setFormat() {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new JsTree());
        formatter = new Format(formats);
        builder.setFormat("jstree");
    }

    @Test
    public void testUndeclared() throws Exception {
        //testing for undeclared choice-case statement (no case inside of choice)
        setFormat();
        final String module = Paths.get(yangPath).resolve("undeclared.yang").toString();
        builder.setYangModules(List.of(module));
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        runJsTreeTest("undeclared.html");
    }

    @Override
    public void runInterfacesTest() throws Exception {
        runJsTreeTest("interfaces.html");
    }

    @Override
    public void runIpTest() throws Exception {
        runJsTreeTest("ip.html");
    }

    @Override
    public void runConnectionOrentedOamTest() throws Exception {
        runJsTreeTest("connectionOrientedOam.html");
    }

    @Override
    public void runRoutingTest() throws Exception {
        runJsTreeTest("routing.html");
    }

    @Override
    public void runCustomModuleTest() throws Exception {
        runJsTreeTest("testModel.html");
    }

    @Override
    public void runDeviationTest() throws Exception {
        runJsTreeTest("moduleDeviation.html");
    }

    @Test
    public void testMultipleFiles() throws IOException {
        final String module1 = Paths.get(yangPath + "/ietf-connection-oriented-oam@2019-04-16.yang").toString();
        final String module2 = Paths.get(yangPath + "/ietf-routing@2018-03-13.yang").toString();
        builder.setYangModules(List.of(module1, module2));
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new JsTree());
        formatter = new Format(formats);
        builder.setFormat("jstree");
        final var configuration = builder.build();
        startLyv(configuration, formatter);
        final Path outLog = Paths.get(outPath).resolve("out.log");
        String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve("multipleModules.html"));

        //assert that file contains the first module
        assertTrue(fileCreated.contains("<b> module name ietf-connection-oriented-oam@2019-04-16, "
                + "namespace urn:ietf:params:xml:ns:yang:ietf-connection-oriented-oam, prefix co-oam </b>"));
        //assert that file contains the second module
        assertTrue(fileCreated.contains("<b> module name ietf-routing@2018-03-13, "
                + "namespace urn:ietf:params:xml:ns:yang:ietf-routing, prefix rt </b>"));
        //assert that there is only one <body> tag
        final int bodyTagCount = fileCreated.split("<body>").length - 1;
        assertEquals(1, bodyTagCount);
        //assert that outputs match
        assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
    }

    private void runJsTreeTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(outPath).resolve("out.log");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve(comapreWithFileName));
        assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
    }

}