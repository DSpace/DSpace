/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.filter;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Define the set of use cases for filtering items of interest through the REST API.
 * @author Terry Brady, Georgetown University
 *
 */

public class ItemFilterDefsMeta implements ItemFilterList {
    protected static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    static Logger log = Logger.getLogger(ItemFilterDefsMeta.class);

    public static final String CAT_META_GEN = "General Metadata Filters";
    public static final String CAT_META_SPEC = "Specific Metadata Filters";
    public static final String CAT_MOD = "Recently Modified";
    private enum EnumItemFilterDefs implements ItemFilterTest {
        has_no_title("Has no dc.title", null, CAT_META_SPEC) {
            public boolean testItem(Context context, Item item) {
                return itemService.getMetadataByMetadataString(item, "dc.title").size() == 0;
            }
        },
        has_no_uri("Has no dc.identifier.uri", null, CAT_META_SPEC) {
            public boolean testItem(Context context, Item item) {
                return itemService.getMetadataByMetadataString(item, "dc.identifier.uri").size() == 0;
            }
        },
        has_mult_uri("Has multiple dc.identifier.uri", null, CAT_META_SPEC) {
            public boolean testItem(Context context, Item item) {
                return itemService.getMetadataByMetadataString(item, "dc.identifier.uri").size() > 1;
            }
        },
        has_compound_subject("Has compound subject", null, CAT_META_SPEC) {
            public boolean testItem(Context context, Item item) {
                String regex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("rest.report-regex-compound-subject");
                return ItemFilterUtil.hasMetadataMatch(item, "dc.subject.*", Pattern.compile(regex));
            }
        },
        has_compound_author("Has compound author", null, CAT_META_SPEC) {
            public boolean testItem(Context context, Item item) {
                String regex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("rest.report-regex-compound-author");
                return ItemFilterUtil.hasMetadataMatch(item, "dc.creator,dc.contributor.author", Pattern.compile(regex));
            }
        },
        has_empty_metadata("Has empty metadata", null, CAT_META_GEN) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.hasMetadataMatch(item, "*", Pattern.compile("^\\s*$"));
            }
        },
        has_unbreaking_metadata("Has unbreaking metadata", null, CAT_META_GEN) {
            public boolean testItem(Context context, Item item) {
                String regex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("rest.report-regex-unbreaking");
                return ItemFilterUtil.hasMetadataMatch(item, "*", Pattern.compile(regex));
            }
        },
        has_long_metadata("Has long metadata field", null, CAT_META_GEN) {
            public boolean testItem(Context context, Item item) {
                String regex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("rest.report-regex-long");
                return ItemFilterUtil.hasMetadataMatch(item, "*", Pattern.compile(regex));
            }
        },
        has_xml_entity("Has XML entity in metadata", null, CAT_META_GEN) {
            public boolean testItem(Context context, Item item) {
                String regex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("rest.report-regex-xml-entity");
                return ItemFilterUtil.hasMetadataMatch(item, "*", Pattern.compile(regex));
            }
        },
        has_non_ascii("Has non-ascii in metadata", null, CAT_META_GEN) {
            public boolean testItem(Context context, Item item) {
                String regex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("rest.report-regex-non-ascii");
                return ItemFilterUtil.hasMetadataMatch(item, "*", Pattern.compile(regex));
            }
        },
        has_desc_url("Has url in description", null, CAT_META_SPEC) {
            public boolean testItem(Context context, Item item) {
                String regex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("rest.report-regex-url");
                return ItemFilterUtil.hasMetadataMatch(item, "dc.description.*", Pattern.compile(regex));
            }
        },
        has_fulltext_provenance("Has fulltext in provenance", null, CAT_META_SPEC) {
            public boolean testItem(Context context, Item item) {
                String regex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("rest.report-regex-fulltext");
                return ItemFilterUtil.hasMetadataMatch(item, "dc.description.provenance", Pattern.compile(regex));
            }
        },
        no_fulltext_provenance("Doesn't have fulltext in provenance", null, CAT_META_SPEC) {
            public boolean testItem(Context context, Item item) {
                String regex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("rest.report-regex-fulltext");
                return !ItemFilterUtil.hasMetadataMatch(item, "dc.description.provenance", Pattern.compile(regex));
            }
        },
        mod_last_day("Modified in last 1 day", null, CAT_MOD) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.recentlyModified(item, 1);
            }
        },
        mod_last_7_days("Modified in last 7 days", null, CAT_MOD) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.recentlyModified(item, 7);
            }
        },
        mod_last_30_days("Modified in last 30 days", null, CAT_MOD) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.recentlyModified(item, 30);
            }
        },
        mod_last_90_days("Modified in last 60 days", null, CAT_MOD) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.recentlyModified(item, 60);
            }
        },
        ;

        private String title = null;
        private String description = null;
        private EnumItemFilterDefs(String title, String description, String category) {
            this.title = title;
            this.description = description;
            this.category = category;
        }

        private EnumItemFilterDefs() {
            this(null, null, null);
        }

        public String getName() {
            return name();
        }
        public String getTitle() {
            return title;
        }
        public String getDescription() {
            return description;
        }

        private String category = null;
        public String getCategory() {
            return category;
        }
    }

    public ItemFilterDefsMeta() {
    }
    public ItemFilterTest[] getFilters() {
        return EnumItemFilterDefs.values();
    }
}
