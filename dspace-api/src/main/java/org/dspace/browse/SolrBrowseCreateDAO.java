/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.sort.OrderFormat;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;

/**
 * 
 * @author Andrea Bollini (CILEA)
 *
 */
public class SolrBrowseCreateDAO implements BrowseCreateDAO,
        SolrServiceIndexPlugin
{
    private static final String INFO_NOSQL_TO_RUN = "No SQL to run: data are stored in the SOLR Search Core. PLEASE NOTE THAT YOU MUST UPDATE THE DISCOVERY INDEX AFTER ANY CHANGES TO THE BROWSE CONFIGURATION";

    // reference to a DBMS BrowseCreateDAO needed to remove old tables when
    // switching from DBMS to SOLR
    private BrowseCreateDAO dbCreateDAO;

    private static final Logger log = Logger
            .getLogger(SolrBrowseCreateDAO.class);

    private BrowseIndex[] bis;

    public SolrBrowseCreateDAO()
    {
        try
        {
            bis = BrowseIndex.getBrowseIndices();
        }
        catch (BrowseException e)
        {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }

        for (BrowseIndex bi : bis)
            bi.generateMdBits();
    }

    public SolrBrowseCreateDAO(Context context) throws BrowseException
    {
        // For compatibility with previous versions
        String db = ConfigurationManager.getProperty("db.name");
        if ("postgres".equals(db))
        {
            dbCreateDAO = new BrowseCreateDAOPostgres(context);
        }
        else if ("oracle".equals(db))
        {
            dbCreateDAO = new BrowseCreateDAOOracle(context);
        }
        else
        {
            throw new BrowseException(
                    "The configuration for db.name is either invalid, or contains an unrecognised database");
        }

        try
        {
            bis = BrowseIndex.getBrowseIndices();
        }
        catch (BrowseException e)
        {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }

        for (BrowseIndex bi : bis)
            bi.generateMdBits();
    }

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument doc)
    {
        if (!(dso instanceof Item))
        {
            return;
        }
        Item item = (Item) dso;

        // faceting for metadata browsing. It is different than search facet
        // because if there are authority with variants support we wan't all the
        // variants to go in the facet... they are sorted by count so just the
        // prefered label is relevant
        for (BrowseIndex bi : bis)
        {
            log.debug("Indexing for item " + item.getID() + ", for index: "
                    + bi.getTableName());

            if (bi.isMetadataIndex())
            {
                // values to show in the browse list
                Set<String> distFValues = new HashSet<String>();
                // value for lookup without authority
                Set<String> distFVal = new HashSet<String>();
                // value for lookup with authority
                Set<String> distFAuths = new HashSet<String>();
                // value for lookup when partial search (the item mapper tool use it)
                Set<String> distValuesForAC = new HashSet<String>();

                // now index the new details - but only if it's archived and not
                // withdrawn
                if (item.isArchived() || item.isWithdrawn())
                {
                    // get the metadata from the item
                    for (int mdIdx = 0; mdIdx < bi.getMetadataCount(); mdIdx++)
                    {
                        String[] md = bi.getMdBits(mdIdx);
                        DCValue[] values = item.getMetadata(md[0], md[1],
                                md[2], Item.ANY);

                        // if we have values to index on, then do so
                        if (values != null && values.length > 0)
                        {
                            int minConfidence = MetadataAuthorityManager
                                    .getManager().getMinConfidence(
                                            values[0].schema,
                                            values[0].element,
                                            values[0].qualifier);

                            boolean ignoreAuthority = new DSpace()
                                    .getConfigurationService()
                                    .getPropertyAsType(
                                            "discovery.browse.authority.ignore."
                                                    + bi.getName(),
                                            new DSpace()
                                                    .getConfigurationService()
                                                    .getPropertyAsType(
                                                            "discovery.browse.authority.ignore",
                                                            new Boolean(false)),
                                            true);
                            for (int x = 0; x < values.length; x++)
                            {
                                // Ensure that there is a value to index before
                                // inserting it
                                if (StringUtils.isEmpty(values[x].value))
                                {
                                    log.error("Null metadata value for item "
                                            + item.getID()
                                            + ", field: "
                                            + values[x].schema
                                            + "."
                                            + values[x].element
                                            + (values[x].qualifier == null ? ""
                                                    : "." + values[x].qualifier));
                                }
                                else
                                {
                                    if (bi.isAuthorityIndex()
                                            && (values[x].authority == null || values[x].confidence < minConfidence))
                                    {
                                        // if we have an authority index only
                                        // authored metadata will go here!
                                        log.debug("Skipping item="
                                                + item.getID() + ", field="
                                                + values[x].schema + "."
                                                + values[x].element + "."
                                                + values[x].qualifier
                                                + ", value=" + values[x].value
                                                + ", authority="
                                                + values[x].authority
                                                + ", confidence="
                                                + values[x].confidence
                                                + " (BAD AUTHORITY)");
                                        continue;
                                    }

                                    // is there any valid (with appropriate
                                    // confidence) authority key?
                                    if ((ignoreAuthority && !bi.isAuthorityIndex())
                                            || (values[x].authority != null && values[x].confidence >= minConfidence))
                                    {
                                        distFAuths.add(values[x].authority);
                                        distValuesForAC.add(values[x].value);

                                        String preferedLabel = null;
                                        boolean ignorePrefered = new DSpace()
                                                .getConfigurationService()
                                                .getPropertyAsType(
                                                        "discovery.browse.authority.ignore-prefered."
                                                                + bi.getName(),
                                                        new DSpace()
                                                                .getConfigurationService()
                                                                .getPropertyAsType(
                                                                        "discovery.browse.authority.ignore-prefered",
                                                                        new Boolean(
                                                                                false)),
                                                        true);
                                        if (!ignorePrefered)
                                        {
                                            preferedLabel = ChoiceAuthorityManager
                                                    .getManager()
                                                    .getLabel(
                                                            values[x].schema,
                                                            values[x].element,
                                                            values[x].qualifier,
                                                            values[x].authority,
                                                            values[x].language);
                                        }
                                        List<String> variants = null;

                                        boolean ignoreVariants = new DSpace()
                                                .getConfigurationService()
                                                .getPropertyAsType(
                                                        "discovery.browse.authority.ignore-variants."
                                                                + bi.getName(),
                                                        new DSpace()
                                                                .getConfigurationService()
                                                                .getPropertyAsType(
                                                                        "discovery.browse.authority.ignore-variants",
                                                                        new Boolean(
                                                                                false)),
                                                        true);
                                        if (!ignoreVariants)
                                        {
                                            variants = ChoiceAuthorityManager
                                                    .getManager()
                                                    .getVariants(
                                                            values[x].schema,
                                                            values[x].element,
                                                            values[x].qualifier,
                                                            values[x].authority,
                                                            values[x].language);
                                        }

                                        if (StringUtils
                                                .isNotBlank(preferedLabel))
                                        {
                                            String nLabel = OrderFormat
                                                    .makeSortString(
                                                            preferedLabel,
                                                            values[x].language,
                                                            bi.getDataType());
                                            distFValues
                                                    .add(nLabel
                                                            + SolrServiceImpl.FILTER_SEPARATOR
                                                            + preferedLabel
                                                            + SolrServiceImpl.AUTHORITY_SEPARATOR
                                                            + values[x].authority);
                                            distValuesForAC.add(preferedLabel);
                                        }

                                        if (variants != null)
                                        {
                                            for (String var : variants)
                                            {
                                                String nVal = OrderFormat
                                                        .makeSortString(
                                                                var,
                                                                values[x].language,
                                                                bi.getDataType());
                                                distFValues
                                                        .add(nVal
                                                                + SolrServiceImpl.FILTER_SEPARATOR
                                                                + var
                                                                + SolrServiceImpl.AUTHORITY_SEPARATOR
                                                                + values[x].authority);
                                                distValuesForAC.add(var);
                                            }
                                        }
                                    }
                                    else
                                    // put it in the browse index as if it
                                    // hasn't have an authority key
                                    {
                                        // get the normalised version of the
                                        // value
                                        String nVal = OrderFormat
                                                .makeSortString(
                                                        values[x].value,
                                                        values[x].language,
                                                        bi.getDataType());
                                        distFValues
                                                .add(nVal
                                                        + SolrServiceImpl.FILTER_SEPARATOR
                                                        + values[x].value);
                                        distFVal.add(values[x].value);
                                        distValuesForAC.add(values[x].value);
                                    }
                                }
                            }
                        }
                    }
                }

                for (String facet : distFValues)
                {
                    doc.addField(bi.getDistinctTableName() + "_filter", facet);
                }
                for (String facet : distFAuths)
                {
                    doc.addField(bi.getDistinctTableName()
                            + "_authority_filter", facet);
                }
                for (String facet : distValuesForAC)
                {
                    doc.addField(bi.getDistinctTableName() + "_partial", facet);
                }
                for (String facet : distFVal)
                {
                    doc.addField(bi.getDistinctTableName()+"_value_filter", facet);
                }
            }
        }

        // Add sorting options as configurated for the browse system
        try
        {
            for (SortOption so : SortOption.getSortOptions())
            {
                DCValue[] dcvalue = item.getMetadata(so.getMetadata());
                if (dcvalue != null && dcvalue.length > 0)
                {
                    String nValue = OrderFormat
                            .makeSortString(dcvalue[0].value,
                                    dcvalue[0].language, so.getType());
                    doc.addField("bi_sort_" + so.getNumber() + "_sort", nValue);
                }
            }
        }
        catch (SortException e)
        {
            // we can't solve it so rethrow as runtime exception
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteByItemID(String table, int itemID) throws BrowseException
    {
    }

    @Override
    public void deleteCommunityMappings(int itemID) throws BrowseException
    {
    }

    @Override
    public void updateCommunityMappings(int itemID) throws BrowseException
    {
    }

    @Override
    public void insertIndex(String table, int itemID, Map sortCols)
            throws BrowseException
    {
		// this is required to be sure that communities2item will be cleaned
		// after the switch to SOLRBrowseDAOs. See DS-1619
    	dbCreateDAO.deleteCommunityMappings(itemID);
    }

    @Override
    public boolean updateIndex(String table, int itemID, Map sortCols)
            throws BrowseException
    {
        return false;
    }

    @Override
    public int getDistinctID(String table, String value, String authority,
            String sortValue) throws BrowseException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int insertDistinctRecord(String table, String value,
            String authority, String sortValue) throws BrowseException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String dropIndexAndRelated(String table, boolean execute)
            throws BrowseException
    {
        return dbCreateDAO.dropIndexAndRelated(table, execute);
    }

    @Override
    public String dropSequence(String sequence, boolean execute)
            throws BrowseException
    {
        return dbCreateDAO.dropSequence(sequence, execute);
    }

    @Override
    public String dropView(String view, boolean execute) throws BrowseException
    {
        return dbCreateDAO.dropView(view, execute);
    }

    @Override
    public String createSequence(String sequence, boolean execute)
            throws BrowseException
    {
        return INFO_NOSQL_TO_RUN;
    }

    @Override
    public String createPrimaryTable(String table, List sortCols,
            boolean execute) throws BrowseException
    {
        return INFO_NOSQL_TO_RUN;
    }

    @Override
    public String[] createDatabaseIndices(String table, List<Integer> sortCols,
            boolean value, boolean execute) throws BrowseException
    {
        return new String[] { INFO_NOSQL_TO_RUN };
    }

    @Override
    public String[] createMapIndices(String disTable, String mapTable,
            boolean execute) throws BrowseException
    {
        return new String[] { INFO_NOSQL_TO_RUN };
    }

    @Override
    public String createCollectionView(String table, String view,
            boolean execute) throws BrowseException
    {
        return INFO_NOSQL_TO_RUN;
    }

    @Override
    public String createCommunityView(String table, String view, boolean execute)
            throws BrowseException
    {
        return INFO_NOSQL_TO_RUN;
    }

    @Override
    public String createDistinctTable(String table, boolean execute)
            throws BrowseException
    {
        return INFO_NOSQL_TO_RUN;
    }

    @Override
    public String createDistinctMap(String table, String map, boolean execute)
            throws BrowseException
    {
        return INFO_NOSQL_TO_RUN;
    }

    public MappingResults updateDistinctMappings(String table, int itemID,
            Set<Integer> distinctIDs) throws BrowseException
    {
        return new MappingResults()
        {

            @Override
            public List<Integer> getRetainedDistinctIds()
            {
                return new ArrayList<Integer>();
            }

            @Override
            public List<Integer> getRemovedDistinctIds()
            {
                return new ArrayList<Integer>();
            }

            @Override
            public List<Integer> getAddedDistinctIds()
            {
                return new ArrayList<Integer>();
            }
        };
    }

    @Override
    public boolean testTableExistence(String table) throws BrowseException
    {
        return dbCreateDAO.testTableExistence(table);
    }

    @Override
    public List<Integer> deleteMappingsByItemID(String mapTable, int itemID)
            throws BrowseException
    {
        return new ArrayList<Integer>();
    }

    @Override
    public void pruneExcess(String table, boolean withdrawn)
            throws BrowseException
    {
    }

    @Override
    public void pruneMapExcess(String map, boolean withdrawn,
            List<Integer> distinctIds) throws BrowseException
    {
    }

    @Override
    public void pruneDistinct(String table, String map,
            List<Integer> distinctIds) throws BrowseException
    {
    }

}
