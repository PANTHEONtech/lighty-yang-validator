/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.yang.validator.check.update.from;

import static io.lighty.yang.validator.Main.runLYV;
import static org.testng.Assert.assertEquals;

import io.lighty.yang.validator.Cleanable;
import io.lighty.yang.validator.Main;
import io.lighty.yang.validator.config.Configuration;
import io.lighty.yang.validator.config.ConfigurationBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RFC6020Test implements Cleanable {

    private static final String NEW = "/checkUpdateFromYangWithProblems/new/";
    private static final String OLD = "/checkUpdateFromYangWithProblems/old/";
    private static final String OUT = "out.log";
    private static final String COMPARE = "compare";

    private String yangPath;
    private ConfigurationBuilder builder;
    private String outPath;

    private Method method;
    private Constructor<Main> constructor;

    @BeforeClass
    public void init() {
        outPath = RFC6020Test.class.getResource("/out").getFile();
        yangPath = RFC6020Test.class.getResource("/yang").getFile();
    }

    @BeforeMethod
    public void setUpOutput() throws Exception {
        constructor = (Constructor<Main>) Main.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        method = Main.class.getDeclaredMethod("setMainLoggerOutput", Configuration.class);
        final Main mainClass = constructor.newInstance();
        method.setAccessible(true);
        final List<String> path = Collections.singletonList(yangPath);
        builder = new ConfigurationBuilder()
                .setRecursive(false)
                .setOutput(outPath)
                .setPath(path)
                .setParseAll(Collections.emptyList())
                .setCheckUpdateFromConfiguration(7950, path);
        method.invoke(mainClass, builder.build());
    }

    @AfterMethod
    public void removeOuptut() throws Exception {
        tearDown();
        method.setAccessible(false);
        constructor.setAccessible(false);
    }

    @Test
    public void testMissingRevision() throws Exception {
        testCheckUpdateFrom("missingRevision/",
                "ietf-interfaces@2018-02-20.yang",
                "ietf-interfaces@2019-02-20.yang",
                "checkUpdateFrom1");
    }

    @Test
    public void testWrongRevision() throws Exception {
        testCheckUpdateFrom("wrongRevision/",
                "ietf-interfaces@2018-02-20.yang",
                "ietf-interfaces@2014-05-08.yang",
                "checkUpdateFrom3");
    }

    @Test
    public void testNoRevision() throws Exception {
        testCheckUpdateFrom("noRevision/",
                "ietf-interfaces@2018-02-20.yang",
                "ietf-interfaces@2019-02-20.yang",
                "checkUpdateFrom2");
    }

    private void testCheckUpdateFrom(final String yangDirPart, final String oldModule,
            final String newModule, final String comapreFile) throws Exception {
        final String newMissing = NEW + yangDirPart;
        final String oldMissing = OLD + yangDirPart;
        final String newFile = RFC6020Test.class.getResource(newMissing + newModule).getFile();
        final String oldFile = RFC6020Test.class.getResource(oldMissing + oldModule).getFile();

        final Configuration config = builder.setUpdateFrom(oldFile)
                .setYangModules(Collections.singletonList(newFile))
                .build();

        runLYV(config.getYang(), config, null);

        final Path outLog = Paths.get(outPath).resolve(OUT);
        final String fileCreated = Files.readString(outLog);
        final String compareWith = Files.readString(outLog.resolveSibling(COMPARE).resolve(comapreFile));

        assertEquals(fileCreated, compareWith);
    }

}
