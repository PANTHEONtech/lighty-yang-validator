package io.lighty.yang.validator;

import io.lighty.yang.validator.utils.ItUtils;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class IntegrationTest implements Cleanable {

    @AfterClass
    public void cleanOutput() throws IOException {
        tearDown();
    }

    @Test
    public void treeFormatParseAllTest() throws IOException {
        String lyvOutput = ItUtils.startLyvParseAllWithFileOutput("yang/parse/all", "tree");
        String outputWithoutGenInfo = ItUtils.removeHtmlGeneratedInfo(lyvOutput);
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestTreeParseAll.txt");
        Assert.assertEquals(expectedOutput, outputWithoutGenInfo);
    }

    @Test
    public void treeFormatTest() throws IOException {
        String lyvOutput = ItUtils.startLyvWithFileOutput("yang/test_model@2020-12-03.yang", "tree");
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestTree.txt");
        Assert.assertEquals(expectedOutput, lyvOutput);
    }

    @Test
    public void dependFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/ietf-netconf-config@2013-10-21.yang", "depend");
        final String expected = "module ietf-netconf-config@2013-10-21 depends on following modules: "
                + "ietf-inet-types ietf-netconf-acm ietf-yang-types ietf-netconf-common@2013-10-21 "
                + "ietf-netconf-common@2013-10-21ietf-netconf-tls@2013-10-21 ietf-x509-cert-to-name \n";
        Assert.assertEquals(expected, lyvOutput);
    }

    @Test
    public void jsonTreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/test_model@2020-12-03.yang", "json-tree");
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestJsonTree.txt");
        Assert.assertEquals(expectedOutput, lyvOutput);
    }

    @Test
    public void jstreeFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("yang/test_model@2020-12-03.yang", "jstree");
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestJsTree.txt");
        Assert.assertEquals(expectedOutput, lyvOutput.replaceAll(" \n", "\n"));
    }

//    @Test
    public void yangFormatTest() throws IOException {
        final String lyvOutput = ItUtils.startLyvWithFileOutput("xml", "yang",
                "yang/without/imports/in/root/ietf-interfaces@2018-02-20.yang", "yang");
        String expectedOutput = ItUtils.getExpectedOutput("integrationTestYang.txt");
        Assert.assertEquals(expectedOutput, lyvOutput);
    }
}
