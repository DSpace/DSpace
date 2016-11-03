/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.batch;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class ScriptDataCiteDOIActivate
{

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(ScriptDataCiteDOIActivate.class);

    private static final int PLACEHOLDER_SENDEDTODATACITE_SUCCESSFULLY = 999;

    private static final String HOST = ConfigurationManager
            .getProperty("datacite.host");

    private static final String PROTOCOL = ConfigurationManager
            .getProperty("datacite.protocol");

    private static final String DATACITE_ENTRYMODE = ConfigurationManager
            .getProperty("datacite.mode");

    private static final String PASSWORD = ConfigurationManager
            .getProperty("datacite.password");

    private static final String USERNAME = ConfigurationManager
            .getProperty("datacite.username");

    public static String TABLE_NAME_DOI2ITEM = "doi2item";

    private static String dbName = ConfigurationManager.getProperty("db.name");

    private static String servicePOST = ConfigurationManager
            .getProperty("datacite.path.register");

    private static AuthScope m_authScope;

    private static UsernamePasswordCredentials m_creds;

    private static URL url;

    public static void main(String[] args) throws ParseException,
            MalformedURLException
    {
        log.info("#### START Script datacite sender: -----" + new Date()
                + " ----- ####");
        Map<Integer, String> result = new HashMap<Integer, String>();

        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("a", "all", false,
                "Work on new inserted row, with placeholder metadata");
        options.addOption("s", "single", true, "Work on single item, , with");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ScriptDataCiteDOIActivate \n", options);
            System.out
                    .println("\n\nUSAGE:\n ScriptDataCiteDOIActivate -a|-s <item_id>] \n");

            System.exit(0);
        }

        if (line.hasOption('s') && line.hasOption('a'))
        {
            System.out
                    .println("\n\nUSAGE:\n ScriptDataCiteDOIActivate -a|-s <item_id>] \n");
            System.out.println("Insert either a or s like parameters");
            log.error("Either a or s like parameters");
            System.exit(1);
        }

        m_creds = new UsernamePasswordCredentials(USERNAME, PASSWORD);
        url = new URL(PROTOCOL + HOST);
        m_authScope = new AuthScope(url.getHost(), AuthScope.ANY_PORT,
                AuthScope.ANY_REALM);

        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
            if (line.hasOption('a'))
            {

                int limit = 100;

                TableRowIterator rows = null;

                if ("oracle".equals(dbName))
                {
                    rows = DatabaseManager.query(context, "select * from "
                            + TABLE_NAME_DOI2ITEM
                            + " d2i where d2i.response_code = '201'"
                            + " AND ROWNUM <= " + limit);
                }
                else
                {
                    rows = DatabaseManager.query(context, "select * from "
                            + TABLE_NAME_DOI2ITEM
                            + " d2i where d2i.response_code = '201'"
                            + " LIMIT " + limit);
                }
                int offset = 0;
                int count = 0;
                try
                {
                    while (rows.hasNext() || count == limit)
                    {
                        if (offset > 0)
                        {
                            if ("oracle".equals(dbName))
                            {
                                rows = DatabaseManager
                                        .query(context,
                                                "select * from "
                                                        + TABLE_NAME_DOI2ITEM
                                                        + " d2i where d2i.response_code = '201'"
                                                        + " AND ROWNUM > "
                                                        + limit
                                                        + " AND ROWNUM <= "
                                                        + (offset + limit));
                            }
                            else
                            {
                                rows = DatabaseManager
                                        .query(context,
                                                "select * from "
                                                        + TABLE_NAME_DOI2ITEM
                                                        + " d2i where d2i.response_code = '201'"
                                                        + " LIMIT " + limit
                                                        + " OFFSET " + offset);
                            }
                        }
                        offset = limit + offset;

                        count = 0;
                        while (rows.hasNext())
                        {
                            count++;
                            TableRow row = rows.next();
                            Item item = Item.find(context,
                                    row.getIntColumn("item_id"));
                            String doi = row.getStringColumn("identifier_doi");

                            try
                            {
                                result.putAll(activateDOIDataCite(context,
                                        item, doi));
                            }
                            catch (IOException e)
                            {
                                log.error("FOR item: " + item.getID()
                                        + " ERRORMESSAGE: " + e.getMessage(), e);
                            }
                            catch (AuthorizeException e)
                            {
                                log.error("FOR item: " + item.getID()
                                        + " ERRORMESSAGE: " + e.getMessage(), e);
                            }
                            catch (CrosswalkException e)
                            {
                                log.error("FOR item: " + item.getID()
                                        + " ERRORMESSAGE: " + e.getMessage(), e);
                            }
                        }
                        context.commit();
                    }
                }
                finally
                {
                    rows.close();
                }
            }
            else
            {
                if (line.hasOption('s'))
                {
                    Integer id = Integer.parseInt(line.getOptionValue("s"));
                    TableRow row = DatabaseManager
                            .querySingle(
                                    context,
                                    "SELECT * FROM "
                                            + TABLE_NAME_DOI2ITEM
                                            + " d2i where d2i.response_code = '201' and d2i.item_id = ?",
                                    id);

                    try
                    {
                        String doi = row.getStringColumn("identifier_doi");
                        Item item = Item.find(context,
                                row.getIntColumn("item_id"));

                        result.putAll(activateDOIDataCite(context, item, doi));
                    }
                    catch (IOException e)
                    {
                        log.error(
                                "FOR item: " + id + " ERRORMESSAGE: "
                                        + e.getMessage(), e);
                    }
                    catch (AuthorizeException e)
                    {
                        log.error(
                                "FOR item: " + id + " ERRORMESSAGE: "
                                        + e.getMessage(), e);
                    }
                    catch (CrosswalkException e)
                    {
                        log.error(
                                "FOR item: " + id + " ERRORMESSAGE: "
                                        + e.getMessage(), e);
                    }
                }
                else
                {
                    System.out
                            .println("\n\nUSAGE:\n ScriptDataCiteDOIActivate -a|-s <item_id>] \n");
                    System.out.println("Option n or s is needed");
                    log.error("Option n or s is needed");
                    System.exit(1);
                }

            }
        }
        catch (SQLException e1)
        {
            log.error(e1.getMessage(), e1);
            if (context.isValid())
            {
                context.abort();
            }
        }

        log.info("#### Import details ####");

        for (Integer key : result.keySet())
        {
            log.info("ITEM: " + key + " RESULT: " + result.get(key));
        }

        log.info("#### ########################### ####");
        log.info("#### END: -----" + new Date() + " ----- ####");

        System.exit(0);
    }

    public static HttpClient getHttpClient()
    {

        HttpClient client = new HttpClient();
        client.getState().setCredentials(m_authScope, m_creds);
        client.getParams().setAuthenticationPreemptive(true);
        return client;
    }

    private static Map<Integer, String> activateDOIDataCite(Context context,
            Item target, String doi) throws CrosswalkException, IOException,
            SQLException, AuthorizeException
    {
        PostMethod post = null;
        Map<Integer, String> result = new HashMap<Integer, String>();

        int responseCode = 0;
        try
        {
            // prepare the post method
            boolean useHandleServer = ConfigurationManager.getBooleanProperty(
                    "datacite.use.url.handleserver", false);
            String url;
            if (useHandleServer)
            {
                url = target.getMetadata("dc", "identifier", "uri", Item.ANY)[0].value;
            }
            else
            {
                url = ConfigurationManager
                        .getProperty("datacite.allowed.domain")
                        + "/handle/"
                        + target.getHandle();
            }

            post = new PostMethod(servicePOST)
            {
                public boolean getFollowRedirects()
                {
                    return true;
                };
            };

            if (DATACITE_ENTRYMODE.equals("test"))
            {
                post.setQueryString(new NameValuePair[] { new NameValuePair(
                        "testMode", "true") });
            }

            StringRequestEntity requestEntity = new StringRequestEntity("doi="
                    + doi + "\nurl=" + url, "text/plain", "UTF-8");
            post.setRequestEntity(requestEntity);
            post.setDoAuthentication(true);

            HostConfiguration nss = new HostConfiguration();
            nss.setHost(HOST);

            responseCode = getHttpClient().executeMethod(nss, post);
            result.put(target.getID(),
                    responseCode + " - " + post.getResponseBodyAsString());

        }
        catch (Exception e)
        {
            result.put(target.getID(), responseCode + " - " + e.getMessage());
            log.error(
                    "FOR item: " + target.getID() + " ERRORMESSAGE: "
                            + e.getMessage(), e);
        }
        finally
        {

            if (post != null)
            {
                post.releaseConnection();
            }

            log.info("FOR item: " + target.getID() + " -> RESPONSECODE: "
                    + responseCode + " MESSAGE:" + result.get(target.getID()));
            if (responseCode == 201)
            {
                Metadatum[] values = target.getMetadata("dc", "identifier",
                        "doi", null);
                boolean found = false;
                for (Metadatum dcval : values)
                {
                    if (dcval.equals(doi))
                    {
                        found = true;
                    }
                }
                if (!found)
                {
                    target.addMetadata("dc", "identifier", "doi", null, doi);
                }
                target.clearMetadata("dc", "utils", "processdoi", Item.ANY);

                try
                {
                    target.update();
                    context.addEvent(new Event(Event.UPDATE_FORCE,
                            Constants.ITEM, target.getID(), target.getHandle()));
                    context.commit();
                }
                catch (AuthorizeException e)
                {
                    log.error("FOR item: " + target.getID() + " ERRORMESSAGE: "
                            + e.getMessage(), e);
                }

                DatabaseManager
                        .updateQuery(
                                context,
                                "UPDATE "
                                        + TABLE_NAME_DOI2ITEM
                                        + " SET LAST_MODIFIED = ?, RESPONSE_CODE = ?, NOTE = ? WHERE ITEM_ID = ?",
                                new java.sql.Timestamp(new Date().getTime()),
                                PLACEHOLDER_SENDEDTODATACITE_SUCCESSFULLY,
                                result.get(target.getID()), target.getID());

            }
            else
            {
                DatabaseManager.updateQuery(context, "UPDATE "
                        + TABLE_NAME_DOI2ITEM
                        + " SET NOTE = ? WHERE ITEM_ID = ?",
                        result.get(target.getID()), target.getID());
            }

            context.commit();
        }

        return result;
    }
}
