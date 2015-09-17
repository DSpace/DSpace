/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 * GUDOE[[twb27:custom module]]
 */
package org.dspace.rest.filter;

import java.sql.SQLException;
import java.util.regex.Pattern;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.rest.filter.ItemFilterUtil.BundleName;

/**
 * Define the set of use cases for filtering items of interest through the REST API.
 * @author Terry Brady, Georgetown University
 *
 */

public class ItemFilterDefsGU implements ItemFilterList {
	public static final String CAT_GU = "GEORGETOWN Filters";
	private enum EnumItemFilterDefs implements ItemFilterTest {
	    has_markdown_description("Item has markdown description", null, CAT_GU) {
	        public boolean testItem(Context context, Item item) {
	        	return ItemFilterUtil.hasMetadataMatch(item, "dc.description", Pattern.compile("^\\[MD\\][\\s\\S]*$"));
	        }        
	    },
	    no_bookview("Bookview Suppressed", null, CAT_GU) {
	        public boolean testItem(Context context, Item item) {
	        	return ItemFilterUtil.countBitstreamByDesc(BundleName.ORIGINAL, item, "NO BOOKVIEW") > 0;
	        }        
	    },
	    gu_access("Original Bitstreams restricted to GU Community", null, CAT_GU) {
	        public boolean testItem(Context context, Item item) {
	            try {
	                for(Bundle bundle: item.getBundles("ORIGINAL")){
	                    for(Bitstream bit: bundle.getBitstreams()) {
	                    	for(Group grp: AuthorizeManager.getAuthorizedGroups(getAnonContext(), bit, org.dspace.core.Constants.READ)){
	                    		if (grp.getName().equals("All Georgetown Users Read")) {
	                    			return true;
	                    		}
	                    	}
	                    }
	                }
	            } catch (SQLException e) {
	            }
	            return false;
	        }
	    },
	    stream_auth_image("Original Bitstream with Description: Auth Image", null, CAT_GU) {
	        public boolean testItem(Context context, Item item) {
	        	return ItemFilterUtil.countBitstreamByDesc(BundleName.ORIGINAL, item, "Sharestream Auth Image") > 0;
	        }
	    },
	    stream_thumb("Original Bitstream with Description: Thumbnail", null, CAT_GU) {
	        public boolean testItem(Context context, Item item) {
	        	return ItemFilterUtil.countBitstreamByDesc(BundleName.ORIGINAL, item, "Sharestream Thumbnail") > 0;
	        }
	    },
	    ;
	    
		private static Context anonContext;
		private static Context getAnonContext() {
			if (anonContext == null) {
				try {
					anonContext = new Context();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			return anonContext;
		}
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

	public ItemFilterDefsGU() {
	}
	public ItemFilterTest[] getFilters() {
		return EnumItemFilterDefs.values();
	}	
}
