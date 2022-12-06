/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

import io.lighty.yang.validator.formats.FormatPlugin;
import io.lighty.yang.validator.utils.ItUtils;
import java.io.IOException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class IntegrationTest implements Cleanable {

    @AfterClass
    public void cleanOutput() throws IOException {
        tearDown();
    }

    @Test
    public void treeFormatParseAllTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvParseAllWithFileOutput("integration/yang/parse/all", "tree");
        final String outputWithoutGenInfo = ItUtils.removeHtmlGeneratedInfo(lyvOutput);
        final String expectedOutput = ItUtils.getExpectedOutput("integrationTestTreeParseAll.txt");

        ItUtils.compareModulesAndAugmentData(outputWithoutGenInfo, expectedOutput);
    }

    @Test
    public void noFormatParseAllValidationTest() throws Exception {
        final var lyvOutput = ItUtils.startLyvParseAllWithFileOutput("integration/yang/parse/all");
        assertFalse(lyvOutput.trim().isEmpty());
        final var outputWithoutGenInfo = ItUtils.removeHtmlGeneratedInfo(lyvOutput);
        assertTrue(outputWithoutGenInfo.trim().isEmpty());
    }

    @Test
    public void noFormatValidationTest() throws Exception {
        final var outPath = ItUtils.class.getResource(ItUtils.OUTPUT_FOLDER).getFile();
        final var args = new String[]{"-o", outPath, "yang/deviation/model.yang"};
        final var lyvOutput = ItUtils.startLyvWithFileOutput(args);
        assertFalse(lyvOutput.trim().isEmpty());
        final var outputWithoutGenInfo = ItUtils.removeHtmlGeneratedInfo(lyvOutput);
        assertTrue(outputWithoutGenInfo.trim().isEmpty());
    }

    @Test
    public void notFoundImportFormatParseAllTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvParseAllWithFileOutput("integration/yang", "tree");

        assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void wrongYangTreeFormatParseAllTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvParseAllWithFileOutput("integration/xml/", "tree");

        assertTrue(lyvOutput.contains(FormatPlugin.EMPTY_MODULE_EXCEPTION));
    }

    @Test
    public void treeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/test_model@2020-12-03.yang", "tree");
        final String expectedOutput = ItUtils.getExpectedOutput("integrationTestTree.txt");
        assertEquals(lyvOutput, expectedOutput);
    }

    @Test
    public void treeFormatRecursivelyTest() throws IOException {
        final String lyvOutput = ItUtils.startRecursivelyLyvWithFileOutput("yang",
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "tree");
        final String expectedOutput = ItUtils.getExpectedOutput("integrationTestTreeRecursive.txt");
        assertEquals(lyvOutput, expectedOutput);
    }

    @Test
    public void notFoundImportTreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput(
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "tree");
        assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void dependFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/ietf-netconf-config@2013-10-21.yang", "depend");
        final String expected = "module ietf-netconf-config@2013-10-21 depends on following modules: "
                + "ietf-inet-types ietf-netconf-acm ietf-yang-types ietf-netconf-common@2013-10-21 "
                + "ietf-netconf-tls@2013-10-21 ietf-x509-cert-to-name \n";
        ItUtils.compareDependFormatOutput(lyvOutput, expected);
    }

    @Test
    public void notFoundImportDependFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput(
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "depend");
        assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void jsonTreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/test_model@2020-12-03.yang", "json-tree");
        final String expectedOutput = ItUtils.getExpectedOutput("integrationTestJsonTree.json");
        assertEquals(lyvOutput, expectedOutput);
    }

    @Test
    public void notFoundImportJsonTreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput(
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "json-tree");
        assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void jstreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/test_model@2020-12-03.yang", "jstree");
        final String expectedOutput = ItUtils.getExpectedOutput("integrationTestJsTree.html");
        assertEquals(lyvOutput.replaceAll(" \n", "\n"), expectedOutput);
    }

    @Test
    public void notFoundImportJstreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput(
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "jstree");
        assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }

    @Test
    public void yangFormatTest() throws IOException {
        final String errorLog = ItUtils.startLyvWithFileOutput("integration/xml", "yang",
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "yang");
        assertTrue(errorLog.isEmpty());
        final String lyvOutput = ItUtils.loadLyvOutput("/out/ietf-interfaces-modified@2018-02-20.yang");
        final String expectedOutput = ItUtils.getExpectedOutput("integrationTestYang.txt");
        ItUtils.compareSimplifyYangOutput(lyvOutput, expectedOutput);
    }

    @Test
    public void notFoundImportYangFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("integration/xml", "integration/yang",
                "integration/yang/ietf-interfaces-modified@2018-02-20.yang", "yang");
        assertTrue(lyvOutput.contains("Failed to parse YANG from source SourceSpecificContext"));
        assertTrue(lyvOutput.contains("Imported module [ietf-yang-types] was not found"));
    }
}
