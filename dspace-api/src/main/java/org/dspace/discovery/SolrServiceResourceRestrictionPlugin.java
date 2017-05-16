/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Restriction plugin that ensures that indexes all the resource policies. When
 * a search is performed extra filter queries are added to retrieve only results
 * to which the user has READ access
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SolrServiceResourceRestrictionPlugin
        implements SolrServiceIndexPlugin, SolrServiceSearchPlugin
{

    private static final Logger log = Logger
            .getLogger(SolrServiceResourceRestrictionPlugin.class);

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        List<String> added = new ArrayList<String>();
        try
        {
            List<ResourcePolicy> policies = AuthorizeManager
                    .getPoliciesActionFilter(context, dso, Constants.READ);
            for (ResourcePolicy resourcePolicy : policies)
            {
                String fieldValue;
                if (resourcePolicy.getGroupID() != -1)
                {
                    // We have a group add it to the value
                    fieldValue = "g" + resourcePolicy.getGroupID();
                }
                else
                {
                    // We have an eperson add it to the value
                    fieldValue = "e" + resourcePolicy.getEPersonID();

                }
                added.add(fieldValue);
                document.addField("read", fieldValue);
            }

            if (dso.getType() == Constants.ITEM)
            {
                String name = dso.getParentObject().getParentObject().getName();
                if ("Central Managed Data".equals(name))
                {   
                        String collName = dso.getParentObject().getName();
                        if ("Publications".equals(collName)
                                || "Result Reports".equals(collName))
                        {
                            String metadatum = dso
                                    .getMetadata("dc.description.sponsorship");
                            if (StringUtils.isNotBlank(metadatum))
                            {
                                TableRow tr = DatabaseManager.querySingle(context,
                                        "select resource_id from metadatavalue mdv join community c on mdv.resource_id = c.community_id where mdv.resource_type_id = 4 and mdv.text_value = ?",
                                        metadatum);

                                if (tr != null)
                                {
                                    int commID = tr.getIntColumn("resource_id");
                                    String shortDesc = Community
                                            .find(context, commID)
                                            .getMetadata("short_description");

                                    // automatism to build READ policy for
                                    // <shortname-institute>-READER at
                                    // Publication
                                    // because this collection is part of
                                    // Central
                                    // Managed Data
                                    if ("Publications".equals(collName))
                                    {
                                        Group groupBasicReader = Group
                                                .findByName(context,
                                                        shortDesc + "-READER");
                                        String value = "g"
                                                + groupBasicReader.getID();
                                        if (!added.contains(value))
                                        {
                                            document.addField("read", value);
                                        }

                                        for (ResourcePolicy rr : policies)
                                        {
                                            if (rr.getAction() == Constants.READ
                                                    && rr.getGroupID() == groupBasicReader
                                                            .getID())
                                            {
                                                AuthorizeManager
                                                        .removeGroupPolicies(
                                                                context, dso,
                                                                groupBasicReader);
                                            }
                                        }
                                        AuthorizeManager.addPolicy(context, dso,
                                                Constants.READ,
                                                groupBasicReader,
                                                ResourcePolicy.TYPE_CUSTOM);
                                    }

                                    // automatism to build READ policy for
                                    // <shortname-institute>-RESULTREPORT-READER
                                    // at
                                    // Result Report because this collection is
                                    // part
                                    // of Central Managed Data
                                    if ("Result Reports".equals(collName))
                                    {
                                        Group groupResultReportReader = Group
                                                .findByName(context, shortDesc
                                                        + "-RESULTREPORT-READER");
                                        String value = "g"
                                                + groupResultReportReader
                                                        .getID();
                                        if (!added.contains(value))
                                        {
                                            document.addField("read", value);
                                        }

                                        for (ResourcePolicy rr : policies)
                                        {
                                            if (rr.getAction() == Constants.READ
                                                    && rr.getGroupID() == groupResultReportReader
                                                            .getID())
                                            {
                                                AuthorizeManager
                                                        .removeGroupPolicies(
                                                                context, dso,
                                                                groupResultReportReader);
                                            }
                                        }
                                        AuthorizeManager.addPolicy(context, dso,
                                                Constants.READ,
                                                groupResultReportReader,
                                                ResourcePolicy.TYPE_CUSTOM);

                                        Item item = (Item) dso;
                                        for (Bundle bundle : item
                                                .getBundles("ORIGINAL"))
                                        {
                                            bundle.replaceAllBitstreamPolicies(
                                                    new ArrayList<ResourcePolicy>());
                                            for (Bitstream bitstream : bundle
                                                    .getBitstreams())
                                            {
                                                AuthorizeManager.addPolicy(context,
                                                        bitstream,
                                                        Constants.READ,
                                                        groupResultReportReader,
                                                        ResourcePolicy.TYPE_CUSTOM);
                                            }
                                        }
                                    }
                                }
                            }
                            context.getDBConnection().commit();
                        }
                }
            }
        }
        catch (SQLException | AuthorizeException e)
        {
            log.error(LogManager.getHeader(context,
                    "Error while indexing resource policies",
                    "DSpace object: (id " + dso.getID() + " type "
                            + dso.getType() + ")"));
        }
    }

    @Override
    public void additionalSearchParameters(Context context,
            DiscoverQuery discoveryQuery, SolrQuery solrQuery)
    {
        try
        {
            if (context != null && !AuthorizeManager.isAdmin(context))
            {

                EPerson currentUser = context.getCurrentUser();
                StringBuilder resourceQuery = new StringBuilder();

                if (currentUser != null)
                {
                    if (currentUser.getEmail().contains("corporate"))
                    {
                        // NOTHING TODO
                    }
                    else
                    {
                        // Always add the anonymous group id to the query
                        resourceQuery.append("read:(g0");
                        if (currentUser != null)
                        {
                            resourceQuery.append(" OR e")
                                    .append(currentUser.getID());
                        }
                        // Retrieve all the groups the current user is a member
                        // of !
                        Set<Integer> groupIds = Group.allMemberGroupIDs(context,
                                currentUser);
                        for (Integer groupId : groupIds)
                        {
                            Group group = Group.find(context, groupId);
                            if (group != null)
                            {
                                if (!group.isNotRelevant())
                                {
                                    resourceQuery.append(" OR g")
                                            .append(groupId);
                                }
                            }
                        }

                        resourceQuery.append(")");
                    }
                }
                solrQuery.addFilterQuery(resourceQuery.toString());
            }
        }
        catch (SQLException e)
        {
            log.error(LogManager.getHeader(context,
                    "Error while adding resource policy information to query",
                    ""), e);
        }
    }

}
