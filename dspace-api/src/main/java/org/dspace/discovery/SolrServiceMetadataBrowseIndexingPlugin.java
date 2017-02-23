/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.HashSet;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.MetadataValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.OrderFormat;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;

/**
 * A Solr Indexing plugin for the "metadata" browse index type.
 * <p>
 * For Example:
 *    webui.browse.index.2 = author:metadata:dc.contributor.*\,dc.creator:text
 *    OR
 *    webui.browse.index.4 = subject:metadata:dc.subject.*:text
 * <p>
 * This plugin was based heavily on the old (DSpace 5.x or below), SolrBrowseCreateDAO
 * class, specifically its "additionalIndex()" method, which used to perform this function.
 * 
 * @author Tim Donohue
 */
public class SolrServiceMetadataBrowseIndexingPlugin implements SolrServiceIndexPlugin {

    private static final Logger log = Logger
            .getLogger(SolrServiceMetadataBrowseIndexingPlugin.class);
    
    @Autowired(required = true)
    protected ItemService itemService;
    
    @Autowired(required = true)
    protected MetadataAuthorityService metadataAuthorityService;
    
    @Autowired(required = true)
    protected ChoiceAuthorityService choiceAuthorityService;

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document)
    {
        // Only works for Items
        if (!(dso instanceof Item))
        {
            return;
        }
        Item item = (Item) dso;

        // Get the currently configured browse indexes
        BrowseIndex[] bis;
        try
        {
            bis = BrowseIndex.getBrowseIndices();
        }
        catch (BrowseException e)
        {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        
        // Faceting for metadata browsing. It is different than search facet
        // because if there are authority with variants support we want all the
        // variants to go in the facet... they are sorted by count so just the
        // prefered label is relevant
        for (BrowseIndex bi : bis)
        {
            log.debug("Indexing for item " + item.getID() + ", for index: "
                    + bi.getTableName());

            // ONLY perform indexing for "metadata" type indices
            if (bi.isMetadataIndex())
            {
                // Generate our bits of metadata (so getMdBits() can be used below)
                bi.generateMdBits();

                // values to show in the browse list
                Set<String> distFValues = new HashSet<String>();
                // value for lookup without authority
                Set<String> distFVal = new HashSet<String>();
                // value for lookup with authority
                Set<String> distFAuths = new HashSet<String>();
                // value for lookup when partial search (the item mapper tool use it)
                Set<String> distValuesForAC = new HashSet<String>();

                // now index the new details - but only if it's archived or
                // withdrawn
                if (item.isArchived() || item.isWithdrawn())
                {
                    // get the metadata from the item
                    for (int mdIdx = 0; mdIdx < bi.getMetadataCount(); mdIdx++)
                    {
                        String[] md = bi.getMdBits(mdIdx);
                        List<MetadataValue> values = itemService.getMetadata(item, md[0], md[1],
                                md[2], Item.ANY);

                        // if we have values to index on, then do so
                        if (values != null && values.size() > 0)
                        {
                            int minConfidence = metadataAuthorityService.getMinConfidence(values.get(0).getMetadataField());

                            boolean ignoreAuthority = DSpaceServicesFactory.getInstance().getConfigurationService()
                                    .getPropertyAsType(
                                            "discovery.browse.authority.ignore."
                                                    + bi.getName(),
                                            DSpaceServicesFactory.getInstance().getConfigurationService()
                                                    .getPropertyAsType(
                                                            "discovery.browse.authority.ignore",
                                                            new Boolean(false)),
                                            true);
                            for (int x = 0; x < values.size(); x++)
                            {
                                // Ensure that there is a value to index before
                                // inserting it
                                if (StringUtils.isEmpty(values.get(x).getValue()))
                                {
                                    log.error("Null metadata value for item "
                                            + item.getID()
                                            + ", field: "
                                            + values.get(x).getMetadataField().toString()
                                    );
                                }
                                else
                                {
                                    if (bi.isAuthorityIndex()
                                            && (values.get(x).getAuthority() == null || values.get(x).getConfidence() < minConfidence))
                                    {
                                        // if we have an authority index only
                                        // authored metadata will go here!
                                        log.debug("Skipping item="
                                                + item.getID() + ", field="
                                                + values.get(x).getMetadataField().toString()
                                                + ", value=" + values.get(x).getValue()
                                                + ", authority="
                                                + values.get(x).getAuthority()
                                                + ", confidence="
                                                + values.get(x).getConfidence()
                                                + " (BAD AUTHORITY)");
                                        continue;
                                    }

                                    // is there any valid (with appropriate
                                    // confidence) authority key?
                                    if ((ignoreAuthority && !bi.isAuthorityIndex())
                                            || (values.get(x).getAuthority() != null && values.get(x).getConfidence() >= minConfidence))
                                    {
                                        distFAuths.add(values.get(x).getAuthority());
                                        distValuesForAC.add(values.get(x).getValue());

                                        String preferedLabel = null;
                                        boolean ignorePrefered = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                .getPropertyAsType(
                                                        "discovery.browse.authority.ignore-prefered."
                                                                + bi.getName(),
                                                        DSpaceServicesFactory.getInstance().getConfigurationService()
                                                                .getPropertyAsType(
                                                                        "discovery.browse.authority.ignore-prefered",
                                                                        new Boolean(
                                                                                false)),
                                                        true);
                                        if (!ignorePrefered)
                                        {
                                            preferedLabel = choiceAuthorityService
                                                    .getLabel(values.get(x), values.get(x).getLanguage());
                                        }
                                        List<String> variants = null;

                                        boolean ignoreVariants = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                .getPropertyAsType(
                                                        "discovery.browse.authority.ignore-variants."
                                                                + bi.getName(),
                                                        DSpaceServicesFactory.getInstance().getConfigurationService()
                                                                .getPropertyAsType(
                                                                        "discovery.browse.authority.ignore-variants",
                                                                        new Boolean(
                                                                                false)),
                                                        true);
                                        if (!ignoreVariants)
                                        {
                                            variants = choiceAuthorityService
                                                    .getVariants(
                                                            values.get(x));
                                        }

                                        if (StringUtils
                                                .isNotBlank(preferedLabel))
                                        {
                                            String nLabel = OrderFormat
                                                    .makeSortString(
                                                            preferedLabel,
                                                            values.get(x).getLanguage(),
                                                            bi.getDataType());
                                            distFValues
                                                    .add(nLabel
                                                            + SolrServiceImpl.FILTER_SEPARATOR
                                                            + preferedLabel
                                                            + SolrServiceImpl.AUTHORITY_SEPARATOR
                                                            + values.get(x).getAuthority());
                                            distValuesForAC.add(preferedLabel);
                                        }

                                        if (variants != null)
                                        {
                                            for (String var : variants)
                                            {
                                                String nVal = OrderFormat
                                                        .makeSortString(
                                                                var,
                                                                values.get(x).getLanguage(),
                                                                bi.getDataType());
                                                distFValues
                                                        .add(nVal
                                                                + SolrServiceImpl.FILTER_SEPARATOR
                                                                + var
                                                                + SolrServiceImpl.AUTHORITY_SEPARATOR
                                                                + values.get(x).getAuthority());
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
                                                        values.get(x).getValue(),
                                                        values.get(x).getLanguage(),
                                                        bi.getDataType());
                                        distFValues
                                                .add(nVal
                                                        + SolrServiceImpl.FILTER_SEPARATOR
                                                        + values.get(x).getValue());
                                        distFVal.add(values.get(x).getValue());
                                        distValuesForAC.add(values.get(x).getValue());
                                    }
                                }
                            }
                        }
                    }
                }

                for (String facet : distFValues)
                {
                    document.addField(bi.getDistinctTableName() + "_filter", facet);
                }
                for (String facet : distFAuths)
                {
                    document.addField(bi.getDistinctTableName()
                            + "_authority_filter", facet);
                }
                for (String facet : distValuesForAC)
                {
                    document.addField(bi.getDistinctTableName() + "_partial", facet);
                }
                for (String facet : distFVal)
                {
                    document.addField(bi.getDistinctTableName()+"_value_filter", facet);
                }
            }
        }

        // Add sorting options as configurated for the browse system
        try
        {
            for (SortOption so : SortOption.getSortOptions())
            {
                List<MetadataValue> dcvalue = itemService.getMetadataByMetadataString(item, so.getMetadata());
                if (dcvalue != null && dcvalue.size() > 0)
                {
                    String nValue = OrderFormat
                            .makeSortString(dcvalue.get(0).getValue(),
                                    dcvalue.get(0).getLanguage(), so.getType());
                    document.addField("bi_sort_" + so.getNumber() + "_sort", nValue);
                }
            }
        }
        catch (SortException e)
        {
            // we can't solve it so rethrow as runtime exception
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
