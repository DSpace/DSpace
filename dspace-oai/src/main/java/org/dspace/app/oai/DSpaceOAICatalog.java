/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.oai;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;
import org.dspace.search.Harvest;
import org.dspace.search.HarvestedItemInfo;
import org.dspace.eperson.Group;

import ORG.oclc.oai.server.catalog.AbstractCatalog;
import ORG.oclc.oai.server.verb.BadArgumentException;
import ORG.oclc.oai.server.verb.BadResumptionTokenException;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.IdDoesNotExistException;
import ORG.oclc.oai.server.verb.NoItemsMatchException;
import ORG.oclc.oai.server.verb.NoMetadataFormatsException;
import ORG.oclc.oai.server.verb.NoSetHierarchyException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;

/**
 * This is class extends OAICat's AbstractCatalog base class to allow metadata
 * harvesting of the metadata in DSpace via OAI-PMH 2.0.
 * 
 * FIXME: Some CNRI Handle-specific stuff in here. Anyone wanting to use
 * something else will need to update this code too. Sorry about that.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class DSpaceOAICatalog extends AbstractCatalog
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(DSpaceOAICatalog.class);

    /** Prefix that all our OAI identifiers have */
    public static final String OAI_ID_PREFIX = "oai:" + ConfigurationManager.getProperty("dspace.hostname") + ":";

    /** Maximum number of records returned by one request */
    private final int MAX_RECORDS = ConfigurationManager.getIntProperty("oai","response.max-records", 100);

    public DSpaceOAICatalog(Properties properties)
    {
        // Don't need to do anything
    }

    /**
     * Retrieve a list of schemaLocation values associated with the specified
     * identifier.
     * 
     * @param identifier
     *            the OAI identifier
     * @return a Vector containing schemaLocation Strings
     * @exception OAIInternalServerError
     *                signals an http status code 500 problem
     * @exception IdDoesNotExistException
     *                the specified identifier can't be found
     * @exception NoMetadataFormatsException
     *                the specified identifier was found but the item is flagged
     *                as deleted and thus no schemaLocations (i.e.
     *                metadataFormats) can be produced.
     */
    public Vector getSchemaLocations(String identifier)
            throws OAIInternalServerError, IdDoesNotExistException,
            NoMetadataFormatsException
    {
        log.info(LogManager.getHeader(null, "oai_request",
                "verb=getSchemaLocations,identifier="
                        + ((identifier == null) ? "null" : identifier)));

        HarvestedItemInfo itemInfo = null;
        Context context = null;

        // Get the item from the DB
        try
        {
            context = new Context();

            // Valid identifiers all have prefix "oai:hostname:"
            if (identifier != null && identifier.startsWith(OAI_ID_PREFIX))
            {
                itemInfo = Harvest.getSingle(context, identifier
                        .substring(OAI_ID_PREFIX.length()), // Strip prefix to
                                                            // get raw handle
                        false);
            }
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context, "database_error", ""), se);

            // Stack trace loss as OAI Exception does not support cause
            throw new OAIInternalServerError(se.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }

        if (itemInfo == null)
        {
            throw new IdDoesNotExistException(identifier);
        }
        else
        {
            if (itemInfo.withdrawn)
            {
                throw new NoMetadataFormatsException();
            }
            else
            {
                return getRecordFactory().getSchemaLocations(itemInfo);
            }
        }
    }

    /**
     * Retrieve a list of identifiers that satisfy the specified criteria
     * 
     * @param from
     *            beginning date using the proper granularity
     * @param until
     *            ending date using the proper granularity
     * @param set
     *            the set name or null if no such limit is requested
     * @param metadataPrefix
     *            the OAI metadataPrefix or null if no such limit is requested
     * @return a Map object containing entries for "headers" and "identifiers"
     *         Iterators (both containing Strings) as well as an optional
     *         "resumptionMap" Map. It may seem strange for the map to include
     *         both "headers" and "identifiers" since the identifiers can be
     *         obtained from the headers. This may be true, but
     *         AbstractCatalog.listRecords() can operate quicker if it doesn't
     *         need to parse identifiers from the XML headers itself. Better
     *         still, do like I do below and override
     *         AbstractCatalog.listRecords(). AbstractCatalog.listRecords() is
     *         relatively inefficient because given the list of identifiers, it
     *         must call getRecord() individually for each as it constructs its
     *         response. It's much more efficient to construct the entire
     *         response in one fell swoop by overriding listRecords() as I've
     *         done here.
     * @exception OAIInternalServerError
     *                signals an http status code 500 problem
     * @exception NoSetHierarchyException
     *                the repository doesn't support sets.
     * @exception CannotDisseminateFormatException
     *                the metadata format specified is not supported by your
     *                repository.
     */
    public Map listIdentifiers(String from, String until, String set,
            String metadataPrefix) throws OAIInternalServerError,
            NoSetHierarchyException, NoItemsMatchException,
            CannotDisseminateFormatException, BadArgumentException
    {
        log
                .info(LogManager.getHeader(null, "oai_request",
                        "verb=listIdentifiers,from="
                                + ((from == null) ? "null" : from)
                                + ",until="
                                + ((until == null) ? "null" : until)
                                + ",set="
                                + ((set == null) ? "null" : set)
                                + ",metadataPrefix="
                                + ((metadataPrefix == null) ? "null"
                                        : metadataPrefix)));

        // We can produce oai_dc and simple DC for all items, so just return IDs
        Context context = null;

        // Lists to put results in
        List<String> headers = new LinkedList<String>();
        List<String> identifiers = new LinkedList<String>();

        try
        {
            context = new Context();

            // Get the relevant OAIItemInfo objects to make headers
            DSpaceObject scope = resolveSet(context, set);
            boolean includeAll = ConfigurationManager.getBooleanProperty("oai", "harvest.includerestricted.oai", true);
            // Warning: In large repositories, setting harvest.includerestricted.oai to false may cause
            // performance problems as all items will need to have their authorization permissions checked,
            // but because we haven't implemented resumption tokens in ListIdentifiers, ALL items will
            // need checking whenever a ListIdentifiers request is made.
            List<HarvestedItemInfo> itemInfos = Harvest.harvest(context, scope, from, until, 0, 0, // Everything
                                                                                // for
                                                                                // now
                    !includeAll, true, true, includeAll);

            // No Item objects, but we need to know collections they're in and
            // withdrawn items
            if (itemInfos.size() == 0)
            {
                log.info(LogManager.getHeader(null, "oai_error",
                        "no_items_match"));
                throw new NoItemsMatchException();
            }

            // Build up lists of headers and identifiers
            Iterator<HarvestedItemInfo> i = itemInfos.iterator();

            while (i.hasNext())
            {
                HarvestedItemInfo itemInfo = i.next();

                String[] header = getRecordFactory().createHeader(itemInfo);

                headers.add(header[0]);
                identifiers.add(header[1]);
            }
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context, "database_error", ""), se);

            // Stack trace loss as OAI Exception does not support cause
            throw new OAIInternalServerError(se.toString());
        }
        catch (ParseException pe)
        {
            // Stack trace loss as OAI Exception does not support cause
            throw new OAIInternalServerError(pe.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }

        // Put results in form needed to return
        Map<String, Iterator<String>> results = new HashMap<String, Iterator<String>>();
        results.put("headers", headers.iterator());
        results.put("identifiers", identifiers.iterator());

        return results;
    }

    /**
     * Retrieve the next set of identifiers associated with the resumptionToken
     * 
     * @param resumptionToken
     *            implementation-dependent format taken from the previous
     *            listIdentifiers() Map result.
     * @return a Map object containing entries for "headers" and "identifiers"
     *         Iterators (both containing Strings) as well as an optional
     *         "resumptionMap" Map.
     * @exception BadResumptionTokenException
     *                the value of the resumptionToken is invalid or expired.
     * @exception OAIInternalServerError
     *                signals an http status code 500 problem
     */
    public Map listIdentifiers(String resumptionToken)
            throws BadResumptionTokenException, OAIInternalServerError
    {
        // Resumption tokens not yet supported
        throw new BadResumptionTokenException();
    }

    /**
     * Retrieve the specified metadata for the specified identifier
     * 
     * @param identifier
     *            the OAI identifier
     * @param metadataPrefix
     *            the OAI metadataPrefix
     * @return the <record/>portion of the XML response.
     * @exception OAIInternalServerError
     *                signals an http status code 500 problem
     * @exception CannotDisseminateFormatException
     *                the metadataPrefix is not supported by the item.
     * @exception IdDoesNotExistException
     *                the identifier wasn't found
     */
    public String getRecord(String identifier, String metadataPrefix)
            throws OAIInternalServerError, CannotDisseminateFormatException,
            IdDoesNotExistException
    {
        log
                .info(LogManager.getHeader(null, "oai_request",
                        "verb=getRecord,identifier="
                                + ((identifier == null) ? "null" : identifier)
                                + ",metadataPrefix="
                                + ((metadataPrefix == null) ? "null"
                                        : metadataPrefix)));

        Context context = null;
        String record = null;
        HarvestedItemInfo itemInfo = null;
        
        // First get the item from the DB
        try
        {
            // Valid IDs start with oai:hostname:
            if (identifier != null && identifier.startsWith(OAI_ID_PREFIX))
            {
                context = new Context();

                /*
                 * Try and get the item. the .substring() is to strip the
                 * oai:(hostname): prefix to get the raw handle
                 */
                itemInfo = Harvest.getSingle(context, identifier
                        .substring(OAI_ID_PREFIX.length()), true);
            }

            if (itemInfo == null)
            {
                log.info(LogManager.getHeader(null, "oai_error",
                        "id_does_not_exist"));
                throw new IdDoesNotExistException(identifier);
            }
            
            boolean includeAll = ConfigurationManager.getBooleanProperty("oai", "harvest.includerestricted.oai", true);

            if (!includeAll)
            {
                Group[] authorizedGroups = AuthorizeManager.getAuthorizedGroups(context, itemInfo.item, Constants.READ);
                boolean authorized = false;
                for (int i = 0; i < authorizedGroups.length; i++)
                {
                    if ((authorizedGroups[i].getID() == 0) && (!authorized))
                    {
                        authorized = true;
                    }
                }

                if (!authorized)
                {
                    log.info(LogManager.getHeader(null, "oai_error",
                            "id_not_accessible"));
                    throw new IdDoesNotExistException(identifier);
                }
            }

            String schemaURL = getCrosswalks().getSchemaURL(metadataPrefix);

            if (schemaURL == null)
            {
                log.info(LogManager.getHeader(null, "oai_error",
                        "cannot_disseminate_format"));
                throw new CannotDisseminateFormatException(metadataPrefix);
            }

            record = getRecordFactory().create(itemInfo, schemaURL,
                    metadataPrefix);
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context, "database_error", ""), se);

            // Stack trace loss as OAI Exception does not support cause
            throw new OAIInternalServerError(se.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }

        return record;
    }

    /**
     * Retrieve a list of records that satisfy the specified criteria. Note,
     * though, that unlike the other OAI verb type methods implemented here,
     * both of the listRecords methods are already implemented in
     * AbstractCatalog rather than abstracted. This is because it is possible to
     * implement ListRecords as a combination of ListIdentifiers and GetRecord
     * combinations. Nevertheless, I suggest that you override both the
     * AbstractCatalog.listRecords methods here since it will probably improve
     * the performance if you create the response in one fell swoop rather than
     * construct it one GetRecord at a time.
     * 
     * @param from
     *            beginning date using the proper granularity
     * @param until
     *            ending date using the proper granularity
     * @param set
     *            the set name or null if no such limit is requested
     * @param metadataPrefix
     *            the OAI metadataPrefix or null if no such limit is requested
     * @return a Map object containing entries for a "records" Iterator object
     *         (containing XML <record/>Strings) and an optional
     *         "resumptionMap" Map.
     * @exception OAIInternalServerError
     *                signals an http status code 500 problem
     * @exception NoSetHierarchyException
     *                The repository doesn't support sets.
     * @exception CannotDisseminateFormatException
     *                the metadataPrefix isn't supported by the item.
     */
    public Map listRecords(String from, String until, String set,
            String metadataPrefix) throws OAIInternalServerError,
            NoSetHierarchyException, CannotDisseminateFormatException,
            NoItemsMatchException, BadArgumentException
    {
        log
                .info(LogManager.getHeader(null, "oai_request",
                        "verb=listRecords,from="
                                + ((from == null) ? "null" : from)
                                + ",until="
                                + ((until == null) ? "null" : until)
                                + ",set="
                                + ((set == null) ? "null" : set)
                                + ",metadataPrefix="
                                + ((metadataPrefix == null) ? "null"
                                        : metadataPrefix)));

        Map m = doRecordHarvest(from, until, set, metadataPrefix, 0);

        // Null means bad metadata prefix was bad
        if (m == null)
        {
            log.info(LogManager.getHeader(null, "oai_error",
                    "cannot_disseminate_format"));
            throw new CannotDisseminateFormatException(metadataPrefix);
        }

        // If there were zero results, return the appropriate error
        Iterator i = (Iterator) m.get("records");

        if ((i == null) || !i.hasNext())
        {
            log.info(LogManager.getHeader(null, "oai_error", "no_items_match"));
            throw new NoItemsMatchException();
        }

        return m;
    }

    /**
     * Retrieve the next set of records associated with the resumptionToken
     * 
     * @param resumptionToken
     *            implementation-dependent format taken from the previous
     *            listRecords() Map result.
     * @return a Map object containing entries for "headers" and "identifiers"
     *         Iterators (both containing Strings) as well as an optional
     *         "resumptionMap" Map.
     * @exception OAIInternalServerError
     *                signals an http status code 500 problem
     * @exception BadResumptionTokenException
     *                the value of the resumptionToken argument is invalid or
     *                expired.
     */
    public Map listRecords(String resumptionToken)
            throws BadResumptionTokenException, OAIInternalServerError
    {
        log.info(LogManager.getHeader(null, "oai_request",
                "verb=listRecords,resumptionToken=" + resumptionToken));

        /*
         * FIXME: This may return zero records if the previous harvest returned
         * a number of records that's an exact multiple of MAX_RECORDS. I hope
         * that's OK.
         */
        Object[] params = decodeResumptionToken(resumptionToken);
        Integer offset = (Integer) params[4];

        Map m = null;

        /*
         * We catch BadArgumentExceptions here, because doRecordHarvest() throws
         * BadArgumentExcpetions when the set spec is bad. set spec bad == bad
         * resumption token.
         */
        try
        {
            m = doRecordHarvest((String) params[0], (String) params[1],
                    (String) params[2], (String) params[3], offset.intValue());
        }
        catch (BadArgumentException bae)
        {
            m = null;
        }

        // null result means a problem -> bad resumption token
        if (m == null)
        {
            log.info(LogManager.getHeader(null, "oai_error",
                    "bad_resumption_token"));
            throw new BadResumptionTokenException();
        }

        return m;
    }

    /**
     * Method to do the actual harvest of records
     * 
     * @param from
     *            OAI 'from' parameter
     * @param until
     *            OAI 'until' parameter
     * @param set
     *            OAI 'set' parameter
     * @param metadataPrefix
     *            OAI 'metadataPrefix' parameter
     * @param offset
     *            where to start this harvest
     * 
     * @return the Map for listRecords to return, or null if the metadataPrefix
     *         is invalid
     */
    private Map doRecordHarvest(String from, String until, String set,
            String metadataPrefix, int offset) throws OAIInternalServerError,
            BadArgumentException
    {
        Context context = null;
        String schemaURL = getCrosswalks().getSchemaURL(metadataPrefix);
        Map results = new HashMap();

        if (schemaURL == null)
        {
            return null;
        }

        // List to put results in
        List<String> records = new LinkedList<String>();

        try
        {
            context = new Context();

            // Get the relevant HarvestedItemInfo objects to make headers
            DSpaceObject scope = resolveSet(context, set);
            boolean includeAll = ConfigurationManager.getBooleanProperty("oai", "harvest.includerestricted.oai", true);
            List<HarvestedItemInfo> itemInfos = Harvest.harvest(context, scope, from, until,
                    offset, MAX_RECORDS, // Limit amount returned from one
                                         // request
                    true, true, true, includeAll); // Need items, containers + withdrawals

            // Build list of XML records from item info objects
            int ignore = 0;
            for (HarvestedItemInfo itemInfo : itemInfos)
            {
                try
                {
                    String recordXML = getRecordFactory().create(itemInfo, schemaURL, metadataPrefix);
                    records.add(recordXML);
                }
                catch (CannotDisseminateFormatException cdfe)
                {
                    /*
                     * FIXME: I've a feeling a
                     * "CannotDisseminateFormatException" should be discarded
                     * here - it's OK if some records in the requested date
                     * range don't have the requested metadata format available.
                     * I'll just log it for now.
                     */
                    ignore++;
                    if (log.isDebugEnabled())
                    {
                        log.debug(LogManager.getHeader(context, "oai_warning",
                                "Couldn't disseminate " + metadataPrefix
                                        + " for " + itemInfo.handle));
                    }
                }
            }

            // Put results in form needed to return
            results.put("records", records.iterator());

            log.info(LogManager.getHeader(context, "oai_harvest", "results=" + records.size() + ", ignore=" + ignore));

            // If we have MAX_RECORDS records, we need to provide a resumption
            // token
            if ((records.size() + ignore) >= MAX_RECORDS)
            {
                String resumptionToken = makeResumptionToken(from, until, set,
                        metadataPrefix, offset + MAX_RECORDS);

                if (log.isDebugEnabled())
                {
                    log.debug(LogManager
                            .getHeader(context, "made_resumption_token",
                                    "token=" + resumptionToken));
                }

                results.put("resumptionMap", getResumptionMap(resumptionToken));

                //results.put("resumptionToken", resumptionToken);
            }
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context, "database_error", ""), se);

            // Stack trace loss as OAI Exception does not support cause
            throw new OAIInternalServerError(se.toString());
        }
        catch (ParseException pe)
        {
            // Stack trace loss as OAI Exception does not support cause
        	throw new OAIInternalServerError(pe.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }

        return results;
    }

    /**
     * Retrieve a list of sets that satisfy the specified criteria
     * 
     * @return a Map object containing "sets" Iterator object (contains
     *         <setSpec/>XML Strings) as well as an optional resumptionMap Map.
     * @exception NoSetHierarchyException
     *                signals an http status code 400 problem
     * @exception OAIInternalServerError
     *                signals an http status code 500 problem
     */
    public Map listSets() throws NoSetHierarchyException,
            OAIInternalServerError
    {
        log.info(LogManager.getHeader(null, "oai_request", "verb=listSets"));

        Context context = null;

        // List to put results in
        List<String> sets = new LinkedList<String>();

        try
        {
            context = new Context();

            Collection[] allCols = Collection.findAll(context);
            StringBuffer spec = null;
            for (int i = 0; i < allCols.length; i++)
            {
                spec = new StringBuffer("<set><setSpec>hdl_");
                spec.append(allCols[i].getHandle().replace('/', '_'));
                spec.append("</setSpec>");
                String collName = allCols[i].getMetadata("name");
                if(collName != null)
                {
                	spec.append("<setName>");
                	spec.append(Utils.addEntities(collName));
                	spec.append("</setName>");
                }
                else
                {
                	spec.append("<setName />");
                    // Warn that there is an error of a null set name
                	log.info(LogManager.getHeader(null, "oai_error",
                			       "null_set_name_for_set_id_" + allCols[i].getHandle()));
                }
                spec.append("</set>");
                sets.add(spec.toString());
            }

            Community[] allComs = Community.findAll(context);
            for (int i = 0; i < allComs.length; i++)
            {
                spec = new StringBuffer("<set><setSpec>hdl_");
                spec.append(allComs[i].getHandle().replace('/', '_'));
                spec.append("</setSpec>");
                String commName = allComs[i].getMetadata("name");
                if(commName != null)
                {
                	spec.append("<setName>");
                	spec.append(Utils.addEntities(commName));
                	spec.append("</setName>");
                }
                else
                {
                	spec.append("<setName />");
                    // Warn that there is an error of a null set name
                	log.info(LogManager.getHeader(null, "oai_error",
                			       "null_set_name_for_set_id_" + allComs[i].getHandle()));
                }
                spec.append("</set>");
                sets.add(spec.toString());
            }
        }
        catch (SQLException se)
        {
            // Log the error
            log.warn(LogManager.getHeader(context, "database_error", ""), se);

            // Stack trace loss as OAI Exception does not support cause
            throw new OAIInternalServerError(se.toString());
        }
        finally
        {
            if (context != null)
            {
                context.abort();
            }
        }

        // Put results in form needed to return
        Map<String, Iterator<String>> results = new HashMap<String, Iterator<String>>();
        results.put("sets", sets.iterator());

        return results;
    }

    /**
     * Retrieve the next set of sets associated with the resumptionToken
     * 
     * @param resumptionToken
     *            implementation-dependent format taken from the previous
     *            listSets() Map result.
     * @return a Map object containing "sets" Iterator object (contains
     *         <setSpec/>XML Strings) as well as an optional resumptionMap Map.
     * @exception BadResumptionTokenException
     *                the value of the resumptionToken is invalid or expired.
     * @exception OAIInternalServerError
     *                signals an http status code 500 problem
     */
    public Map listSets(String resumptionToken)
            throws BadResumptionTokenException, OAIInternalServerError
    {
        // Resumption tokens not yet supported
        throw new BadResumptionTokenException();
    }

    /**
     * close the repository
     */
    public void close()
    {
    }

    // ******************************************
    // Internal DSpace utility methods below here
    // ******************************************

    /**
     * Get the community or collection signified by a set spec
     * 
     * @param context
     *            DSpace context object
     * @param set
     *            OAI set spec
     * @return the corresponding community or collection, or null if no set
     *         provided
     */
    private DSpaceObject resolveSet(Context context, String set)
            throws SQLException, BadArgumentException
    {
        if (set == null)
        {
            return null;
        }

        DSpaceObject o = null;

        /*
         * set specs are in form hdl_123.456_789 corresponding to
         * hdl:123.456/789
         */
        if (set.startsWith("hdl_"))
        {
            // Looks OK so far... turn second _ into /
            String handle = set.substring(4).replace('_', '/');
            o = HandleManager.resolveToObject(context, handle);
        }

        // If it corresponds to a collection or a community, that's the set we
        // want
        if ((o != null) &&
                ((o instanceof Collection) || (o instanceof Community))) 
        {
            return o;
        }

        // Handle is either non-existent, or corresponds to a non-collection
        // Either way, a bad set spec, ergo a bad argument
        throw new BadArgumentException();
    }

    /**
     * Create a resumption token. The relevant parameters for the harvest are
     * put in a
     * 
     * @param from
     *            OAI 'from' parameter
     * @param until
     *            OAI 'until' parameter
     * @param set
     *            OAI 'set' parameter
     * @param prefix
     *            OAI 'metadataPrefix' parameter
     * @param offset
     *            where to start the next harvest
     * 
     * @return the appropriate resumption token
     */
    private String makeResumptionToken(String from, String until, String set,
            String prefix, int offset)
    {
        StringBuffer token = new StringBuffer();

        if (from != null)
        {
            token.append(from);
        }

        token.append("/");

        if (until != null)
        {
            token.append(until);
        }

        token.append("/");

        if (set != null)
        {
            token.append(set);
        }

        token.append("/");

        if (prefix != null)
        {
            token.append(prefix);
        }

        token.append("/");
        token.append(String.valueOf(offset));

        return (token.toString());
    }

    /**
     * Get the information out of a resumption token
     * 
     * @param token
     *            the resumption token
     * @return a 5-long array of Objects; 4 Strings (from, until, set, prefix)
     *         and an Integer (the offset)
     */
    private Object[] decodeResumptionToken(String token)
            throws BadResumptionTokenException
    {
        Object[] obj = new Object[5];
        StringTokenizer st = new StringTokenizer(token, "/", true);

        try
        {
            // Extract from, until, set, prefix
            for (int i = 0; i < 4; i++)
            {
                if (!st.hasMoreTokens())
                {
                    throw new BadResumptionTokenException();
                }

                String s = st.nextToken();

                // If this value is a delimiter /, we have no value for this
                // part
                // of the resumption token.
                if (s.equals("/"))
                {
                    obj[i] = null;
                }
                else
                {
                    obj[i] = s;

                    // Skip the delimiter
                    st.nextToken();
                }

                log.debug("is: " + (String) obj[i]);
            }

            if (!st.hasMoreTokens())
            {
                throw new BadResumptionTokenException();
            }

            obj[4] = Integer.valueOf(st.nextToken());
        }
        catch (NumberFormatException nfe)
        {
            // Stack trace loss as OAI Exception does not support cause
            throw new BadResumptionTokenException();
        }
        catch (NoSuchElementException nsee)
        {
            // Stack trace loss as OAI Exception does not support cause
            throw new BadResumptionTokenException();
        }

        return obj;
    }
}
