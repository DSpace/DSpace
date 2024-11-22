/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.testing;

import org.apache.commons.cli.ParseException;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Testing extends DSpaceRunnable<TestingScriptConfiguration> {

    @Override
    public TestingScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
                .getServiceByName("testing-script", TestingScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {

    }

    @Override
    public void internalRun() throws Exception {
        System.out.println("Hello world from java");
        try {
            // loading python scripts stored in resources
            InputStream scriptInputStream = getClass().getClassLoader().getResourceAsStream("python-script.py");
            if (scriptInputStream == null) {
                throw new FileNotFoundException("Python script not found in resources");
            }

            File tempFile = File.createTempFile("python-script", ".py");
            tempFile.deleteOnExit();
            // transfer the content to the temporary file
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                scriptInputStream.transferTo(out);
            }

            // configure ProcessBuilder to run script using python commands
            ProcessBuilder processBuilder = new ProcessBuilder("python", tempFile.getAbsolutePath());
            processBuilder.directory(tempFile.getParentFile());
            Process process = processBuilder.start();

            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(inputStreamReader);

            // reading the scripts output and then outputs it to the  console
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }

            inputStreamReader.close();
            reader.close();
            int exitCode = process.waitFor();
            System.out.println("Exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
