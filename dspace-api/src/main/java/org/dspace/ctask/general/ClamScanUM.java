package org.dspace.ctask.general;

import java.io.File;
import java.io.InputStream;

import java.io.*;
import java.net.Socket;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;


import org.apache.logging.log4j.Logger;
    

public class ClamScanUM {
    private String clamHost;
    private int clamPort;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ClamScanUM.class);

    private static ConfigurationService configurationService = 
       DSpaceServicesFactory.getInstance().getConfigurationService();

    protected final String PLUGIN_PREFIX = "clamav";

    public ClamScanUM() {
        this("localhost", 3310); // default values
    }

    public ClamScanUM(String clamHost, int clamPort) {

        String host = configurationService.getProperty(PLUGIN_PREFIX + ".service.host");
        int port = configurationService.getIntProperty(PLUGIN_PREFIX + ".service.port");

        //timeout = configurationService.getIntProperty(PLUGIN_PREFIX + ".socket.timeout");
        //failfast = configurationService.getBooleanProperty(PLUGIN_PREFIX + ".scan.failfast");
        log.info("VIRUS: Found a virus host=" + host);
        log.info("VIRUS: Found a virus port=" + Integer.toString(port));

        this.clamHost = host;
        this.clamPort = port;
    }

    public ClamScanResult virusCheck(InputStream stream) {

    boolean doVirus = configurationService.getBooleanProperty("bitstream.virus.check", false);

    if (!doVirus) {
       return new ClamScanResult(false, "VIRUS:Not checking for Virus, because not config to check.");
    }

    try (Socket socket = new Socket(clamHost, clamPort);
         BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

        // Send zINSTREAM to clamd
        out.write("zINSTREAM\0".getBytes("US-ASCII"));
        out.flush();

        // Send file contents in chunks (as per clamd protocol)
        byte[] buf = new byte[2048];
        int read;
        while ((read = stream.read(buf)) != -1) {
            // clamd expects a 4-byte length prefix
            out.write(new byte[] {
                (byte)(read >>> 24),
                (byte)(read >>> 16),
                (byte)(read >>> 8),
                (byte)(read)
            });
            out.write(buf, 0, read);
            out.flush();
        }
        // Indicate EOF to clamd
        out.write(new byte[] {0, 0, 0, 0});
        out.flush();

        // Read and interpret clamd's response
        String response = in.readLine();
        if (response == null) {
            return new ClamScanResult(true, "No response from clamd.");
        }
        if (response.contains("OK")) {
            return new ClamScanResult(false, response);
        } else if (response.contains("FOUND")) {
            String virus = response.substring(response.indexOf(":") + 1, response.indexOf("FOUND")).trim();
            return new ClamScanResult(true, virus);
        } else {
            return new ClamScanResult(true, "Unknown response: " + response);
        }

        } catch (IOException e) {
            return new ClamScanResult(true, "IOException: " + e.getMessage());
        }
    }

    // Nested result class
    public static class ClamScanResult {
        private boolean virusFound;
        private String details;

        public ClamScanResult(boolean virusFound, String details) {
            this.virusFound = virusFound;
            this.details = details.replaceAll("[^\\x20-\\x7E]", "?");
        }

        public boolean isVirusFound() { return virusFound; }
        public String getDetails()    { return details; }
    }
}
