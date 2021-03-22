/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.utils;

import io.lighty.yang.validator.Main;
import io.lighty.yang.validator.MainTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;

public final class ItUtils {
    private static final String OUTPUT_FOLDER = "/out";
    private static final String OUTPUT_LOG = "/out/out.log";

    private ItUtils() {
        throw new UnsupportedOperationException("Util class");
    }

    public static String startLyvWithFileOutput(final String xmlFolder, final String yangImports,
            final String modelPath, final String format) throws IOException {
        URL xmlFolderURL = MainTest.class.getClassLoader().getResource(xmlFolder);
        Assert.assertNotNull(xmlFolderURL);
        URL yangImportsURL = MainTest.class.getClassLoader().getResource(yangImports);
        Assert.assertNotNull(yangImportsURL);
        URL moduleURL = MainTest.class.getClassLoader().getResource(modelPath);
        Assert.assertNotNull(moduleURL);

        final String outPath = MainTest.class.getResource(OUTPUT_FOLDER).getFile();
        final String[] args = {"-o", outPath, "-s", xmlFolderURL.getPath(), "-p", yangImportsURL.getPath(), "-f",
                               format, moduleURL.getPath()};
        return startLyvWithFileOutput(args);
    }

    public static String startLyvWithFileOutput(final String modelPath, final String format) throws IOException {
        URL resource = MainTest.class.getClassLoader().getResource(modelPath);
        Assert.assertNotNull(resource);
        final String outPath = MainTest.class.getResource(OUTPUT_FOLDER).getFile();
        final String[] args = {"-o", outPath, "-f", format, resource.getPath()};
        return startLyvWithFileOutput(args);
    }

    public static String startLyvWithFileOutput(final String[] args) throws IOException {
        Main.main(args);
        InputStream out = MainTest.class.getResourceAsStream(OUTPUT_LOG);
        Assert.assertNotNull(out);
        return IOUtils.toString(out, StandardCharsets.UTF_8);
    }

    public static String startLyvParseAllWithFileOutput(final String modelFolder, final String format)
            throws IOException {
        URL resource = MainTest.class.getClassLoader().getResource(modelFolder);
        Assert.assertNotNull(resource);
        final String outPath = MainTest.class.getResource(OUTPUT_FOLDER).getFile();
        final String[] args = {"-o", outPath, "-f", format, "-a", resource.getPath()};
        return startLyvWithFileOutput(args);
    }

    public static String getExpectedOutput(String fileName) throws IOException {
        InputStream inputStream = MainTest.class.getClassLoader()
                .getResourceAsStream(String.format("out/compare/integration/%s", fileName));
        Assert.assertNotNull(inputStream);
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    public static String removeHtmlGeneratedInfo(final String text) {
        int length = text.length();
        int cut = 0;
        for (int i = length - 2; i >= 0; i--) {
            if (text.charAt(i) == '\n') {
                cut = i;
                break;
            }
        }
        return text.substring(0, cut);
    }

    public static void compareModulesAndAugmentData(final String expected, final String output) {
        List<String> splitExp =  Arrays.stream(expected.split("module|augment"))
                .map(String::trim)
                .filter(t -> !(t.isBlank() || t.isEmpty()))
                .collect(Collectors.toList());
        List<String> splitOut = Arrays.stream(output.split("module|augment"))
                .map(String::trim)
                .filter(t -> !(t.isBlank() || t.isEmpty()))
                .collect(Collectors.toList());

        verifyLengthOfElements(splitExp, splitOut);
        Collections.sort(splitExp);
        Collections.sort(splitOut);
        for (int i = 0; i < splitExp.size(); i++) {
            Assert.assertEquals(splitExp.get(i), splitOut.get(i));
        }
    }


    private static void verifyLengthOfElements(List<String> expected, List<String> output) {
        if (expected.size() > output.size()) {
            List<String> notFoundExpectedElements = expected.stream()
                    .filter(t -> !output.contains(t))
                    .collect(Collectors.toList());
            Assert.fail(String.format("Expected elements are not contained in output %s", notFoundExpectedElements));
        }
        if (expected.size() < output.size()) {
            List<String> additionalOutputElements = output.stream()
                    .filter(t -> !expected.contains(t))
                    .collect(Collectors.toList());
            Assert.fail(String.format("Additional elements contained in output %s", additionalOutputElements));
        }
    }
}
