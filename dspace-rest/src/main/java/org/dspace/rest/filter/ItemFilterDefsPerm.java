/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.filter;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.rest.filter.ItemFilterUtil.BundleName;

/**
 * Define the set of use cases for filtering items of interest through the REST API.
 * @author Terry Brady, Georgetown University
 *
 */
public class ItemFilterDefsPerm implements ItemFilterList {
    protected static AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	public static final String CAT_PERM = "Perimission Filters";
	private static Logger log = Logger.getLogger(ItemFilterDefsPerm.class);
	public ItemFilterDefsPerm(){
	}
	public enum EnumItemFilterPermissionDefs implements ItemFilterTest {
	    has_restricted_original("Item has Restricted Original Bitstream", 
	    		"Item has at least one original bitstream that is not accessible to Anonymous user", CAT_PERM) {
	        public boolean testItem(Context context, Item item) {
	            try {
	                for(Bundle bundle: item.getBundles()){
	                	if (!bundle.getName().equals(BundleName.ORIGINAL.name())) {
	                		continue;
	                	}
	                    for(Bitstream bit: bundle.getBitstreams()) {
	                        if (!authorizeService.authorizeActionBoolean(getAnonContext(), bit, org.dspace.core.Constants.READ)) {
	                            return true;
	                        }
	                    }
	                }
	            } catch (SQLException e) {
	            	ItemFilterDefsPerm.log.warn("SQL Exception testing original bitstream access " + e.getMessage(), e);
	            }
	            return false;
	        }        
	    },
	    has_restricted_thumbnail("Item has Restricted Thumbnail", 
	    		"Item has at least one thumbnail that is not accessible to Anonymous user", CAT_PERM) {
	        public boolean testItem(Context context, Item item) {
	            try {
	                for(Bundle bundle: item.getBundles()){
	                	if (!bundle.getName().equals(BundleName.THUMBNAIL.name())) {
	                		continue;
	                	}
	                    for(Bitstream bit: bundle.getBitstreams()) {
	                        if (!authorizeService.authorizeActionBoolean(getAnonContext(), bit, org.dspace.core.Constants.READ)) {
	                            return true;
	                        }
	                    }
	                }
	            } catch (SQLException e) {
	            	ItemFilterDefsPerm.log.warn("SQL Exception testing thumbnail bitstream access " + e.getMessage(), e);
	            }
	            return false;
	        }        
	    },
	    has_restricted_metadata("Item has Restricted Metadata", 
	    		"Item has metadata that is not accessible to Anonymous user", CAT_PERM) {
	        public boolean testItem(Context context, Item item) {
	        	try {
					return !authorizeService.authorizeActionBoolean(getAnonContext(), item, org.dspace.core.Constants.READ);
				} catch (SQLException e) {
	            	ItemFilterDefsPerm.log.warn("SQL Exception testing item metadata access " + e.getMessage(), e);
					return false;
				}
	        }        
	    },
	    ;
	    
		private static Context anonContext;
		private static Context getAnonContext() {
			if (anonContext == null) {
				anonContext = new Context();
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