/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.filter;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Define the set of use cases for filtering items of interest through the REST API.
 * @author Terry Brady, Georgetown University
 *
 */
public class ItemFilterDefsPerm implements ItemFilterList {
	public static final String CAT_PERM = "Perimission Filters";
	public ItemFilterDefsPerm(){
	}
	public enum EnumItemFilterPermissionDefs implements ItemFilterTest {
	    has_restricted_original("Item has Restricted Original Bitstream", 
	    		"Item has at least one original bitstream that is not accessible to Anonymous user", CAT_PERM) {
	        public boolean testItem(Context context, Item item) {
	            try {
	                for(Bundle bundle: item.getBundles("ORIGINAL")){
	                    for(Bitstream bit: bundle.getBitstreams()) {
	                        if (!AuthorizeManager.authorizeActionBoolean(getAnonContext(), bit, org.dspace.core.Constants.READ)) {
	                            return true;
	                        }
	                    }
	                }
	            } catch (SQLException e) {
	            }
	            return false;
	        }        
	    },
	    has_restricted_thumbnail("Item has Restricted Thumbnail", 
	    		"Item has at least one thumbnail that is not accessible to Anonymous user", CAT_PERM) {
	        public boolean testItem(Context context, Item item) {
	            try {
	                for(Bundle bundle: item.getBundles("THUMBNAIL")){
	                    for(Bitstream bit: bundle.getBitstreams()) {
	                        if (!AuthorizeManager.authorizeActionBoolean(getAnonContext(), bit, org.dspace.core.Constants.READ)) {
	                            return true;
	                        }
	                    }
	                }
	            } catch (SQLException e) {
	            }
	            return false;
	        }        
	    },
	    has_restricted_metadata("Item has Restricted Metadata", 
	    		"Item has metadata that is not accessible to Anonymous user", CAT_PERM) {
	        public boolean testItem(Context context, Item item) {
	        	try {
					return !AuthorizeManager.authorizeActionBoolean(getAnonContext(), item, org.dspace.core.Constants.READ);
				} catch (SQLException e) {
					e.printStackTrace();
					return false;
				}
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
	    private EnumItemFilterPermissionDefs(String title, String description, String category) {
	        this.title = title;
	        this.description = description;
	        this.category = category;
	    }
	    
	    private EnumItemFilterPermissionDefs() {
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

	@Override
	public ItemFilterTest[] getFilters() {
		return EnumItemFilterPermissionDefs.values();
	}
}