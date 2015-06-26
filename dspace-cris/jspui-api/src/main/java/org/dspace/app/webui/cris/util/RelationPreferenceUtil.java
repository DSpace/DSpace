/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.configuration.ColumnVisualizationConfiguration;
import org.dspace.app.cris.configuration.RelationPreferenceConfiguration;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.webui.cris.dto.RelatedObject;
import org.dspace.app.webui.cris.dto.RelatedObjects;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class RelationPreferenceUtil
{

    private static final String UNLINKED_FILTER = "({2}) OR "
            + RelationPreference.PREFIX_RELATIONPREFERENCES + "{0}" + "."
            + RelationPreference.UNLINKED.toLowerCase() + ":\"" + "{1}" + "\"";

    private static final String SELECTED_FILTER = "-"
            + RelationPreference.PREFIX_RELATIONPREFERENCES + "{0}" + "."
            + RelationPreference.SELECTED.toLowerCase() + ":\"" + "{1}" + "\"";

    public static String HIDDEN_FILTER = "-"
            + RelationPreference.PREFIX_RELATIONPREFERENCES + "{0}" + "."
            + RelationPreference.HIDED.toLowerCase() + ":\"" + "{1}" + "\"";

    private DSpace dspace = new DSpace();

    private ApplicationService applicationService = dspace.getServiceManager()
            .getServiceByName("applicationService", ApplicationService.class);

    private CrisSearchService searcher = dspace.getServiceManager()
            .getServiceByName(SearchService.class.getName(),
                    CrisSearchService.class);

    private RelationPreferenceService preferenceService = dspace
            .getServiceManager().getServiceByName(
                    RelationPreferenceService.class.getName(),
                    RelationPreferenceService.class);

    public List<RelatedObject> getSelected(Context context, ACrisObject cris,
            String relationType)
    {
        List<RelatedObject> related = new ArrayList<RelatedObject>();
        String configurationName = preferenceService.getConfigurationName(cris,
                relationType);
        List<RelationPreference> relations = applicationService
                .findSelectedRelationsPreferencesOfUUID(cris.getUuid(),
                        configurationName);
        List<DSpaceObject> dsoList = new ArrayList<DSpaceObject>();
        for (RelationPreference rp : relations)
        {
            if (rp.getTargetUUID() == null)
            {
                try
                {
                    dsoList.add(Item.find(context, rp.getItemID()));
                }
                catch (SQLException e)
                {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            else
            {
                dsoList.add(applicationService.getEntityByUUID(rp
                        .getTargetUUID()));
            }
        }

        RelationPreferenceConfiguration configuration = preferenceService
                .getConfigurationService().getRelationPreferenceConfiguration(
                        configurationName);
        for (DSpaceObject dso : dsoList)
        {
            related.add(convert(context, dso, configuration,
                    RelationPreference.SELECTED));
        }
        return related;
    }

    public RelatedObjects getRelatedObject(Context context, ACrisObject cris,
            String relationType, String userQuery, String status,
            List<Sort> sorts, int rpp, int offset)
            throws SearchServiceException, SQLException
    {
        String uuid = cris.getUuid();
        String configurationName = preferenceService.getConfigurationName(cris,
                relationType);
        RelationPreferenceConfiguration configuration = preferenceService
                .getConfigurationService().getRelationPreferenceConfiguration(
                        configurationName);
        List<ColumnVisualizationConfiguration> columns = configuration
                .getColumnsVisualizationConfiguration();
        RelatedObjects result = new RelatedObjects();
        List<RelatedObject> related = result.getObjects();

        String query = MessageFormat.format(configuration
                .getRelationConfiguration().getQuery(), cris.getCrisID(), cris
                .getUuid());

        boolean sysAdmin = AuthorizeManager.isAdmin(context);

        SolrQuery solrQuery = new SolrQuery();
        if (StringUtils.isNotEmpty(userQuery))
        {
            solrQuery.addFilterQuery("{!tag=user}"
                    + ClientUtils.escapeQueryChars(userQuery) + "*");
            solrQuery.addFacetQuery("{!ex=user}*:*");
        }

        if (!configuration.isActionEnabled(RelationPreference.HIDED, sysAdmin))
        {
            solrQuery.addFilterQuery(getHiddenFilter(cris, configurationName));
        }
        if (!configuration.isActionEnabled(RelationPreference.SELECTED,
                sysAdmin))
        {
            solrQuery
                    .addFilterQuery(getSelectedFilter(cris, configurationName));
        }
        if (configuration
                .isActionEnabled(RelationPreference.UNLINKED, sysAdmin))
        {
            query = getUnlinkedFilter(cris, configurationName, query);
        }

        solrQuery.setQuery(query);

        if (StringUtils.isNotEmpty(status))
        {

            solrQuery
                    .addFilterQuery(RelationPreference.PREFIX_RELATIONPREFERENCES
                            + configurationName
                            + "."
                            + status.toLowerCase()
                            + ":\"" + cris.getUuid() + "\"");

        }

        if (sorts != null)
        {
            for (Sort s : sorts)
            {
                if (s.col < 0)
                {
                    solrQuery.addSortField("cris-uuid", s.asc ? ORDER.asc
                            : ORDER.desc);
                    solrQuery.addSortField("search.resourceid",
                            s.asc ? ORDER.asc : ORDER.desc);
                }
                else
                {
                    solrQuery.addSortField(columns.get(s.col).getSortField(),
                            s.asc ? ORDER.asc : ORDER.desc);
                }
            }
        }

        solrQuery.setRows(rpp);
        solrQuery.setStart(offset);
        solrQuery
                .setFields("search.resourceid", "cris-uuid",
                        RelationPreference.PREFIX_RELATIONPREFERENCES
                                + configurationName + "."
                                + RelationPreference.UNLINKED,
                        RelationPreference.PREFIX_RELATIONPREFERENCES
                                + configurationName + "."
                                + RelationPreference.HIDED,
                        RelationPreference.PREFIX_RELATIONPREFERENCES
                                + configurationName + "."
                                + RelationPreference.SELECTED);
        QueryResponse qRes = searcher.search(solrQuery);
        result.setFilterRecords((int) qRes.getResults().getNumFound());
        if (StringUtils.isNotEmpty(userQuery))
        {
            result.setTotalRecords(qRes.getFacetQuery().get("{!ex=user}*:*"));
        }
        else
        {
            result.setTotalRecords((int) qRes.getResults().getNumFound());
        }
        List<Object[]> dsoList = new ArrayList<Object[]>();
        for (SolrDocument doc : qRes.getResults())
        {
            Collection<Object> unlinked = doc
                    .getFieldValues(RelationPreference.PREFIX_RELATIONPREFERENCES
                            + configurationName
                            + "."
                            + RelationPreference.UNLINKED);
            Collection<Object> hided = doc
                    .getFieldValues(RelationPreference.PREFIX_RELATIONPREFERENCES
                            + configurationName
                            + "."
                            + RelationPreference.HIDED);
            Collection<Object> selected = doc
                    .getFieldValues(RelationPreference.PREFIX_RELATIONPREFERENCES
                            + configurationName
                            + "."
                            + RelationPreference.SELECTED);
            String relStatus = null;
            if (unlinked != null && unlinked.contains(uuid))
            {
                relStatus = RelationPreference.UNLINKED;
            }
            else if (hided != null && hided.contains(uuid))
            {
                relStatus = RelationPreference.HIDED;
            }
            else if (selected != null && selected.contains(uuid))
            {
                relStatus = RelationPreference.SELECTED;
            }
            if (doc.getFieldValue("cris-uuid") == null)
            {
                try
                {
                    dsoList.add(new Object[] {
                            Item.find(context, (Integer) doc
                                    .getFieldValue("search.resourceid")),
                            relStatus });
                }
                catch (SQLException e)
                {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            else
            {
                dsoList.add(new Object[] {
                        applicationService.getEntityByUUID((String) doc
                                .getFieldValue("cris-uuid")), relStatus });
            }
        }
        for (Object[] dso : dsoList)
        {
            related.add(convert(context, (DSpaceObject) dso[0], configuration,
                    (String) dso[1]));
        }
        return result;
    }

    private String getUnlinkedFilter(ACrisObject cris,
            String configurationName, String query)
    {
        return MessageFormat.format(UNLINKED_FILTER, configurationName,
                cris.getUuid(), query);
    }

    private String getSelectedFilter(ACrisObject cris, String configurationName)
    {
        return MessageFormat.format(SELECTED_FILTER, configurationName,
                cris.getUuid());
    }

    public static String getHiddenFilter(ACrisObject cris,
            String configurationName)
    {
        return MessageFormat.format(HIDDEN_FILTER, configurationName,
                cris.getUuid());
    }

    private RelatedObject convert(Context context, DSpaceObject dso,
            RelationPreferenceConfiguration configuration, String status)
    {
        RelatedObject rel = new RelatedObject();
        rel.setUuid(dso instanceof ACrisObject ? ((ACrisObject) dso).getUuid()
                : String.valueOf(dso.getID()));
        rel.setRelationPreference(status);
        ArrayList<String> descriptionColumns = new ArrayList<String>();
        rel.setDescriptionColumns(descriptionColumns);
        for (ColumnVisualizationConfiguration vis : configuration
                .getColumnsVisualizationConfiguration())
        {
            descriptionColumns
                    .add(vis.getHTMLContent(dso instanceof ACrisObject ? (ACrisObject) dso
                            : (Item) dso));
        }
        return rel;
    }

    public static class Sort
    {
        public int col;

        public boolean asc;
    }
}
