package org.datadryad.api;

/** 
    Utilities to facilitate communication with the DASH-based version of Dryad.

    To use from the command line:
    - retrieve an item from Dash:
      /opt/dryad/bin/dspace dash-service <doi>
 **/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;




public class DashService {

    private static final Logger log = Logger.getLogger(DashService.class);
    private String dashServer = "";
    private String oauthToken = "";

    public DashService() {
        // init oauth connection with DASH
        dashServer = ConfigurationManager.getProperty("dash.server");
        String dashAppID = ConfigurationManager.getProperty("dash.application.id");
        String dashAppSecret = ConfigurationManager.getProperty("dash.application.secret");
        
        oauthToken = getOAUTHtoken(dashServer, dashAppID, dashAppSecret);
    }

    
    private String getOAUTHtoken(String dashServer, String dashAppID, String dashAppSecret) {
        
        String url = dashServer + "/oauth/token";
        String auth = dashAppID + ":" + dashAppSecret;
        String authentication = Base64.getEncoder().encodeToString(auth.getBytes());
        Pattern tokenPattern = Pattern.compile(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*");
        String token = "";

        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", "Basic " + authentication);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            con.setRequestProperty("Accept", "application/json");
            
            PrintStream os = new PrintStream(con.getOutputStream());
            os.print("grant_type=client_credentials");
            os.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = null;
            StringWriter out = new StringWriter(con.getContentLength() > 0 ? con.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String response = out.toString();
            Matcher matcher = tokenPattern.matcher(response);
            if (matcher.matches() && matcher.groupCount() > 0) {
                token = matcher.group(1);
            }

            log.info("got OAuth token " + token);

        } catch (Exception e) {
            log.fatal("Unable to obtain OAuth token", e);
        }

        return token;
    }

    /**
       Read the JSON format of a dataset from Dash.
    **/
    public String getDashJSON(String doi) {
        String response = "";
        log.debug("getting Dash JSON for " + doi);
        
        try {
            doi = URLEncoder.encode(doi, "UTF-8");
            URL url = new URL(dashServer + "/api/datasets/" + doi);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = null;
            StringWriter responseContent = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }
            response = responseContent.toString();
            log.debug("got response content: " + responseContent);
        } catch (Exception e) {
            log.error("Unable to get Dash JSON", e);
        }
        
        return response;
    }

    /**
       POSTs a DryadDataPackage to Dash, creating a new submission. This will eventually be
       deprecated in favor of PUT, to ensure that only one submission is created for each identifier.
    **/
    public String postDataPackage(DryadDataPackage dataPackage) {
        String dashJSON = dataPackage.getDashJSON();
        String responseCode = "POST not completed";
        log.debug("Got JSON object: " + dashJSON);
            
        BufferedReader reader = null;

        //TODO: replace this pattern matching with real JSON parsing 
        Pattern datasetIDPattern = Pattern.compile("(/api/datasets/.+?)\"},"); 
        
        try {
            URL url = new URL(dashServer + "/api/datasets");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(dashJSON);
            wr.close();

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }

            responseCode = connection.getResponseCode() + " " + connection.getResponseMessage();
            String response = out.toString();
            String datasetID = "";
            Matcher matcher = datasetIDPattern.matcher(response);
            if (matcher.matches() && matcher.groupCount() > 0) {
                datasetID = matcher.group(1);
            }

            dataPackage.addDashTransferDate();
            log.info("got dataset ID " + datasetID);
            log.info("result object " + response);
        } catch (Exception e) {
            log.fatal("Unable to send item to DASH", e);
        }

        return responseCode;
    }
    

    public static void main(String[] args) throws IOException {
        
        String usage = "\n\nUsage: \n" +
            "\tlookup a specific item: dash-service doi\n";
        DashService service = new DashService();

        // LOOKUP: args[0]=DOI
        if (args.length == 1) {
            String doiID = args[0];
            System.out.println(service.getDashJSON(doiID));
        } else {
            System.out.println(usage);
        }
    }


}
