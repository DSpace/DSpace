/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.xml.sax.SAXException;

public class ScriptDataCiteDeposit
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptDataCiteDeposit.class);

    private static Validator validator;

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

    private static String dbName = ConfigurationManager.getProperty("db.name");

    public static String TABLE_NAME_DOI2ITEM = "doi2item";

    private static String servicePOST = ConfigurationManager
            .getProperty("datacite.path.deposit");

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
        options.addOption(
                "a",
                "all",
                false,
                "Work on new inserted row, with last_modified equals to null or item update recently (see only-new parameter to disable recenty update)");
        options.addOption(
                "n",
                "only-new",
                false,
                "Used with -a parameter to enable only new inserted rows (with last_modified equals to null)");
        options.addOption(
                "s",
                "single",
                true,
                "Work on single item, , with last_modified equals to null or item update recently");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ScriptDataCiteDOIRegister \n", options);
            System.out
                    .println("\n\nUSAGE:\n ScriptDataCiteDOIRegister -a [-n]|-s <item_id>] \n");

            System.exit(0);
        }

        if (line.hasOption('s') && line.hasOption('a'))
        {
            System.out
                    .println("\n\nUSAGE:\n ScriptDataCiteDOIRegister -a [-n]|-s <item_id>] \n");
            System.out.println("Insert either a or s like parameters");
            log.error("Either a or s like parameters");
            System.exit(1);
        }

        if (!line.hasOption('s') && !line.hasOption('a'))
        {
            System.out
                    .println("\n\nUSAGE:\n ScriptDataCiteDOIRegister -a [-n]|-s <item_id>] \n");
            System.out.println("Please insert a [-n] or s parameters");
            log.error("Please insert a [-n] or s parameters");
            System.exit(1);
        }
        
        // create xsd validator
        SchemaFactory factory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        String uriSchema = ConfigurationManager
                .getProperty("crosswalk.datacite.schemaLocation");

        Source schemaSource = new StreamSource(uriSchema);

        Schema schema = null;
        try
        {
            schema = factory.newSchema(schemaSource);
        }
        catch (SAXException e2)
        {
            log.error(e2.getMessage(), e2);
        }
        // validator to validate source xml to xsd
        validator = schema.newValidator();

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

                boolean onlyNew = false;
                if(line.hasOption('n')) {
                    onlyNew = true;
                }
                int limit = 100;

                TableRowIterator rows = null;

                if ("oracle".equals(dbName))
                {
                    rows = DatabaseManager
                            .query(context,
                                    "select * from "
                                            + TABLE_NAME_DOI2ITEM
                                            + " d2i left join item i on d2i.item_id = i.item_id where (d2i.last_modified is null" + (onlyNew?")":(" OR d2i.last_modified < i.last_modified)"))
                                            + " AND ROWNUM <= " + limit);
                }
                else
                {
                    rows = DatabaseManager
                            .query(context,
                                    "select * from "
                                            + TABLE_NAME_DOI2ITEM
                                            + " d2i left join item i on d2i.item_id = i.item_id where (d2i.last_modified is null"+ (onlyNew?")":(" OR d2i.last_modified < i.last_modified)"))
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
                                                        + " d2i left join item i on d2i.item_id = i.item_id where d2i.last_modified is null"
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
                                                        + " d2i left join item i on d2i.item_id = i.item_id where d2i.last_modified is null"
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
                            String criteria = row.getStringColumn("criteria");
                            String doi = row.getStringColumn("identifier_doi");

                            try
                            {
                                result.putAll(depositToDataCite(context, item,
                                        criteria, doi));
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
                    }
                    context.commit();
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
                                            + " d2i left join item i on d2i.item_id = i.item_id where d2i.item_id = ? AND d2i.last_modified is null",
                                    id);

                    try
                    {
                        String criteria = row.getStringColumn("criteria");
                        String doi = row.getStringColumn("identifier_doi");
                        Item item = Item.find(context,
                                row.getIntColumn("item_id"));

                        result.putAll(depositToDataCite(context, item,
                                criteria, doi));
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
                            .println("\n\nUSAGE:\n ScriptDataCiteSender -a|-s <item_id>] \n");
                    System.out.println("Option a or s is needed");
                    log.error("Option a or s is needed");
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

    private static Map<Integer, String> depositToDataCite(Context context,
            Item target, String criteria, String doi)
            throws CrosswalkException, IOException, SQLException,
            AuthorizeException
    {

        final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) PluginManager
                .getNamedPlugin(StreamDisseminationCrosswalk.class, criteria);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamCrosswalkDefault.disseminate(context, target, out);

        File targetFile = File.createTempFile("doi_",
                DateFormatUtils.format(new Date(), "dd-MM-yyyy_HH_MM_SS_sss"));
        FileOutputStream fos = new FileOutputStream(targetFile);
        out.writeTo(fos);
        fos.close();

        PostMethod post = null;
        Map<Integer, String> result = new HashMap<Integer, String>();

        int responseCode = 0;
        String urlDeposited = "";
        try
        {

            // validate xml
            validator.validate(new StreamSource(targetFile));

            // prepare the post method
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

            post.getParams().setContentCharset("UTF8");
            post.setRequestEntity(new FileRequestEntity(targetFile,
                    "application/xml;charset=UTF-8"));
            post.setDoAuthentication(true);

            HostConfiguration nss = new HostConfiguration();
            nss.setHost(HOST);

            responseCode = getHttpClient().executeMethod(nss, post);
            Header location = post.getResponseHeader("Location");
            urlDeposited = location != null ? location.getValue() : null;

            result.put(target.getID(),
                    responseCode + " - " + post.getResponseBodyAsString()
                            + "\nLocation:" + urlDeposited);

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
                target.clearMetadata("dc", "utils", "processdoi", Item.ANY);
                target.addMetadata("dc", "utils", "processdoi", null,
                        "datacite");

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
                                        + " SET LAST_MODIFIED = ?, RESPONSE_CODE = ?, NOTE = ?, FILENAME = ? WHERE ITEM_ID = ?",
                                new java.sql.Timestamp(new Date().getTime()),
                                responseCode, result.get(target.getID()),
                                urlDeposited, target.getID());

            }
            else
            {
                DatabaseManager.updateQuery(context, "UPDATE "
                        + TABLE_NAME_DOI2ITEM
                        + " SET RESPONSE_CODE = ?, NOTE = ? WHERE ITEM_ID = ?",
                        responseCode,
                        responseCode + "-" + result.get(target.getID()),
                        target.getID());
            }

            context.commit();
        }

        return result;
    }

}
