/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.statistics;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.UsageReportCategoryRest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Site;
import org.dspace.utils.DSpace;

/**
 * This class provides access to the configured reports and categories
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class StatisticsReportsConfiguration {

    private Map<String, List<UsageReportCategoryRest>> mapping;

    /**
     * Sets the mapping between DSO category keys (e.g. "site", "community", "item-&lt;entityType&gt;") and the list of
     * report categories available for each of them.
     *
     * @param mapping the category mapping keyed by DSO category
     */
    public void setMapping(Map<String, List<UsageReportCategoryRest>> mapping) {
        this.mapping = mapping;
    }

    /**
     * Returns the list of report categories configured for the given DSpace object, resolving the most specific
     * mapping available (including entity-type specific mappings for collections and items) and falling back to the
     * generic mapping for the object kind.
     *
     * @param dso the DSpace object the report categories are requested for
     * @return the matching list of report categories, or null if none is configured
     */
    public List<UsageReportCategoryRest> getCategories(DSpaceObject dso) {
        if (dso instanceof Site) {
            return mapping.get("site");
        } else if (dso instanceof Community) {
            return mapping.get("community");
        } else if (dso instanceof Collection) {
            String entityType = getEntityType(dso);
            if (StringUtils.isNotEmpty(entityType)) {
                List<UsageReportCategoryRest> result = mapping.get("collection-" + entityType);
                if (result != null) {
                    return result;
                }
            }
            return mapping.get("collection");
        } else if (dso instanceof Item) {
            Item item = (Item) dso;
            List<MetadataValue> metadatavalues = item.getItemService().getMetadataByMetadataString(item,
                    "dspace.entity.type");
            if (metadatavalues != null && metadatavalues.size() > 0) {
                String entityType = metadatavalues.get(0).getValue();
                List<UsageReportCategoryRest> result = mapping.get("item-" + entityType);
                if (result != null) {
                    return result;
                }
            }
            return mapping.get("item");
        } else if (dso instanceof Bitstream) {
            return mapping.get("bitstream");
        }
        return null;
    }

    private String getEntityType(DSpaceObject dso) {
        return dso.getMetadata()
                  .stream()
                  .filter(metadataValue ->
                      "dspace.entity.type".equals(metadataValue.getMetadataField().toString('.')))
                  .map(MetadataValue::getValue)
                  .findFirst()
                  .orElse("");
    }

    /**
     * Returns the report generator matching the given report id among the categories configured for the given DSO.
     *
     * @param dso      the DSpace object the report is requested for
     * @param reportId the id of the report whose generator is requested
     * @return the matching report generator, or null if none is found
     */
    public UsageReportGenerator getReportGenerator(DSpaceObject dso, String reportId) {
        return getCategories(dso).stream()
                                 .filter(x -> x.getReports().containsKey(reportId))
                                 .findFirst()
                                 .map(usageReportCategoryRest -> usageReportCategoryRest.getReports().get(reportId))
                                 .orElse(null);
    }

    /**
     * Looks up a report category by its configured bean name in the service manager.
     *
     * @param categoryId the bean name of the report category
     * @return the resolved report category, or null if no such bean exists
     */
    public UsageReportCategoryRest getCategory(String categoryId) {
        return new DSpace().getServiceManager().getServiceByName(categoryId, UsageReportCategoryRest.class);
    }
}
