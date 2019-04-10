/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import it.cilea.osd.common.core.HasTimeStampInfo;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.AValue;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.value.DateValue;
import it.cilea.osd.jdyna.value.PointerValue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.integration.CrisEnhancer;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ICrisObject;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicNestedProperty;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicProperty;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.model.jdyna.OUNestedObject;
import org.dspace.app.cris.model.jdyna.OUNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUNestedProperty;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUProperty;
import org.dspace.app.cris.model.jdyna.OUTypeNestedObject;
import org.dspace.app.cris.model.jdyna.ProjectNestedObject;
import org.dspace.app.cris.model.jdyna.ProjectNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectNestedProperty;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectProperty;
import org.dspace.app.cris.model.jdyna.ProjectTypeNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.RPTypeNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightingConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryViewAndHighlightConfiguration;
import org.dspace.discovery.configuration.DiscoveryViewConfiguration;
import org.dspace.discovery.configuration.DiscoveryViewFieldConfiguration;
import org.dspace.discovery.configuration.HierarchicalSidebarFacetConfiguration;
import org.dspace.utils.DSpace;

public class CrisSearchService extends SolrServiceImpl
{

    private static final Logger log = Logger.getLogger(CrisSearchService.class);

    public ApplicationService getApplicationService()
    {

        return new DSpace().getServiceManager().getServiceByName(
                "applicationService", ApplicationService.class);
    }

    @Override
    public void indexContent(Context context, DSpaceObject dso, boolean force)
            throws SQLException
    {
    	if (dso == null) return;
        if (dso.getType() >= CrisConstants.CRIS_TYPE_ID_START)
        {
            indexCrisObject((ACrisObject) dso, force);
        }
        else
        {
			if (dso.getType() == Constants.ITEM) {
				super.indexContent(context, ((Item) dso).getWrapper(), force);
			} else {
				super.indexContent(context, dso, force);
			}
        }
    }

    @Override
    public void createIndex(Context context) throws SQLException, IOException
    {
        createCrisIndex(context);
        super.createIndex(context);
    }

    @Override
    public void updateIndex(Context context, boolean force)
    {
        updateCrisIndex(context, force);
        super.updateIndex(context, force);
    }

    @Override
    public void cleanIndex(boolean force) throws IOException, SQLException,
            SearchServiceException
    {
        super.cleanIndex(force);
        try
        {
            if (force)
            {
                getSolr().deleteByQuery(
                        "search.resourcetype:["
                                + CrisConstants.CRIS_TYPE_ID_START + " TO *]");
            }
            else
            {
                cleanIndex(force, CrisConstants.RP_TYPE_ID);
                cleanIndex(force, CrisConstants.NRP_TYPE_ID);
                cleanIndex(force, CrisConstants.PROJECT_TYPE_ID);
                cleanIndex(force, CrisConstants.NPROJECT_TYPE_ID);
                cleanIndex(force, CrisConstants.OU_TYPE_ID);
                cleanIndex(force, CrisConstants.NOU_TYPE_ID);
            }
        }
        catch (Exception e)
        {

            throw new SearchServiceException(e.getMessage(), e);
        }

    }

    @Override
    public DSpaceObject findDSpaceObject(Context context, SolrDocument doc)
            throws SQLException
    {
        Integer type = (Integer) doc.getFirstValue("search.resourcetype");
        if (type != null && type >= CrisConstants.CRIS_TYPE_ID_START)
        {
            Integer id = (Integer) doc.getFirstValue("search.resourceid");
            ACrisObject o = null;
            if (type > CrisConstants.CRIS_DYNAMIC_TYPE_ID_START)
            {
                o = getApplicationService()
                .get(ResearchObject.class, id);
            }
            else
            {
                switch (type)
                {
                case CrisConstants.RP_TYPE_ID:
                	o = getApplicationService()
                            .get(ResearcherPage.class, id);
                	break;

                case CrisConstants.PROJECT_TYPE_ID:
                	o = getApplicationService().get(Project.class, id);
                	break;

                case CrisConstants.OU_TYPE_ID:
                	o = getApplicationService().get(OrganizationUnit.class,
                            id);
                	break;

                default:
                	o = null;
                }
            }
            
            if (o != null)
            {
            	for (String f : doc.getFieldNames()) {
            		o.extraInfo.put(f, doc.getFirstValue(f));
                }
            }
            return o;
        }
        else
        {
            return super.findDSpaceObject(context, doc);
        }
    }


    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> boolean indexCrisObject(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> dso, boolean b)
    {
        boolean result = false;
        SolrInputDocument doc = buildDocument(dso.getType(), dso.getID(), null,
                null);

        log.debug("Building Cris: " + dso.getUuid());

        String schema = "cris" + dso.getPublicPath();
        String uuid = dso.getUuid();
        Boolean status = dso.getStatus();
        String sourceref = dso.getSourceRef();
        String sourceid = dso.getSourceID();
        String crisID = ResearcherPageUtils.getPersistentIdentifier(dso);
        if(StringUtils.isNotBlank(sourceref)) {
        	doc.addField("cris-sourceref", sourceref);
        }
        if(StringUtils.isNotBlank(sourceid)) {
        	doc.addField("cris-sourceid", sourceid);
        }
        if(StringUtils.isNotBlank(crisID)) {
            doc.addField("cris-id", crisID);
        }
        commonIndexerHeader(status, uuid, doc);

        // Keep a list of our sort values which we added, sort values can only
        // be added once
        List<String> sortFieldsAdded = new ArrayList<String>();
        Set<String> hitHighlightingFields = new HashSet<String>();
        List<String> toIgnoreFields = new ArrayList<String>();
        // A map used to save each sidebarFacet config by the metadata
        // fields
        Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<String, List<DiscoverySearchFilter>>();
        Map<String, DiscoverySortFieldConfiguration> sortFields = new HashMap<String, DiscoverySortFieldConfiguration>();
        Set<String> moreLikeThisFields = new HashSet<String>();
        List<String> toProjectionFields = new ArrayList<String>();

        commonIndexerDiscovery(schema, toIgnoreFields, searchFilters,
                toProjectionFields, sortFields, hitHighlightingFields);

        // add the special crisXX.this metadata
        indexProperty(doc, dso.getUuid(), schema + ".this", dso.getName(),
                    crisID,
                    toIgnoreFields, searchFilters, toProjectionFields,
                    sortFields, sortFieldsAdded, hitHighlightingFields,
                    moreLikeThisFields);


        commonsIndexerAnagrafica(dso, doc, schema, sortFieldsAdded,
                hitHighlightingFields, uuid, toIgnoreFields, searchFilters,
                toProjectionFields, sortFields, moreLikeThisFields);

        commonsIndexerEnhancer(dso, doc, schema, sortFieldsAdded,
                hitHighlightingFields, uuid, toIgnoreFields, searchFilters,
                toProjectionFields, sortFields, moreLikeThisFields);

        commonsIndexerTimestamp(dso, doc, schema);

        log.debug("  Added Metadata");

        // Do any additional indexing, depends on the plugins
        List<CrisServiceIndexPlugin> solrServiceIndexPlugins = new DSpace()
                .getServiceManager().getServicesByType(
                        CrisServiceIndexPlugin.class);
        for (CrisServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
        {
            solrServiceIndexPlugin.additionalIndex(dso, doc, searchFilters);
        }

        // write the index and close the inputstreamreaders
        try
        {
            writeDocument(doc,null);
            result = true;
            log.info("Wrote cris: " + dso.getUuid() + " to Index");
        }
        catch (Exception e)
        {
            log.error(
                    "Error while writing cris to discovery index: "
                            + dso.getUuid() + " message:" + e.getMessage(), e);
        }
        return result;
    }

    private void createCrisIndex(Context context)
    {
        this.<ResearcherPage, RPProperty, RPPropertiesDefinition, RPNestedProperty, RPNestedPropertiesDefinition, RPNestedObject, RPTypeNestedObject>createCrisIndex(context, ResearcherPage.class);
        this.<Project, ProjectProperty, ProjectPropertiesDefinition, ProjectNestedProperty, ProjectNestedPropertiesDefinition, ProjectNestedObject, ProjectTypeNestedObject>createCrisIndex(context, Project.class);
        this.<OrganizationUnit, OUProperty, OUPropertiesDefinition, OUNestedProperty, OUNestedPropertiesDefinition, OUNestedObject, OUTypeNestedObject>createCrisIndex(context, OrganizationUnit.class);
        this.<ResearchObject, DynamicProperty, DynamicPropertiesDefinition, DynamicNestedProperty, DynamicNestedPropertiesDefinition, DynamicNestedObject, DynamicTypeNestedObject>createCrisIndex(context, ResearchObject.class);
    }

    private void updateCrisIndex(Context context, boolean force)
    {
        cleanCrisIndex(context);
        createCrisIndex(context);
    }

    private void cleanCrisIndex(Context context)
    {
        try
        {
            getSolr().deleteByQuery(
                    "search.resourcetype:[" + CrisConstants.CRIS_TYPE_ID_START
                            + " TO *]");
        }
        catch (Exception e)
        {
            log.error("Error cleaning cris discovery index: " + e.getMessage(),
                    e);
        }
    }

    private <P extends Property<TP>, TP extends PropertiesDefinition> void indexProperty(
            SolrInputDocument doc, String uuid, String field, P meta,
            List<String> toIgnoreFields,
            Map<String, List<DiscoverySearchFilter>> searchFilters,
            List<String> toProjectionFields,
            Map<String, DiscoverySortFieldConfiguration> sortFields,
            List<String> sortFieldsAdded, Set<String> hitHighlightingFields,
            Set<String> moreLikeThisFields)
    {
        AValue value = meta.getValue();

        boolean storePrivate = ConfigurationManager.getBooleanProperty(CrisConstants.CFG_MODULE, "system.store.private.field", false);
        if (meta.getVisibility() != VisibilityConstants.PUBLIC)
        {
            if(value == null) {
                return;    
            }
            
            if(storePrivate) {
                doc.addField(field + "_private", value);
            }
            return;
        }

        String svalue = meta.toString();
        String authority = null;
        if (value instanceof PointerValue
                && value.getObject() instanceof ACrisObject)
        {
            authority = ResearcherPageUtils
                    .getPersistentIdentifier((ACrisObject) value.getObject());
        }

        if (value instanceof DateValue)
        {
            // TODO: make this date format configurable !
            // WARN: please note that the date in cris objects are assumed to be in UTC format 
            svalue = DateFormatUtils.format(((DateValue) value).getObject(),
                    "yyyy-MM-dd");
        }

        indexProperty(doc, uuid, field, svalue, authority, toIgnoreFields,
                searchFilters, toProjectionFields, sortFields, sortFieldsAdded,
                hitHighlightingFields, moreLikeThisFields);
    }

    private void indexProperty(SolrInputDocument doc, String uuid,
            String field, String svalue, String authority,
            List<String> toIgnoreFields,
            Map<String, List<DiscoverySearchFilter>> searchFilters,
            List<String> toProjectionFields,
            Map<String, DiscoverySortFieldConfiguration> sortFields,
            List<String> sortFieldsAdded, Set<String> hitHighlightingFields,
            Set<String> moreLikeThisFields)
    {
        if (toIgnoreFields.contains(field) || svalue == null)
        {
            return;
        }

        if ((searchFilters.get(field) != null))
        {
            List<DiscoverySearchFilter> searchFilterConfigs = searchFilters
                    .get(field);

            for (DiscoverySearchFilter searchFilter : searchFilterConfigs)
            {
                String separator = new DSpace().getConfigurationService()
                        .getProperty("discovery.solr.facets.split.char");
                if (separator == null)
                {
                    separator = FILTER_SEPARATOR;
                }
                doc.addField(searchFilter.getIndexFieldName(), svalue);
                doc.addField(searchFilter.getIndexFieldName() + "_keyword",
                        svalue);

				if (searchFilter.isUsedForCollapsingFeature()) {
					if (authority != null) {
						doc.addField(searchFilter.getIndexFieldName() + "_group", authority);
					} else {
						doc.addField(searchFilter.getIndexFieldName() + "_group", svalue);
					}

				}
				
                if (authority != null)
                {
                    doc.addField(searchFilter.getIndexFieldName() + "_keyword",
                            svalue + AUTHORITY_SEPARATOR + authority);
                    doc.addField(searchFilter.getIndexFieldName()
                            + "_authority", authority);
                    doc.addField(searchFilter.getIndexFieldName() + "_acid",
                            svalue.toLowerCase() + separator + svalue
                                    + AUTHORITY_SEPARATOR + authority);
                }
                // Add a dynamic fields for auto complete in search
                doc.addField(searchFilter.getIndexFieldName() + "_ac",
                        svalue.toLowerCase() + separator + svalue);

                if (searchFilter.getFilterType().equals(
                        DiscoverySearchFilterFacet.FILTER_TYPE_FACET))
                {
                    if (searchFilter.getType().equals(
                            DiscoveryConfigurationParameters.TYPE_TEXT))
                    {
                        // Add a special filter
                        // We use a separator to split up the lowercase
                        // and regular case, this is needed to get our
                        // filters in regular case
                        // Solr has issues with facet prefix and cases
                        if (authority != null)
                        {
                            String facetValue = svalue;
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_filter", facetValue.toLowerCase()
                                    + separator + facetValue
                                    + AUTHORITY_SEPARATOR + authority);
                        }
                        else
                        {
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_filter", svalue.toLowerCase()
                                    + separator + svalue);
                        }
                    }
                    else if (searchFilter.getType().equals(
                            DiscoveryConfigurationParameters.TYPE_DATE))
                    {
                        Date date = toDate(svalue);
                        if (date != null)
                        {
                            String indexField = searchFilter
                                    .getIndexFieldName() + ".year";
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword",
                                    DateFormatUtils.formatUTC(date, "yyyy"));
                            doc.addField(indexField,
                                    DateFormatUtils.formatUTC(date, "yyyy"));
                            // Also save a sort value of this year, this
                            // is required for determining the upper &
                            // lower bound year of our facet
                            if (doc.getField(indexField + "_sort") == null)
                            {
                                // We can only add one year so take the
                                // first one
                                doc.addField(indexField + "_sort",
                                        DateFormatUtils.formatUTC(date, "yyyy"));
                            }
                        }
                    }
                    else if (searchFilter.getType().equals(
                            DiscoveryConfigurationParameters.TYPE_HIERARCHICAL))
                    {
                        HierarchicalSidebarFacetConfiguration hierarchicalSidebarFacetConfiguration = (HierarchicalSidebarFacetConfiguration) searchFilter;
                        String[] subValues = svalue
                                .split(hierarchicalSidebarFacetConfiguration
                                        .getSplitter());
                        if (hierarchicalSidebarFacetConfiguration
                                .isSkipFirstNodeLevel() && 1 < subValues.length)
                        {
                            // Remove the first element of our array
                            subValues = (String[]) ArrayUtils.subarray(
                                    subValues, 1, subValues.length);
                        }
                        for (int i = 0; i < subValues.length; i++)
                        {
                            StringBuilder valueBuilder = new StringBuilder();
                            for (int j = 0; j <= i; j++)
                            {
                                valueBuilder.append(subValues[j]);
                                if (j < i)
                                {
                                    valueBuilder
                                            .append(hierarchicalSidebarFacetConfiguration
                                                    .getSplitter());
                                }
                            }

                            String indexValue = valueBuilder.toString().trim();
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_tax_" + i + "_filter",
                                    indexValue.toLowerCase() + separator
                                            + indexValue);
                            // We add the field x times that it has
                            // occurred
                            for (int j = i; j < subValues.length; j++)
                            {
                                doc.addField(searchFilter.getIndexFieldName()
                                        + "_filter", indexValue.toLowerCase()
                                        + separator + indexValue);
                                doc.addField(searchFilter.getIndexFieldName()
                                        + "_keyword", indexValue);
                            }
                        }
                    }
                }
            }
        }

        if ((sortFields.get(field) != null && !sortFieldsAdded.contains(field)))
        {
            // Only add sort value once
            String type = "";
            if (sortFields.get(field) != null)
            {
                type = sortFields.get(field).getType();
            }

            if (type.equals(DiscoveryConfigurationParameters.TYPE_DATE))
            {
                Date date = toDate(svalue);
                if (date != null)
                {
                    doc.addField(field + "_dt", date);
                }
                else
                {
                    log.warn("Error while indexing sort date field, cris: "
                            + uuid + " metadata field: " + field
                            + " date value: " + svalue);
                }
            }
            else
            {
                doc.addField(field + "_sort", svalue);
            }
            sortFieldsAdded.add(field);
        }

        if (hitHighlightingFields.contains(field)
                || hitHighlightingFields.contains("*"))
        {
            doc.addField(field + "_hl", svalue);
        }

        if (moreLikeThisFields.contains(field))
        {
            doc.addField(field + "_mlt", svalue);
        }

        doc.addField(field, svalue);
        if (authority != null)
        {
            doc.addField(field + "_authority", authority);
        }
        if (toProjectionFields.contains(field))
        {
            doc.addField(field + "_stored", svalue + STORE_SEPARATOR
                    + authority);
        }
    }


    @Override
    public void updateIndex(Context context, boolean force, int type)
    {

        if (type > CrisConstants.CRIS_DYNAMIC_TYPE_ID_START)
        {
            this.<ResearchObject, DynamicProperty, DynamicPropertiesDefinition, DynamicNestedProperty, DynamicNestedPropertiesDefinition, DynamicNestedObject, DynamicTypeNestedObject>createCrisIndex(context, ResearchObject.class);
        }
        else
        {
            if (CrisConstants.RP_TYPE_ID == type)
            {
                this.<ResearcherPage, RPProperty, RPPropertiesDefinition, RPNestedProperty, RPNestedPropertiesDefinition, RPNestedObject, RPTypeNestedObject>createCrisIndex(context, ResearcherPage.class);
            }
            else if (CrisConstants.PROJECT_TYPE_ID == type)
            {
                this.<Project, ProjectProperty, ProjectPropertiesDefinition, ProjectNestedProperty, ProjectNestedPropertiesDefinition, ProjectNestedObject, ProjectTypeNestedObject>createCrisIndex(context, Project.class);
            }
            else if (CrisConstants.OU_TYPE_ID == type)
            {
                this.<OrganizationUnit, OUProperty, OUPropertiesDefinition, OUNestedProperty, OUNestedPropertiesDefinition, OUNestedObject, OUTypeNestedObject>createCrisIndex(context, OrganizationUnit.class);
            }
            else
            {
                super.updateIndex(context, force, type);
            }
        }
    }

    private <T extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void createCrisIndex(
            Context context, Class<T> classCrisObject)
    {
    	long tot = getApplicationService().count(classCrisObject);
    	final int MAX_RESULT = 10;
    	long numpages = (tot / MAX_RESULT) + 1;
        for (int page = 1; page <= numpages; page++)
        {
			List<T> rpObjects = getApplicationService().getPaginateList(
					classCrisObject, "id", false, page, MAX_RESULT);
	
	        if (rpObjects != null)
	        {
	            for (T cris : rpObjects)
	            {
	                indexCrisObject(cris, true);
	                // indexing nested
	                for (ATNO anestedtype : getApplicationService().getList(
	                        cris.getClassTypeNested()))
	                {
	                    List<ACNO> anesteds = getApplicationService()
	                            .getNestedObjectsByParentIDAndTypoID(cris.getId(),
	                                    anestedtype.getId(), cris.getClassNested());
	                    for (ACNO anested : anesteds)
	                    {
	                        indexNestedObject(anested, true);
	                    }
	                }
	            }
	        }
        }
    }

    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> boolean indexNestedObject(
            ACNO dso, boolean b)
    {
        boolean result = false;
        SolrInputDocument doc = buildDocument(dso.getType(), dso.getID(), null,
                null);

        log.debug("Building Cris: " + dso.getUuid());

        ICrisObject<P, TP> parent = (ICrisObject<P, TP>) dso.getParent();
        doc.addField("search.parentfk", parent.getType() + "-" + parent.getID());
        String confName = "ncris" + parent.getPublicPath();
        String schema = confName + dso.getTypo().getShortName();
        doc.addField("search.schema_s", schema);
        String uuid = dso.getUuid();
        Boolean status = dso.getStatus();
        Integer position = dso.getPositionDef();
        doc.addField("position", position);
        commonIndexerHeader(status, uuid, doc);

        // Keep a list of our sort values which we added, sort values can only
        // be added once
        List<String> sortFieldsAdded = new ArrayList<String>();
        Set<String> hitHighlightingFields = new HashSet<String>();
        List<String> toIgnoreFields = new ArrayList<String>();
        // A map used to save each sidebarFacet config by the metadata
        // fields
        Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<String, List<DiscoverySearchFilter>>();
        Map<String, DiscoverySortFieldConfiguration> sortFields = new HashMap<String, DiscoverySortFieldConfiguration>();
        Set<String> moreLikeThisFields = new HashSet<String>();
        List<String> toProjectionFields = new ArrayList<String>();

        commonIndexerDiscovery(confName, toIgnoreFields, searchFilters,
                toProjectionFields, sortFields, hitHighlightingFields);

        commonsIndexerAnagrafica(dso, doc, schema, sortFieldsAdded,
                hitHighlightingFields, uuid, toIgnoreFields, searchFilters,
                toProjectionFields, sortFields, moreLikeThisFields);

        commonsIndexerEnhancer(dso, doc, schema, sortFieldsAdded,
                hitHighlightingFields, uuid, toIgnoreFields, searchFilters,
                toProjectionFields, sortFields, moreLikeThisFields);

        commonsIndexerTimestamp(dso, doc, schema);

        // Do any additional indexing, depends on the plugins
        List<CrisServiceIndexPlugin> solrServiceIndexPlugins = new DSpace()
                .getServiceManager().getServicesByType(
                        CrisServiceIndexPlugin.class);
        for (CrisServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
        {
            solrServiceIndexPlugin.additionalIndex(dso, doc, null);
        }
        
        log.debug("  Added Metadata");

        // write the index and close the inputstreamreaders
        try
        {
            writeDocument(doc,null);
            result = true;
            log.info("Wrote cris: " + dso.getUuid() + " to Index");
        }
        catch (Exception e)
        {
            log.error(
                    "Error while writing cris to discovery index: "
                            + dso.getUuid() + " message:" + e.getMessage(), e);
        }
        return result;

    }

    private <P, TP, PP, PTP> void commonIndexerHeader(Boolean status,
            String uuid, SolrInputDocument doc)
    {
               
        boolean disabled = false;
        if (status == null || !status)
        {            
            // only admin can searh/browse disabled researcher page
            disabled = true;
        }
        
        doc.addField("withdrawn", disabled);
        doc.addField("disabled", disabled);
        doc.addField("discoverable", !disabled);// item.isDiscoverable());
        
        doc.addField("read", "g0");        
        doc.addField("cris-uuid", uuid);
    }

    private void commonsIndexerTimestamp(HasTimeStampInfo dso,
            SolrInputDocument doc, String schema)
    {
        try
        {
            if (dso.getTimeStampInfo() != null
                    && dso.getTimeStampInfo().getTimestampCreated() != null
                    && dso.getTimeStampInfo().getTimestampCreated()
                            .getTimestamp() != null)
            {
                doc.addField(schema + ".time_creation_dt", dso
                        .getTimeStampInfo().getTimestampCreated()
                        .getTimestamp());
                doc.addField(
                        "crisDateIssued.year",
                        DateFormatUtils.formatUTC(dso.getTimeStampInfo()
                                .getTimestampCreated().getTimestamp(), "yyyy"));
            }

            if (dso.getTimeStampInfo() != null
                    && dso.getTimeStampInfo().getTimestampLastModified() != null
                    && dso.getTimeStampInfo().getTimestampLastModified()
                            .getTimestamp() != null)
            {
                doc.addField(schema + ".time_lastmodified_dt", dso
                        .getTimeStampInfo().getTimestampLastModified()
                        .getTimestamp());
                doc.addField(
                        "crisDateIssued.year_lastmodified",
                        DateFormatUtils.formatUTC(dso.getTimeStampInfo()
                                .getTimestampCreated().getTimestamp(), "yyyy"));
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    private <P extends Property<TP>, TP extends PropertiesDefinition> void commonsIndexerEnhancer(
            ICrisObject<P, TP> dso, SolrInputDocument doc, String schema,
            List<String> sortFieldsAdded, Set<String> hitHighlightingFields,
            String uuid, List<String> toIgnoreFields,
            Map<String, List<DiscoverySearchFilter>> searchFilters,
            List<String> toProjectionFields,
            Map<String, DiscoverySortFieldConfiguration> sortFields,
            Set<String> moreLikeThisFields)
    {
        try
        {
            List<CrisEnhancer> crisEnhancers = new DSpace().getServiceManager()
                    .getServicesByType(CrisEnhancer.class);

            for (CrisEnhancer cEnh : crisEnhancers)
            {
                if (cEnh.getClazz().isAssignableFrom(dso.getClass()))
                {
                    for (String qual : cEnh.getQualifiers())
                    {
                        List<P> props = cEnh.getProperties(dso, qual);
                        for (P meta : props)
                        {
                            String field = schema + "." + cEnh.getAlias() + "."
                                    + qual;
                            indexProperty(doc, uuid, field, meta,
                                    toIgnoreFields, searchFilters,
                                    toProjectionFields, sortFields,
                                    sortFieldsAdded, hitHighlightingFields,
                                    moreLikeThisFields);

                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    private <P extends Property<TP>, TP extends PropertiesDefinition> void commonsIndexerAnagrafica(
            AnagraficaSupport<P, TP> dso, SolrInputDocument doc, String schema,
            List<String> sortFieldsAdded, Set<String> hitHighlightingFields,
            String uuid, List<String> toIgnoreFields,
            Map<String, List<DiscoverySearchFilter>> searchFilters,
            List<String> toProjectionFields,
            Map<String, DiscoverySortFieldConfiguration> sortFields,
            Set<String> moreLikeThisFields)
    {
        try
        {
            List<P> mydc = dso.getAnagrafica();
            for (P meta : mydc)
            {
                String field = schema + "." + meta.getTypo().getShortName();
                indexProperty(doc, uuid, field, meta, toIgnoreFields,
                        searchFilters, toProjectionFields, sortFields,
                        sortFieldsAdded, hitHighlightingFields,
                        moreLikeThisFields);
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    private void commonIndexerDiscovery(String confName,
            List<String> toIgnoreFields,
            Map<String, List<DiscoverySearchFilter>> searchFilters,
            List<String> toProjectionFields,
            Map<String, DiscoverySortFieldConfiguration> sortFields, Set<String> hitHighlightingFields)
    {
        try
        {
            List<DiscoveryConfiguration> discoveryConfigurations = new ArrayList<DiscoveryConfiguration>();
            DiscoveryConfiguration generalConfiguration = SearchUtils
                    .getDiscoveryConfiguration();
            if (generalConfiguration != null)
            {
                discoveryConfigurations.add(generalConfiguration);
            }
            DiscoveryConfiguration globalConfiguration = SearchUtils
                    .getDiscoveryConfigurationByName(DiscoveryConfiguration.GLOBAL_CONFIGURATIONNAME);
            if (globalConfiguration != null && globalConfiguration.getId().equals(DiscoveryConfiguration.GLOBAL_CONFIGURATIONNAME))
            {
                discoveryConfigurations.add(globalConfiguration);
            }

            DiscoveryConfiguration crisConfiguration = SearchUtils
                    .getDiscoveryConfigurationByName(confName);
            if (crisConfiguration != null)
            {
                discoveryConfigurations.add(crisConfiguration);
            }
            
            List<String> listExtraConfiguration = SearchUtils.getConfigurationService().getExtraConfigurationMapping().get(confName);
            if (listExtraConfiguration != null) {
                for (String eConf : listExtraConfiguration) {
                    DiscoveryConfiguration extraCrisConfiguration = SearchUtils
                            .getDiscoveryConfigurationByName(eConf);
                    if (extraCrisConfiguration != null)
                    {
                        discoveryConfigurations.add(extraCrisConfiguration);
                    }
                }
            }
            
            for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations)
            {
                for (int i = 0; i < discoveryConfiguration.getSearchFilters()
                        .size(); i++)
                {
                    DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration
                            .getSearchFilters().get(i);
                    for (int j = 0; j < discoverySearchFilter
                            .getMetadataFields().size(); j++)
                    {
                        String metadataField = discoverySearchFilter
                                .getMetadataFields().get(j);
                        List<DiscoverySearchFilter> resultingList;
                        if (searchFilters.get(metadataField) != null)
                        {
                            resultingList = searchFilters.get(metadataField);
                        }
                        else
                        {
                            // New metadata field, create a new list for it
                            resultingList = new ArrayList<DiscoverySearchFilter>();
                        }
                        resultingList.add(discoverySearchFilter);

                        searchFilters.put(metadataField, resultingList);
                    }
                }

                DiscoverySortConfiguration sortConfiguration = discoveryConfiguration
                        .getSearchSortConfiguration();
                if (sortConfiguration != null)
                {
                    for (DiscoverySortFieldConfiguration discoverySortConfiguration : sortConfiguration
                            .getSortFields())
                    {
                        sortFields.put(
                                discoverySortConfiguration.getMetadataField(),
                                discoverySortConfiguration);
                    }
                }
        
				DiscoveryHitHighlightingConfiguration hitHighlightingConfiguration = discoveryConfiguration
						.getHitHighlightingConfiguration();
				if (hitHighlightingConfiguration != null) {
					List<DiscoveryHitHighlightFieldConfiguration> fieldConfigurations = hitHighlightingConfiguration
							.getMetadataFields();
					for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : fieldConfigurations) {
						hitHighlightingFields.add(fieldConfiguration.getField());
					}
				}
			}

            String ignoreFieldsString = new DSpace().getConfigurationService()
                    .getProperty("discovery.index.ignore");
            if (ignoreFieldsString != null)
            {
                if (ignoreFieldsString.contains(","))
                {
                    for (int i = 0; i < ignoreFieldsString.split(",").length; i++)
                    {
                        toIgnoreFields.add(ignoreFieldsString.split(",")[i]
                                .trim());
                    }
                }
                else
                {
                    toIgnoreFields.add(ignoreFieldsString);
                }
            }

            String projectionFieldsString = new DSpace()
                    .getConfigurationService().getProperty(
                            "discovery.index.projection");
            if (projectionFieldsString != null)
            {
                if (projectionFieldsString.indexOf(",") != -1)
                {
                    for (int i = 0; i < projectionFieldsString.split(",").length; i++)
                    {
                        toProjectionFields.add(projectionFieldsString
                                .split(",")[i].trim());
                    }
                }
                else
                {
                    toProjectionFields.add(projectionFieldsString);
                }
            }

        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    public <P extends ANestedProperty<TP>, TP extends ANestedPropertiesDefinition, PP extends Property<PTP>, PTP extends PropertiesDefinition> void unIndexContent(
            Object context, ACrisNestedObject<P, TP, PP, PTP> nested,
            boolean commit)
    {
        try
        {
            if (nested == null)
            {
                return;
            }
            String uniqueID = nested.getType() + "-" + nested.getID();
            getSolr().deleteById(uniqueID);
            if (commit)
            {
                getSolr().commit();
            }
        }
        catch (Exception exception)
        {
            log.error(exception.getMessage(), exception);
            emailException(exception);
        }
       }
        
        
        public void updateIndex(Context context, List<Integer> ids, boolean force, int type) {
            if (type >= CrisConstants.CRIS_TYPE_ID_START)
            {
                for (Integer id : ids)
                {
                    if (type >= CrisConstants.CRIS_DYNAMIC_TYPE_ID_START) {
                        indexCrisObject(getApplicationService().get(ResearchObject.class, id), force);
                    }                    
                    else if (CrisConstants.RP_TYPE_ID == type)
                    {
                        indexCrisObject(getApplicationService().get(ResearcherPage.class, id), force);
                    }
                    else if (CrisConstants.PROJECT_TYPE_ID == type)
                    {
                        indexCrisObject(getApplicationService().get(Project.class, id), force);
                    }
                    else if (CrisConstants.OU_TYPE_ID == type)
                    {
                        indexCrisObject(getApplicationService().get(OrganizationUnit.class, id), force);
                    }
                }
            }
            else
            {
                super.updateIndex(context, ids, force, type);
            }
        }

        public void renewMetricsCache() throws SearchServiceException
        {            
            SolrQuery solrQuery = new SolrQuery()
                    .setQuery("*:*");
            // Only return obj identifier fields in result doc
            solrQuery.setFields("clearcache-crismetrics");            
            search(solrQuery);
        }
}
