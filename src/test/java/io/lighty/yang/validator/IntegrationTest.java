/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator;

import io.lighty.yang.validator.utils.ItUtils;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class IntegrationTest implements Cleanable {

    @AfterMethod
    public void cleanOutput() throws IOException {
        tearDown();
    }

    @Test
    public void treeFormatParseAllTest() throws IOException {
        String lyvOutput = ItUtils.startLyvParseAllWithFileOutput("integration/yang/parse/all", "tree");
        String outputWithoutGenInfo = ItUtils.removeHtmlGeneratedInfo(lyvOutput);
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestTreeParseAll.txt");

        ItUtils.compareModulesAndAugmentData(expectedOutput, outputWithoutGenInfo);
    }

    @Test
    public void notFoundImportFormatParseAllTest() throws IOException {
        String lyvOutput = ItUtils.startLyvParseAllWithFileOutput("integration/yang", "tree");

        Assert.assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        Assert.assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void wrongYangTreeFormatParseAllTest() throws IOException {
        String lyvOutput = ItUtils.startLyvParseAllWithFileOutput("integration/xml/", "tree");

        Assert.assertTrue(lyvOutput.contains("Failed to create YangContextFactory"));
        Assert.assertTrue(lyvOutput.contains("Model with specific module-name does not exist"));
    }

    @Test
    public void treeFormatTest() throws IOException {
        String lyvOutput = ItUtils.startLyvWithFileOutput("yang/test_model@2020-12-03.yang", "tree");
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestTree.txt");
        Assert.assertEquals(expectedOutput, lyvOutput);
    }

    @Test
    public void treeFormatRecursivelyTest() throws IOException {
        String lyvOutput = ItUtils.startRecursivelyLyvWithFileOutput("yang",
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "tree");
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestTreeRecursive.txt");
        Assert.assertEquals(expectedOutput, lyvOutput);
    }

    @Test
    public void notFoundImportTreeFormatTest() throws IOException {
        String lyvOutput = ItUtils.startLyvWithFileOutput(
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "tree");
        Assert.assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        Assert.assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void dependFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/ietf-netconf-config@2013-10-21.yang", "depend");
        final String expected = "module ietf-netconf-config@2013-10-21 depends on following modules: "
                + "ietf-inet-types ietf-netconf-acm ietf-yang-types ietf-netconf-common@2013-10-21 "
                + "ietf-netconf-common@2013-10-21ietf-netconf-tls@2013-10-21 ietf-x509-cert-to-name \n";
        ItUtils.compareDependFormatOutput(expected, lyvOutput);
    }

    @Test
    public void notFoundImportDependFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput(
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "depend");
        Assert.assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        Assert.assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void jsonTreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/test_model@2020-12-03.yang", "json-tree");
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestJsonTree.json");
        Assert.assertEquals(expectedOutput, lyvOutput);
    }

    @Test
    public void notFoundImportJsonTreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput(
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "json-tree");
        Assert.assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        Assert.assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void jstreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/test_model@2020-12-03.yang", "jstree");
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestJsTree.html");
        Assert.assertEquals(expectedOutput, lyvOutput.replaceAll(" \n", "\n"));
    }

    @Test
    public void notFoundImportJstreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput(
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "jstree");
        Assert.assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        Assert.assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void yangFormatTest() throws IOException {
        String errorLog = ItUtils.startLyvWithFileOutput("integration/xml", "yang",
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "yang");
        Assert.assertTrue(errorLog.isEmpty());
        String lyvOutput = ItUtils.loadLyvOutput("/out/ietf-interfaces-modified@2018-02-20.yang");
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestYang.txt");
        ItUtils.compareSimplifyYangOutput(expectedOutput, lyvOutput);
    }

    @Test
    public void notFoundImportYangFormatTest() throws IOException {
        String lyvOutput = ItUtils.startLyvWithFileOutput("integration/xml", "integration/yang",
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "yang");
        Assert.assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        Assert.assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }
}
