/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
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

public class NameRevisionTest extends FormatTest {

    @Override
    public void setFormat() {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new NameRevision());
        formatter = new Format(formats);
        builder.setFormat("name-revision");
    }

    @Override
    public void runInterfacesTest() throws Exception {
        runNameRevisionTest("interfaces-name-revision");
    }

    @Override
    public void runIpTest() throws Exception {
        runNameRevisionTest("ip-name-revision");
    }

    @Override
    public void runConnectionOrentedOamTest() throws Exception {
        runNameRevisionTest("connectionOrientedOam-name-revision");
    }

    @Override
    public void runRoutingTest() throws Exception {
        runNameRevisionTest("routing-name-revision");
    }

    @Override
    public void runCustomModuleTest() throws Exception {
        runNameRevisionTest("testModel-name-revision");
    }

    @Override
    public void runDeviationTest() throws Exception {
        runNameRevisionTest("moduleDeviation-name-revision");
    }

    @Override
    public void runMultipleFilesTest() throws Exception {
        runNameRevisionTest("multipleModules-name-revision");
    }

    private void runNameRevisionTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(outPath).resolve("out.log");
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling("compare").resolve(comapreWithFileName));
        Assert.assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
    }

}