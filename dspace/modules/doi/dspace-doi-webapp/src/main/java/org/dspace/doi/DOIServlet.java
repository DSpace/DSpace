package org.dspace.doi;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class DOIServlet extends HttpServlet {

    private static final String CROSSREF_URL = "http://api.labs.crossref.org/";

    private Minter myMinter;

    @Override
    protected void doGet(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {

        String item = aRequest.getParameter("item");
        String redirect = aRequest.getParameter("redirect");
        String lookup = aRequest.getParameter("lookup");
        String mdLookup = aRequest.getParameter("mdLookup");
        String remove = aRequest.getParameter("remove");

        // TODO: Check it !!!
        String url = aRequest.getParameter("targeturl");
        String lookupURL = aRequest.getParameter("lookupbyurl");
        String lookupall = aRequest.getParameter("lookupall");

        boolean register = aRequest.getParameterMap().containsKey("register");

        String aDOI = aRequest.getParameter("doi");

        try {
            // want to limit minting to the localhost
            if (item != null && !item.equals("")) {
                mint(aResponse, item, register, aDOI);
            }
            else if (redirect != null && !redirect.equals("")) {
                redirect(aResponse, redirect);
            }
            else if (lookupURL != null && !lookupURL.equals("")) {
                lookupByURL(aResponse, lookupURL);
            }
            else if (lookup != null && !lookup.equals("")) {
                lookup(aResponse, lookup);
            }
            else if (url != null && !url.equals("")) {
                targetURL(aResponse, url);
            }
            else if (remove != null && !remove.equals("")) {
                remove(aResponse, remove);
            }
            else if (mdLookup != null && !mdLookup.equals("")) {
                mdLookup(aResponse, mdLookup);
            }
            else if (lookupall != null && !lookupall.equals("")) {
                lookupAll(aResponse);
            }
        }
        catch (RuntimeException details) {
            log("Fatal error in servlet", details);
            aResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "RuntimeException: " + details.getMessage());
        }
    }

    private void mdLookup(HttpServletResponse aResponse, String mdLookup) {
        try {
            DOI doi = new DOI(mdLookup.trim());
            PrintWriter writer = aResponse.getWriter();
            String crURL = CROSSREF_URL + doi.toString() + ".json";

            writer.println(checkDOI(crURL));
            writer.close();
        }
        catch (MalformedURLException details) {
            log(details.getMessage(), details);
        }
        catch (IOException details) {
            log(details.getMessage(), details);
        }
        catch (DOIFormatException details) {
            log(details.getMessage(), details);
            // going to not throw exception since this isn't intended
            // to be used by anything but our JavaScript
        }
    }

    private void remove(HttpServletResponse aResponse, String remove) throws IOException {
        PrintWriter writer = null;
        try {
            if (remove != null && !remove.equals("")) {
                DOI doi = myMinter.getKnownDOI(remove);

                if (myMinter.remove(doi)) {
                    writer = aResponse.getWriter();
                    writer.println(doi + " Removed.");                    
                } else {
                    aResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "DOI or supplied Item URL not found.");
                }
            } else {
                aResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "DOI or supplied Item URL not found.");
            }
        } finally {
            if (writer != null) writer.close();
        }
    }

    private void targetURL(HttpServletResponse aResponse, String lookup) throws IOException {
        DOI doi = myMinter.getKnownDOI(lookup);
        PrintWriter writer = null;
        try {
            if (doi == null) {
                aResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown DOI");
            } else {
                writer = aResponse.getWriter();
                writer.println(doi.getTargetURL());
            }
        }
        finally {
            if (writer != null) writer.close();
        }
    }

    private void lookup(HttpServletResponse aResponse, String lookup) throws IOException {
        DOI doi = myMinter.getKnownDOI(lookup);

        if (doi == null) {
            aResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown DOI");
        } else {
            PrintWriter writer = aResponse.getWriter();
            writer.println(doi.getTargetURL().toString());
            writer.close();
        }
    }


    private void lookupByURL(HttpServletResponse aResponse, String lookupURL) throws IOException {

        Set<DOI> dois = myMinter.getKnownDOIByURL(lookupURL);

        if (dois == null || dois.size()==0) {
            aResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown DOI for URL: " + lookupURL);
        }
        else {
            PrintWriter writer = aResponse.getWriter();
            for(DOI d: dois) writer.println(d.toString() + " ");

            writer.close();
        }
    }

    private void lookupAll(HttpServletResponse aResponse) throws IOException {
        PrintWriter writer = aResponse.getWriter();
        Set<DOI> dois = myMinter.getALlKnownDOI();
        if (dois == null || dois.size()==0) {
            writer.println("Database is empty");
        }
        else{
            for(DOI d: dois) writer.println(d.toString() + printBlank(d.toString()) + d.getTargetURL().toString());
        }
        writer.close();        
    }

    private String printBlank(String str){
        String blank="";
        for(int i=str.length(); i < 50; i++) blank+=" ";

        return blank;
    }

    private void redirect(HttpServletResponse aResponse, String redirect) throws IOException {
        DOI doi = myMinter.getKnownDOI(redirect);
        URL target;

        if (doi == null) {
            aResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            target = doi.getTargetURL();
            aResponse.sendRedirect(target.getPath());
        }
    }

    private void mint(HttpServletResponse aResponse, String url, boolean register, String aDOI) throws IOException {
        if (aDOI != null && !aDOI.equals("")) {

            // FB: Changed behavior here to
            DOI doi = myMinter.mintDOI(aDOI, url);

            if (register) {
                // Need this instead of a DOI constructor b/c
                // we also need the target associated with this DOI
                myMinter.register(myMinter.getKnownDOI(doi.toString()));
            }

            PrintWriter writer = aResponse.getWriter();
            writer.println(doi);
            writer.close();
        } else {
            aResponse.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "DOI or supplied Item URL not provided.");

            // Were you trying to create a data file DOI before the
            // data package DOI?
        }
    }

    @Override
    public void init() throws ServletException {
        ServletContext context = this.getServletContext();
        String configFileName = context.getInitParameter("dspace.config");

        myMinter = new Minter(new File(configFileName));
    }

    private String checkDOI(String aEndpoint) throws IOException {
        URL url = new URL(aEndpoint);
        URLConnection conx = url.openConnection();
        InputStream inStream = conx.getInputStream();
        InputStreamReader isReader = new InputStreamReader(inStream);
        BufferedReader reader = new BufferedReader(isReader);
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }

        reader.close();
        return stringBuilder.toString();
    }
}
