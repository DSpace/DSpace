package org.datadryad.api;

/** 
    Utilities to facilitate communication with the DASH-based version of Dryad.

    To use from the command line:
    - retrieve an item from Dash:
      /opt/dryad/bin/dspace dash-service <doi>
 **/

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Package;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.content.Bitstream;
import org.dspace.eperson.EPerson;

import javax.ws.rs.core.UriBuilder;

public class DashService {

    private static final Logger log = Logger.getLogger(DashService.class);
    private String dashServer = "";
    private String oauthToken = "";
    private ObjectMapper mapper = null;
    private String lastCurationStatus = "";

    public DashService() {
        // init oauth connection with DASH
        dashServer = ConfigurationManager.getProperty("dash.server");
        String dashAppID = ConfigurationManager.getProperty("dash.application.id");
        String dashAppSecret = ConfigurationManager.getProperty("dash.application.secret");

        oauthToken = getOAUTHtoken(dashServer, dashAppID, dashAppSecret);
        mapper = new ObjectMapper();
        fixHttpURLConnection();
    }

    /**
       Fix the fact that HttpURLConnection doesn't natively allow PATCH requests.

       This will be "fixed" by replacement classes in Java 11. See https://bugs.openjdk.java.net/browse/JDK-8207840
       Fix adapted from code at https://stackoverflow.com/questions/25163131/httpurlconnection-invalid-http-method-patch
     **/
    private static void fixHttpURLConnection() {
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

            methodsField.setAccessible(true);

            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodsSet.add("PATCH");
            String[] newMethods = methodsSet.toArray(new String[0]);

            methodsField.set(null/*static field*/, newMethods);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
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

            InputStream stream = con.getErrorStream();
            if (stream == null) {
                stream = con.getInputStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
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
            URL url = new URL(dashServer + "/api/v2/datasets/" + doi);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);

            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
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

    public String getDatasetID(String manuscriptNumber) {
        String response = "";
        log.debug("getting Dash datasetID for " + manuscriptNumber);

        try {
            String manu = URLEncoder.encode(manuscriptNumber, "UTF-8");
            URL url = new URL(dashServer + "/api/v2/datasets?manuscriptNumber=" + manu);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);

            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter responseContent = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }
            String json = responseContent.toString();
            log.debug("got response content: " + responseContent);
            ObjectNode jsonObj = (ObjectNode) mapper.readTree(json);
            response = jsonObj.findValue("identifier").asText();
        } catch (Exception e) {
            log.error("Unable to get Dash JSON", e);
        }
        
        return response;
    }
    
    /**
       Checks whether a dataset has been stored in DASH. First, checks whether the dryad.dashStoredDate is set.
       If not, calls the DASH API to determine whether the dataset has been fully stored (Merritt status is 'submitted')
     **/
    public boolean isDatasetStored(Package pkg) {        
        //is flag set in metadata already?
        DryadDataPackage ddp = pkg.getDataPackage();
        String storedDate = ddp.getDashStoredDate();
        if(storedDate != null && storedDate.length() > 0) {
            log.debug("dataset was previously stored");
            return true;
        }

        // call dash API and get the versionStatus
        String json = getDashJSON(pkg.getDataPackage().getVersionlessIdentifier());
        boolean isStored = false;
        try {
            ObjectNode jsonObj = (ObjectNode) mapper.readTree(json);
            String merrittStatus = jsonObj.findValue("versionStatus").asText();
            isStored = merrittStatus.equals("submitted");
            log.debug("Merritt Status = " + merrittStatus + ", " +
                      "isStored = " + isStored);
        } catch (Exception e) {
            log.error("can't parse DASH JSON", e);
        }
        if(isStored) {
            ddp.addDashStoredDate();
        }
        
        return isStored;
    }

    /*
      For items in review status, get the DASH sharingLink.
     */
    public String getSharingLink(String doi) {
        log.debug("getting sharingLink for " + doi);
        String sharingLink = null;
        // call dash API and get the link
        String json = getDashJSON(doi);
        try {
            ObjectNode jsonObj = (ObjectNode) mapper.readTree(json);
            sharingLink = jsonObj.findValue("sharingLink").asText();
            log.debug("sharingLink = " + sharingLink);
        } catch (Exception e) {
            log.error("can't parse DASH JSON", e);
        }
        
        return sharingLink;
    }

    /**
       PUTs a DryadDataPackage to Dash, creating a new submission or updating an
       existing submission (using the DOI contained in the Data Package).
       
       @return a HTTP response code
       @param pkg
    **/
    public int putDataset(Package pkg) {
        log.info("Putting dataset " + pkg.getItemID() + ", " + pkg.getDataPackage().getVersionlessIdentifier());

        int responseCode = 0;
        BufferedReader reader = null;

        // find the submitter's userId in Dash and set this as the userId in the package
        if (pkg.getDataPackage().getSubmitter() != null) {
            int dashUserId = getDashUser(pkg.getDataPackage());
            log.debug("dash user is " + dashUserId);
            if (dashUserId != 0) {
                pkg.getDataPackage().setDashUserID(dashUserId);
                log.debug("dash user set to " + pkg.getDataPackage().getDashUserID());
            }
        }

        // generate the main package JSON
        String dashJSON = pkg.getDataPackage().getDashJSON();
        log.debug("Got JSON object: " + dashJSON);
        
        try {
            String versionlessDOI = pkg.getDataPackage().getVersionlessIdentifier();
            String encodedDOI = URLEncoder.encode(versionlessDOI, "UTF-8");
            URL url = new URL(dashServer + "/api/v2/datasets/" + encodedDOI);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(dashJSON);
            wr.close();

            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String response = out.toString();

            responseCode = connection.getResponseCode();

            if(responseCode == 200 || responseCode == 201 || responseCode == 202) {
                log.debug("package create/update successful");
                pkg.getDataPackage().addDashTransferDate();
            } else {
                log.fatal("Unable to send item to DASH, response: " + responseCode +
                          connection.getResponseMessage());
            }

            log.info("result object " + response);
        } catch (Exception e) {
            log.fatal("Unable to send item to DASH", e);
        }

        updateInternalMetadata(pkg);
        
        return responseCode;
    }

    /**
       Set this package to have the appropriate embargo status in Dash. Assumes that the package has
       already transferred curation statuses to Dash, so the CurationStatusForDash has a meaningful value.
     **/
    public void setEmbargoStatus(Package pkg) {
        log.debug("setting embargo status for this package");
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        SimpleDateFormat verysdf = new SimpleDateFormat("yyyy-MM-dd");
        
        DryadDataPackage ddp = pkg.getDataPackage();
        String curationStatus = ddp.getCurationStatusForDash();
        log.debug("-- curationStatus " + curationStatus);
        
        // get target embargo settings
        String embargoType = ddp.getPackageEmbargoType();
        Date embargoDate = ddp.getPackageEmbargoDate();
        log.debug("-- embargoType " + embargoType);
        log.debug("-- embargoDate " + embargoDate);

        // exit early if the there is no date, the item was published before the cutoff date and the embargo isn't "custom"
        if(!embargoType.equals("custom") &&
           (embargoDate == null || embargoDate.length() == 0) &&
           ddp.getPublicationDate().compareTo("2018-09") < 0) {
            return;
        }

        if(curationStatus.equals("published") || curationStatus.equals("embargoed")) {
            // if it's published or embargoed, and a file is still under embargo, set the
            // status to embargoed
            if (embargoType.equals("custom") && embargoDate != null) {
                addCurationActivity(ddp, "embargoed",
                                    "Setting package-level embargo to reflect previous file-level embargo. Type=" + embargoType + ", PublicationDate=" +sdf.format(embargoDate), 
                                    getNowString(),
                                    "migration");
            } else if (embargoType.equals("oneyear") || embargoType.equals("one year")) {
                // set to the target date, or if no date, 2 years after today
                if(embargoDate == null) {
                    embargoDate = DateUtils.addYears(new Date(), 2);
                }
                addCurationActivity(ddp, "embargoed",
                                    "Setting package-level embargo to reflect previous file-level embargo. Type=" + embargoType + ", PublicationDate=" +sdf.format(embargoDate), 
                                    getNowString(),
                                    "migration");
            } else if (embargoType.equals("untilArticleAppears")) {
                // set to the date, or if no date, 1 year after today
                if(embargoDate == null) {
                    embargoDate = DateUtils.addYears(new Date(), 1);
                }
                addCurationActivity(ddp, "embargoed", 
                                    "Setting package-level embargo to reflect previous file-level embargo. Type=" + embargoType + ", PublicationDate=" +sdf.format(embargoDate), 
                                    getNowString(),
                                    "migration");
            } else {
                // no embargo, do nothing
            }                            
        } else {
            // it's in curation and a file has embargo, we can't set the
            // status to embargoed yet -- how will the curators know? just
            // make a note that curators can notice.
            if (embargoType != null && !embargoType.equals("none") && embargoDate != null) {
                addCurationActivity(ddp, "",
                                    "User has requested embargo. Type=" + embargoType + ", PublicationDate=" +sdf.format(embargoDate), 
                                    getNowString(),
                                    "migration");
            } 
        }
    }

    private String getNowString() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        return sdf.format(now);
    }
    
    /**
       Update the internal metadata in the Dash dataset for a given Package
    **/
    private void updateInternalMetadata(Package pkg) {
        DryadDataPackage ddp = pkg.getDataPackage();
        
        if (!"".equals(ddp.getManuscriptNumber())) {
            setManuscriptNumber(pkg, ddp.getManuscriptNumber());
        }
        
        if (ddp.getJournalConcept() != null) {
            setPublicationISSN(pkg, ddp.getJournalConcept().getISSN());
            setPublicationName(pkg, ddp.getJournalConcept().getFullName());
        }

        if (ddp.getPublicationDOI() != null && ddp.getPublicationDOI().length() > 0) {
            setPublicationDOI(pkg, ddp.getPublicationDOI());
        }
        
        if (ddp.getPubmedID() != null && ddp.getPubmedID().length() > 0) {
            setPubmedID(pkg, ddp.getPubmedID());
        }

        if (ddp.getDansArchiveDate() != null && ddp.getDansArchiveDate().length() > 0) {
            setDansArchiveDate(pkg, ddp.getDansArchiveDate());
        }

        if (ddp.getDansEditIRI() != null && ddp.getDansEditIRI().length() > 0) {
            setDansEditIRI(pkg, ddp.getDansEditIRI());
        }
                
        if (ddp.getFormerManuscriptNumbers().size() > 0) {
            List<String> prevFormerMSIDs = getFormerManuscriptNumbers(pkg);
            for (String msid : ddp.getFormerManuscriptNumbers()) {
                if (!prevFormerMSIDs.contains(msid)) {
                    addFormerManuscriptNumber(pkg, msid);
                }
            }
        }
        
        if (ddp.getMismatchedDOIs().size() > 0) {
            List<String> prevMismatches = getMismatchedDOIs(pkg);
            for (String doi : ddp.getMismatchedDOIs()) {
                if (!prevMismatches.contains(doi)) {
                    addMismatchedDOI(pkg, doi);
                }
            }
        }
        
        if (ddp.getDuplicatePackages(null).size() > 0) {
            List<String> prevDuplicates = getDuplicateItems(pkg);
            for (DryadDataPackage dup : ddp.getDuplicatePackages(null)) {
                if (!prevDuplicates.contains(dup.getVersionlessIdentifier())) {
                    addDuplicateItem(pkg, dup.getVersionlessIdentifier());
                }
            }
        }
    }

    
    /**
       POSTs references for all data files to Dash, attaching them to the DryadDataPackage.
       The DryadDataPackage must already have been PUT to Dash.

       @return a HTTP response code, or -1 if there is an exception that prevents getting a response code
    **/
    public int postDataFileReferences(Context context, DryadDataPackage dataPackage) {
        int responseCode = 0;
        BufferedReader reader = null;
        log.debug("posting data file references");
        try {
            log.debug("number of files: " + dataPackage.getDataFiles(context).size());
            for(DryadDataFile dryadFile : dataPackage.getDataFiles(context)) {
                String fileTitle = dryadFile.getTitle();
                String fileDescription = dryadFile.getDescription();
                String previousBitstreamFilename = "";
                for(Bitstream dspaceBitstream : dryadFile.getAllBitstreams()) {
                    if(dspaceBitstream.isDeleted()) {
                        continue;
                    }
                    log.debug("transferring bitstream " + dspaceBitstream.getName());
                    DryadBitstream dryadBitstream = new DryadBitstream(dspaceBitstream);
                    dryadBitstream.setFileDescription(fileDescription);
                    if(dryadBitstream.isReadme()) {
                        dryadBitstream.setReadmeFilename(previousBitstreamFilename);
                    } else {
                        previousBitstreamFilename = dspaceBitstream.getName();                        
                    }
                    String dashJSON = dryadBitstream.getDashReferenceJSON();
                    log.debug("Got JSON object: " + dashJSON);
                    String encodedPackageDOI = URLEncoder.encode(dataPackage.getVersionlessIdentifier(), "UTF-8");
                    URL url = new URL(dashServer + "/api/v2/datasets/" + encodedPackageDOI + "/urls");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");

                    OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                    wr.write(dashJSON);
                    wr.close();

                    responseCode = connection.getResponseCode();
                    log.info("response code = " + responseCode);

                    InputStream stream = connection.getErrorStream();
                    if (stream == null) {
                        stream = connection.getInputStream();
                    }
                    reader = new BufferedReader(new InputStreamReader(stream));
                    String line = null;
                    StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
                    while ((line = reader.readLine()) != null) {
                        out.append(line);
                    }
                    String response = out.toString();
                    log.info("result object " + response);

                    if(responseCode == 200 || responseCode == 201 || responseCode == 202) {
                        log.debug("file create/update successful");
                    } else {
                        log.fatal("Unable to send file reference to DASH, response: " + responseCode +
                                  connection.getResponseMessage());
                        return responseCode;
                    }
                }
            }
        } catch (Exception e) {
            log.fatal("Unable to send item to DASH", e);
            return -1;
        }

        return responseCode;
    }

    /**
       Delete the data files in a dataset. Assumes that the dataset is in_progress.
    **/
    public void deleteDataFiles(Package pkg) {

        // get file list
        JsonNode fileJson = getFiles(pkg);
        
        
        // for each file, call a delete
        for(int i = 0; i < fileJson.size(); i++) {
            try {
                String fileID = fileJson.get(i).get("_links").get("self").get("href").textValue();
                URL url = new URL(dashServer + fileID);
                log.info("deleting " + url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
                connection.setRequestMethod("DELETE");
                
                InputStream stream = connection.getErrorStream();
                if (stream == null) {
                    stream = connection.getInputStream();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line = null;
                StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }
                String response = out.toString();
                log.info("result object " + response);
            } catch (Exception e) {
                throw new RuntimeException("Unable to delete file from Dash", e);
            }
        }
    }
    
    public void migrateProvenances(Package pkg) {
        log.debug("migrating provenances");
        // get curationActivities from Dash package
        JsonNode curationActivities = getCurationActivity(pkg);
        JsonNode provenances = pkg.getDataPackage().getProvenancesAsCurationActivities();
        // if the only curation activity is the default "in_progress," delete it,
        // but not if the package has no provenances of its own!
        if (curationActivities != null &&
            curationActivities.size() == 1 &&
            curationActivities.get(0).get("status").textValue().equals("In Progress") &&
            provenances != null &&
            provenances.size() > 0) {
            int unsubmittedID = curationActivities.get(0).get("id").intValue();
            try {
                URL url = new URL(dashServer + "/api/v2/curation_activity/" + unsubmittedID);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
                connection.setRequestMethod("DELETE");
                
                InputStream inputStream = connection.getInputStream();
            } catch (Exception e) {
                throw new RuntimeException("Unable to delete curation activity from Dash", e);
            }
        }
        
        // add provenances as curation activities
        log.debug("migrating provenances " + provenances.toString());
        for (int i=0; i<provenances.size(); i++) {
            int responseCode = addCurationActivity(pkg.getDataPackage(), provenances.get(i));
            if (responseCode < 200 || responseCode > 202) {
                log.fatal("Unable to send provenance to DASH, response: " + responseCode);
            }
        }
    }

    /**
       Given the (unencoded) DOI of a Dryad Data Package that has
       already been transferred to DASH, force the DASH dataset into
       "submitted" status.
     **/
    public int submitDashDataset(String doi) {
        int responseCode = 0;
        BufferedReader reader = null;

        String submissionsFinalize = ConfigurationManager.getProperty("dash.submissions.finalize");
        if(!submissionsFinalize.equals("true")) {
            log.info("Skipping finalization of " + doi + " due to dash.submissions.finalize = " + submissionsFinalize);
            return 200;
        }

        try {
            String encodedDOI = URLEncoder.encode(doi, "UTF-8");
            URL url = new URL(dashServer + "/api/v2/datasets/" + encodedDOI);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json-patch+json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setDoOutput(true);
            connection.setRequestMethod("PATCH");

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write("[{\"op\": \"replace\", \"path\": \"/versionStatus\", " +
                     "\"value\": \"submitted\"}]");
            wr.close();

            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String response = out.toString();
            
            responseCode = connection.getResponseCode();

            if(responseCode == 200 || responseCode == 201 || responseCode == 202) {
                log.debug("package submission successful");
            } else {
                log.fatal("Unable to update submission status of item in DASH, response: " + responseCode +
                        connection.getResponseMessage());
            }

            log.info("result object " + response);
        } catch (Exception e) {
            log.fatal("Unable to set item to \"submitted\" in DASH", e);
        }

        return responseCode;
    }

    public int addCurationActivity(DryadDataPackage dataPackage, String status, String note,
                                   String createdAt, String processKeyword) {
        return addCurationActivity(dataPackage, status, note, createdAt, processKeyword);
    }

    public int addCurationActivity(String dashDatasetDOI, String status, String note,
                                   String createdAt, String processKeyword) {
        ObjectNode node = mapper.createObjectNode();
        lastCurationStatus = status;
        node.put("status", status);
        node.put("note", note);
        if(createdAt != null) {
            node.put("created_at", createdAt);
        }
        if(processKeyword != null) {
            node.put("keywords", processKeyword);
        }
        return addCurationActivity(dashDatasetDOI, node);
    }
        
    private int addCurationActivity(DryadDataPackage dataPackage, JsonNode node) {
        return addCurationActivity(dataPackage.getVersionlessIdentifier(), node);
    }
    
    private int addCurationActivity(String dashDatasetDOI, JsonNode node) {
        int responseCode = 0;

        log.debug("starting addCurationActivity");
        try {
            lastCurationStatus = node.get("status").textValue();
            String dashJSON = mapper.writeValueAsString(node);
            log.debug("curation activity json is " + dashJSON);
            String encodedDOI = URLEncoder.encode(dashDatasetDOI, "UTF-8");
            URL url = new URL(dashServer + "/api/v2/datasets/" + encodedDOI + "/curation_activity");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(dashJSON);
            wr.close();
            
            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String response = out.toString();

            responseCode = connection.getResponseCode();
            
            if(responseCode == 200 || responseCode == 201 || responseCode == 202) {
                log.debug("curation activity added");
            } else {
                log.fatal("Unable to send curation activity to DASH, response: " + responseCode +
                          connection.getResponseMessage());
            }
            
            log.info("result object " + response);
        } catch (Exception e) {
            log.fatal("Unable to send curation_activity to DASH", e);
        }
        
        return responseCode;
    }

    public String getLatestVersionID(Package pkg) {
        String result = null;
        
        try {
            String encodedDOI = URLEncoder.encode(pkg.getDataPackage().getVersionlessIdentifier(), "UTF-8");
            URL url = new URL(dashServer + "/api/v2/datasets/" + encodedDOI + "/versions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);

            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter responseContent = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }
            String response = responseContent.toString();
            log.debug("got response content: " + response);
            JsonNode rootNode = mapper.readTree(response);
            JsonNode theVersions = rootNode.get("_embedded").get("stash:versions");
            result = theVersions.get(theVersions.size() - 1).get("_links").get("self").get("href").textValue();
        } catch (Exception e) {
            log.fatal("Unable to retrieve versionID for package", e);
        }

        return result;
    }
    
    public JsonNode getFiles(Package pkg) {
        String dashVersionID = getLatestVersionID(pkg);
        log.debug("getting files for version " + dashVersionID);
        try {
            URL url = new URL(dashServer + dashVersionID + "/files");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setRequestMethod("GET");

            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            JsonNode rootNode = mapper.readTree(out.toString());
            return rootNode.get("_embedded").get("stash:files");
        } catch (Exception e) {
            log.fatal("Unable to get file list from Dash", e);
        }
        return null;
    }

    public JsonNode getCurationActivity(Package pkg) {
        try {
            String encodedDOI = URLEncoder.encode(pkg.getDataPackage().getVersionlessIdentifier(), "UTF-8");
            URL url = new URL(dashServer + "/api/v2/datasets/" + encodedDOI + "/curation_activity");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setRequestMethod("GET");

            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            JsonNode rootNode = mapper.readTree(out.toString());
            if (rootNode.isArray()) {
                return rootNode;
            }
        } catch (Exception e) {
            log.fatal("Unable to get curation activity from Dash", e);
        }
        return null;
    }

    private int getDashUser(DryadDataPackage dryadDataPackage) {
        EPerson eperson = dryadDataPackage.getSubmitter();
        if (dryadDataPackage.getItem() != null) {
            try {
                URI uri = UriBuilder.fromUri(dashServer + "/api/v2/users/").queryParam("ePersonId", Integer.toString(eperson.getID())).build();
                log.debug("URL is " + uri.toURL());
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
                connection.setRequestMethod("GET");

                InputStream stream = connection.getErrorStream();
                if (stream == null) {
                    stream = connection.getInputStream();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line = null;
                StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }
                JsonNode rootNode = mapper.readTree(out.toString());
                JsonNode usersNode = rootNode.path("_embedded").path("stash:users");
                if (!usersNode.isMissingNode() && usersNode.isArray()) {
                    return usersNode.get(0).path("id").intValue();
                }

            } catch (Exception e) {
                log.fatal("Unable to get user from Dash", e);
            }
        } else {
            // if it's not a Dspace item, the submitter is already a dash user
            return eperson.getID();
        }
        return 0;
    }

    private JsonNode getInternalData(Package pkg) {
        BufferedReader reader = null;

        try {
            String encodedDOI = URLEncoder.encode(pkg.getDataPackage().getVersionlessIdentifier(), "UTF-8");
            URL url = new URL(dashServer + "/api/v2/datasets/" + encodedDOI + "/internal_data");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setRequestMethod("GET");

            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            JsonNode rootNode = mapper.readTree(out.toString());
            if (rootNode.isArray()) {
                return rootNode;
            }
        } catch (Exception e) {
            log.fatal("Unable to get internal data from Dash", e);
        }
        return null;
    }

    public String getPublicationISSN(Package pkg) {
        JsonNode resultNode = getInternalData(pkg);
        for (int i=0; i < resultNode.size(); i++) {
            if (resultNode.get(i).get("data_type").textValue().equals("publicationISSN")) {
                return resultNode.get(i).get("value").textValue();
            }
        }
        return "";
    }

    public int setPublicationISSN(Package pkg, String issn) {
        if(issn != null && issn.length() > 0) {
            return postInternalDatum(pkg, "set", "publicationISSN", issn);
        } else {
            return -1;
        }
    }

    public int setPublicationName(Package pkg, String name) {
        if(name != null && name.length() > 0) {
            return postInternalDatum(pkg, "set", "publicationName", name);
        } else {
            return -1;
        }
    }
    
    public int setManuscriptNumber(Package pkg, String msid) {
        return postInternalDatum(pkg, "set", "manuscriptNumber", msid);
    }

    public int setPublicationDOI(Package pkg, String doi) {
        return postInternalDatum(pkg, "set", "publicationDOI", doi);
    }

    public int setDansArchiveDate(Package pkg, String date) {
        return postInternalDatum(pkg, "set", "dansArchiveDate", date);
    }
    
    public int setDansEditIRI(Package pkg, String iri) {
        return postInternalDatum(pkg, "set", "dansEditIRI", iri);
    }

    public String getManuscriptNumber(Package pkg) {
        JsonNode resultNode = getInternalData(pkg);
        for (int i=0; i < resultNode.size(); i++) {
            if (resultNode.get(i).get("data_type").textValue().equals("manuscriptNumber")) {
                return resultNode.get(i).get("value").textValue();
            }
        }
        return "";
    }

    public int addFormerManuscriptNumber(Package pkg, String formerMSID) {
        return postInternalDatum(pkg, "add", "formerManuscriptNumber", formerMSID);
    }

    public List<String> getFormerManuscriptNumbers(Package pkg) {
        ArrayList<String> result = new ArrayList<>();
        JsonNode resultNode = getInternalData(pkg);
        for (int i=0; i < resultNode.size(); i++) {
            if (resultNode.get(i).get("data_type").textValue().equals("formerManuscriptNumber")) {
                result.add(resultNode.get(i).get("value").textValue());
            }
        }
        return result;
    }

    public int setPubmedID(Package pkg, String pubmedID) {
        return postInternalDatum(pkg, "set", "pubmedID", pubmedID);
    }
    
    public int addMismatchedDOI(Package pkg, String mismatchedDOI) {
        return postInternalDatum(pkg, "add", "mismatchedDOI", mismatchedDOI);
    }

    

    public List<String> getMismatchedDOIs(Package pkg) {
        ArrayList<String> result = new ArrayList<>();
        JsonNode resultNode = getInternalData(pkg);
        for (int i=0; i < resultNode.size(); i++) {
            if (resultNode.get(i).get("data_type").textValue().equals("mismatchedDOI")) {
                result.add(resultNode.get(i).get("value").textValue());
            }
        }
        return result;
    }

    public int addDuplicateItem(Package pkg, String duplicateItem) {
        Pattern itemIDPattern = Pattern.compile("\\d+");
        if (itemIDPattern.matcher(duplicateItem).matches()) {
            duplicateItem = pkg.getDataPackage().getVersionlessIdentifier();
        }
        return postInternalDatum(pkg, "add", "duplicateItem", duplicateItem);
    }

    public List<String> getDuplicateItems(Package pkg) {
        ArrayList<String> result = new ArrayList<>();
        JsonNode resultNode = getInternalData(pkg);
        for (int i=0; i < resultNode.size(); i++) {
            if (resultNode.get(i).get("data_type").textValue().equals("duplicateItem")) {
                result.add(resultNode.get(i).get("value").textValue());
            }
        }
        return result;
    }

    private int postInternalDatum(Package pkg, String requestType, String dataType, String value) {
        String dashJSON = "{\"data_type\": \"" + dataType + "\", \"value\": \"" + value + "\"}";
        int responseCode = 0;
        BufferedReader reader = null;

        try {
            String encodedDOI = URLEncoder.encode(pkg.getDataPackage().getVersionlessIdentifier(), "UTF-8");
            URL url = new URL(dashServer + "/api/v2/datasets/" + encodedDOI + "/" + requestType + "_internal_datum");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(dashJSON);
            wr.close();

            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String response = out.toString();

            responseCode = connection.getResponseCode();

            if (responseCode == 200 || responseCode == 201 || responseCode == 202) {
                log.debug("internal datum added");
            } else {
                log.fatal("Unable to send internal datum to DASH, response: " + responseCode +
                        connection.getResponseMessage());
            }

            log.info("result object " + response);
        } catch (Exception e) {
            log.fatal("Unable to send internal datum to DASH", e);
        }

        return responseCode;
    }

    public List<DryadDataPackage> findAllUnpublishedPackagesWithISSN(String issn) {
        String[] unpublishedStatuses = {
                "Unsubmitted",
                "Submitted",
                "Private for Peer Review",
                "Curation",
                "Author Action Required",
                "Embargoed"
        };

        ArrayList<DryadDataPackage> dryadDataPackages = new ArrayList<>();
        for (String status : unpublishedStatuses) {
            HashMap<String, String> queryPairs = new HashMap<>();
            queryPairs.put("publicationISSN", issn);
            queryPairs.put("curationStatus", status);
            dryadDataPackages.addAll(getPackagesWithQueryParameters(queryPairs));
        }
        return dryadDataPackages;
    }

    private List<DryadDataPackage> getPackagesWithQueryParameters(Map<String, String> queryPairs) {
        int responseCode = 0;
        BufferedReader reader = null;
        ArrayList<DryadDataPackage> dryadDataPackages = new ArrayList<>();

        try {
            URIBuilder ub = new URIBuilder(dashServer + "/api/v2/datasets/");
            for (String param : queryPairs.keySet()) {
                ub.addParameter(param, queryPairs.get(param));
            }
            URL url = new URL(ub.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            
            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }

            responseCode = connection.getResponseCode();

            if(responseCode == 200 || responseCode == 201 || responseCode == 202) {
                log.debug("DASH query successful");
            } else {
                log.fatal("Unable to get query from DASH, response: " + responseCode +
                        connection.getResponseMessage());
            }

            JsonNode rootNode = mapper.readTree(out.toString());
            JsonNode packagesNode = rootNode.path("_embedded").path("stash:datasets");
            if (!packagesNode.isMissingNode() && packagesNode.isArray()) {
                for (int i = 0; i < packagesNode.size(); i++) {
                    JsonNode packageNode = packagesNode.get(i);
                    dryadDataPackages.add(new DryadDataPackage(packageNode));
                }
            }
        } catch (Exception e) {
            log.fatal("Unable to get query from DASH", e);
        }

        return dryadDataPackages;
    }

    /**
       Command-line functionality for the DashService.
     **/
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
