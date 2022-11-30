/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.formats;

import static io.lighty.yang.validator.Main.runLYV;
import static org.testng.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import io.lighty.yang.validator.FormatTest;
import io.lighty.yang.validator.LyvEffectiveModelContextFactory;
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
        final var configuration = builder.build();
        final var lyvContext = LyvEffectiveModelContextFactory.create(ImmutableList.of(module), configuration);
        final var modules = lyvContext.testedModules();
        assertEquals(modules.size(), 1);
        runLYV(modules.iterator().next(), configuration, formatter, lyvContext.context());
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

    private void runJsTreeTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(outPath).resolve("out.log");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve(comapreWithFileName));
        assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
    }

}