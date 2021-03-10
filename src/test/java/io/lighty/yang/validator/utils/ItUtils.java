package io.lighty.yang.validator.utils;

import io.lighty.yang.validator.Main;
import io.lighty.yang.validator.MainTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    /**
     * Remove information about generated file included folder path and generated time.
     * @param text output from lyv with parameter parse-all/-a
     * @param endOfTestedFile required end of tested file
     * @return output without generated info
     */
    public static String removeHtmlGeneratedInfo(final String text, final String endOfTestedFile) {
        int length = text.length();
        int cut = 0;
        for (int i = length - 2; i > endOfTestedFile.length(); i--) {
            if (text.startsWith(endOfTestedFile, i - endOfTestedFile.length())) {
                cut = i;
                break;
            }
        }
        return text.substring(0, cut);
    }
}
