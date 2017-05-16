/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.AValue;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.value.PointerValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.integration.CrisEnhancer;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.sort.OrderFormat;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;

public class CrisBrowseSolrIndexPlugin implements CrisServiceIndexPlugin
{
    private static final Logger log = Logger
            .getLogger(CrisBrowseSolrIndexPlugin.class);

    private BrowseIndex[] bis;

    public CrisBrowseSolrIndexPlugin()
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

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> dso, SolrInputDocument doc, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        if (!(dso instanceof ACrisObject))
        {
            return;
        }
        
        List<CrisEnhancer> crisEnhancers = new DSpace().getServiceManager()
                .getServicesByType(CrisEnhancer.class);
        
        ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item = dso;

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
                // value for lookup when partial search (the item mapper tool
                // use it)
                Set<String> distValuesForAC = new HashSet<String>();

                // now index the new details - but only if it's archived and not
                // withdrawn
                if (item.getStatus() != null && item.getStatus())
                {
                    // get the metadata from the item
                    for (int mdIdx = 0; mdIdx < bi.getMetadataCount(); mdIdx++)
                    {
                        String[] md = bi.getMdBits(mdIdx);
                        if (!md[0].equalsIgnoreCase("cris"
                                + item.getPublicPath()))
                        {
                            continue;
                        }

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

                        List<P> proprieties = item.getAnagrafica4view().get(
                                md[1]);
                        
                        // add support for enhanced property in the browse
                        if ((proprieties == null || proprieties.size() == 0) 
                        		&& StringUtils.isNotEmpty(md[2])) {
                        	// check if it is an enhanced property
                            for (CrisEnhancer cEnh : crisEnhancers)
                            {
                                if (cEnh.getClazz().isAssignableFrom(dso.getClass()))
                                {
                                	if (StringUtils.equals(cEnh.getAlias(), md[1])) {
                                		proprieties = cEnh.getProperties(dso, md[2]);
                                		// clean the md[2] as we have already found the final value
                                		md[2] = "";
                                	}
                                }
                            }
                        }
                        
                        List<AValue> values = new ArrayList<AValue>();
                        if (proprieties != null)
                        {
                            for (P prop : proprieties)
                            {
                                AValue val = prop.getValue();
                                if (StringUtils.isNotEmpty(md[2])
                                        && val instanceof PointerValue
                                        && prop.getObject() instanceof ACrisObject)
                                {
                                    List pointProps = (List) ((ACrisObject) prop.getObject())
                                            .getAnagrafica4view().get(md[2]);
                                    if (pointProps != null
                                            && pointProps.size() > 0)
                                    {
                                        for (Object pprop : pointProps)
                                        {
                                            values.add(((Property) pprop)
                                                    .getValue());
                                        }
                                    }
                                }
                                else
                                {
                                    values.add(val);
                                }
                            }
                        }

                        // if we have values to index on, then do so
                        for (AValue val : values)
                        {
                            if (val == null)
                            {
                                continue;
                            }
                            String authority = null;
                            String sval = val.toString();
                            if (val instanceof PointerValue
                                    && val.getObject() instanceof ACrisObject)
                            {
                                authority = ((ACrisObject) val.getObject()).getId()
                                        .toString();
                            }                            

                            if (bi.isAuthorityIndex()
                                    && StringUtils.isNotEmpty(authority))
                            {
                                // if we have an authority index only
                                // authored metadata will go here!
                                log.debug("Skipping cris=" + item.getUuid()
                                        + ", field=" + md[0] + "." + md[1]
                                        + "." + md[2] + ", value=" + sval
                                        + ", authority=" + authority
                                        + " (BAD AUTHORITY)");
                                continue;
                            }

                            // is there any valid (with appropriate
                            // confidence) authority key?
                            if ((ignoreAuthority && !bi.isAuthorityIndex())
                                    || StringUtils.isNotEmpty(authority))
                            {
                                String nLabel = OrderFormat.makeSortString(
                                        sval, null, bi.getDataType());

                                distFValues.add(nLabel
                                        + SolrServiceImpl.FILTER_SEPARATOR
                                        + sval
                                        + SolrServiceImpl.AUTHORITY_SEPARATOR
                                        + authority);
                                distFAuths.add(authority);
                                distValuesForAC.add(sval);
                            }
                            else
                            // put it in the browse index as if it
                            // hasn't have an authority key
                            {
                                // get the normalised version of the
                                // value
                                String nVal = OrderFormat.makeSortString(sval,
                                        null, bi.getDataType());
                                distFValues.add(nVal
                                        + SolrServiceImpl.FILTER_SEPARATOR
                                        + sval);
                                distFVal.add(sval);
                                distValuesForAC.add(sval);
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
                    doc.addField(bi.getDistinctTableName() + "_value_filter",
                            facet);
                }
            }
        }

        // Add sorting options as configurated for the browse system
        try
        {
            for (SortOption so : SortOption.getSortOptions())
            {
                String[] md = so.getMdBits();
                if (!md[0].equalsIgnoreCase("cris" + item.getPublicPath()))
                {
                    continue;
                }

                List<P> proprieties = item.getAnagrafica4view().get(md[1]);
                Object val = null;
                if (proprieties != null && proprieties.size() > 0)
                {
                    val = proprieties.get(0).getObject();
                }

                if (StringUtils.isNotEmpty(md[2]) && val instanceof ACrisObject)
                {
                    List pointProps = (List) ((ACrisObject) val)
                            .getAnagrafica4view().get(md[2]);
                    if (pointProps != null && pointProps.size() > 0)
                    {
                        val = ((Property) pointProps.get(0)).getObject();
                    }
                }
                if (val != null)
                {
                    String sval = val.toString();
                    String nValue = OrderFormat.makeSortString(sval, null,
                            so.getType());
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
	public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
			ACNO dso, SolrInputDocument sorlDoc, Map<String, List<DiscoverySearchFilter>> searchFilters) {
		// FIXME NOT SUPPORTED OPERATION
	}
}
