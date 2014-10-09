/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.dryadwidgets.display;

import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import junit.framework.TestCase;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.serialization.XMLSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.excalibur.source.impl.HTTPClientSource;
import org.apache.excalibur.source.Source;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.FileEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

/**
 *
 * @author Nathan Day
 */
public class WidgetDisplayBitstreamGeneratorTest extends TestCase {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WidgetDisplayBitstreamGeneratorTest.class);

    LocalTestServer testServer;
    private final String resourceInputDir = "/input/";
    private final String resourceOutputDir = "/xml/";
    private final String outputFileSuffix = ".xml";
    private final String dataOneFormat = "DataONE-formatId";

    public WidgetDisplayBitstreamGeneratorTest(String testName) {
        super(testName);
    }
    
    private class GeneratorTestResource {
        public String filename;
        public String mimetype;
        public String doi;
        public GeneratorTestResource(String filename, String mimetype, String doi) {
            assert !(filename == null && mimetype == null && doi == null)
                && !(filename.equals("") || mimetype.equals("") || doi.equals(""))
                : "Bad init in GeneratorTestResource";
            this.filename = filename;
            this.mimetype = mimetype;
            this.doi = doi;
        }
    }
    ArrayList<GeneratorTestResource> resources = new ArrayList<GeneratorTestResource>();
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // add resources to handle by generator
        resources.add(new GeneratorTestResource("invert.data-may19.csv","text/csv","doi:10.5061/dryad.v1908/9"));
        resources.add(new GeneratorTestResource("DryadData_BonnPacificlamprey.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","doi:10.5061/dryad.t0391/1"));
        resources.add(new GeneratorTestResource("filter_fasta.py","application/x-python", "10.5061/dryad.6p76c3pb/8"));

        // see below for why this isn't done here
        // testServer = new LocalTestServer(null, null);
        // testServer.start();
    }
    
    @Override
    protected void tearDown() throws Exception {
        // see below for why this isn't done here
        // testServer.stop();
        super.tearDown();
    }

    /**
     * Test of generate method, of class WidgetDisplayBitstreamGenerator.
     */
    @Test
    public void testGenerate() throws Exception {
        
        for (GeneratorTestResource r : resources) {
            // creating a new test server for each resource seems expensive,
            // but it prevents a bug encountered in some requests handlers
            // not being called as expected when the server persists across
            // resources
            testServer = new LocalTestServer(null, null);
            testServer.start();

            final SourceResolver sourceResolver = genSourceResolver();
            HashMap objectModel = new HashMap();
            final String inputPath = resourceInputDir + r.filename;
            final String outputPath = this.getClass().getResource(resourceOutputDir + r.filename + outputFileSuffix).getFile();
            final File tempFile = File.createTempFile(r.filename, outputFileSuffix);
            FileOutputStream tempFS = new FileOutputStream(tempFile);
            String url = "http://" + testServer.getServiceHostName() + ":" + testServer.getServicePort() + inputPath;
            HttpRequestHandler requestHandler = makeRequestHandler(r, inputPath);
            testServer.register(inputPath, requestHandler);
            
            // request parameters from Cocoon map:generate call
            Parameters parameters = new Parameters();
            parameters.setParameter("referrer","");
            parameters.setParameter("doi",r.doi);
            parameters.setParameter("bitstream", url);
            WidgetDisplayBitstreamGenerator instance = new WidgetDisplayBitstreamGenerator();
            instance.setup(sourceResolver, objectModel, url, parameters);

            // generate pipeline content from the input file
            XMLSerializer consumer = new XMLSerializer();
            consumer.init();
            consumer.setOutputStream(tempFS);
            instance.setConsumer(consumer);
            instance.generate();
            tempFS.close();

            // doctor the output a bit, since the local test server's port changes
            removeMatchingString(tempFile.getAbsolutePath(), "http://" + testServer.getServiceHostName() + ":" + testServer.getServicePort());
           
            // check files are equal
            boolean generateSuccess = FileUtils.contentEquals(new File(outputPath), new File(tempFile.getAbsolutePath()));
            // do tmp file cleanup if we had a successful run
            if (generateSuccess) {
                tempFile.deleteOnExit();
            } else {
                log.error("Temp file for resource '" + inputPath + "' remains at '" + tempFile.getAbsolutePath() + "'");
            }
            assertTrue(generateSuccess);

            // server cleanup
            testServer.unregister(inputPath);
            testServer.stop();
        }
    }

    // remove a 
    private void removeMatchingString(String path, String str) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String temp = path + ".tmp";
        FileWriter fw = new FileWriter(temp);
        String line = null;
        Pattern p = Pattern.compile(Pattern.quote(str));
        Matcher m;
        Boolean matched = false;
        while ((line = br.readLine()) != null) {
            if (!matched) {
                m = p.matcher(line);
                if (m.find()) {
                    line = m.replaceFirst("");
                    matched = true;
                }
            }
            fw.write(line + "\n");
        }
        br.close();
        fw.close();
        Files.move(new File(temp), new File(path));
    }
    
    private HttpRequestHandler makeRequestHandler(final GeneratorTestResource r, final String resourcePath) {
        final URL url = this.getClass().getResource(resourcePath);
        final FileEntity body = new FileEntity(new File(url.getFile()), r.mimetype);
        assert url != null : "Test resource does not exist: " + r.filename;
        assert body != null && body.getContentLength() != 0: "Null or empty response body";

        HttpRequestHandler requestHandler = new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context) {
                response.setStatusCode(HttpStatus.SC_OK);
                response.setHeader(dataOneFormat, r.mimetype);
                response.setEntity(body);
            }
        };
        return requestHandler;
    }

    private SourceResolver genSourceResolver() {
        // Cocoon source resolver mock
        return new SourceResolver() {
            @Override
            public Source resolveURI(String uri) throws MalformedURLException, IOException {
                HTTPClientSource source = null;
                try {
                    source = new HTTPClientSource(uri, null, null);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
                assert source != null : "SourceResolver not instantiated for uri: " + uri;
                try {
                    source.initialize();
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
                assert source.getInputStream() != null : "Null input stream for source: " + uri;
                return source;
            }

            @Override
            public Source resolveURI(String string, String string1, Map map) throws MalformedURLException, IOException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void release(Source source) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }
    
    /*  Simulating Faults
        http://wiremock.org/simulating-faults.html
    */

    
}
