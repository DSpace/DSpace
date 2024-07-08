/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/* Created for LINDAT/CLARIAH-CZ (UFAL) */

package org.dspace.xoai.filter;

import java.sql.SQLException;
import java.util.Objects;

import com.lyncode.xoai.dataprovider.core.ReferenceSet;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;

/**
 * Serves as filter in xoai for OAI-PMH interface.
 * Taken from
 * https://github.com/ufal/clarin-dspace/blob
 * /8780782ce2977d304f2390b745a98eaea00b8255
 * /dspace-oai/src/main/java/cz/cuni/mff/ufal/dspace/xoai/filter/ColComFilter.java
 */
public class ColComFilter extends DSpaceFilter {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ColComFilter.class);

    private DSpaceObject dso = null;

    private static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    private static CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    private static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    @Override
    public SolrFilterResult buildSolrQuery() {
        if (getDSpaceObject() != null) {
            /*
                -foo is transformed by solr into (*:* -foo) only if the top level query is a pure negative query
                bar OR (-foo) is not transformed; so we need bar OR (*:* -foo)
                bar comes from org.dspace.xoai.services.impl.xoai.BaseDSpaceFilterResolver#buildSolrQuery
             */
            String q = "*:* AND ";
            String setSpec = getSetSpec();
            if (dso.getType() == Constants.COLLECTION) {
                return new SolrFilterResult(q + "-item.collections:"
                        + ClientUtils.escapeQueryChars(setSpec));
            } else if (dso.getType() == Constants.COMMUNITY) {
                return new SolrFilterResult(q + "-item.communities:"
                        + ClientUtils.escapeQueryChars(setSpec));
            }
        }
        ;
        return new SolrFilterResult("*:*");
    }

    @Override
    public boolean isShown(DSpaceItem item) {
        if (getDSpaceObject() != null) {
            String setSpec = getSetSpec();
            for (ReferenceSet s : item.getSets()) {
                if (s.getSetSpec().equals(setSpec)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getSetSpec() {
        // Set prefix for the community as default value.
        String handlePrefix;
        if (dso instanceof Collection) {
            // Prefix for the Collection.
            handlePrefix = "col_";
        } else if (dso instanceof Community) {
            handlePrefix = "com_";
        } else {
            String message = "The DSO object must be of type Community or Collection.";
            log.error(message);
            throw new RuntimeException(message);
        }
        return handlePrefix + dso.getHandle().replace("/", "_");
    }

    private DSpaceObject getDSpaceObject() {
        if (Objects.nonNull(dso)) {
            return dso;
        }
        if (Objects.nonNull(getConfiguration().get("handle"))) {
            String handle = getConfiguration().get("handle").asSimpleType().asString();
            try {
                dso = handleService.resolveToObject(context, handle);
            } catch (SQLException e) {
                log.error(e);
            }
        } else if (Objects.nonNull(getConfiguration().get("name"))) {
            String name = getConfiguration().get("name").asSimpleType().asString();
            try {
                for (Community c : communityService.findAll(context)) {
                    if (name.equals(c.getName())) {
                        dso = c;
                        break;
                    }
                }
                if (Objects.isNull(dso)) {
                    for (Collection c : collectionService.findAll(context)) {
                        if (name.equals(c.getName())) {
                            dso = c;
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
        return dso;
    }
}
