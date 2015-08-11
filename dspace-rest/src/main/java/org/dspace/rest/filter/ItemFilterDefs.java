package org.dspace.rest.filter;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;

public enum ItemFilterDefs implements ItemFilterTest {
    is_not_withdrawn("Withdrawn Items") {
        public boolean testItem(Context context, Item item) {
            return !item.isWithdrawn();
        }        
    },
    is_withdrawn("Available Items - Not Withdrawn") {
        public boolean testItem(Context context, Item item) {
            return item.isWithdrawn();
        }        
    },
    is_discoverable("Discoverable Items - Not Private") {
        public boolean testItem(Context context, Item item) {
            return item.isDiscoverable();
        }        
    },
    is_not_discoverable("Private Items - Not Discoverable") {
        public boolean testItem(Context context, Item item) {
            return !item.isDiscoverable();
        }        
    },
    has_multiple_originals("Item has Multiple Originals") {
        public boolean testItem(Context context, Item item) {
            int count = 0;
            try {
                for(Bundle bundle: item.getBundles("ORIGINAL")){
                    count += bundle.getBitstreams().length;
                }
            } catch (SQLException e) {
            }
            return count > 1;
        }        
    },
    has_no_originals("Item has No Original Bitstreams") {
        public boolean testItem(Context context, Item item) {
            int count = 0;
            try {
                for(Bundle bundle: item.getBundles("ORIGINAL")){
                    count += bundle.getBitstreams().length;
                }
            } catch (SQLException e) {
            }
            return count == 0;
        }        
    },
    has_one_original("Item has One Original Bitstream") {
        public boolean testItem(Context context, Item item) {
            int count = 0;
            try {
                for(Bundle bundle: item.getBundles("ORIGINAL")){
                    count += bundle.getBitstreams().length;
                }
            } catch (SQLException e) {
            }
            return count == 1;
        }        
    },
    has_restricted_original("Item has Restricted Original", "Item has at least one original bitstream that is not accessible to Anonymous user") {
        public boolean testItem(Context context, Item item) {
            try {
                for(Bundle bundle: item.getBundles("ORIGINAL")){
                    for(Bitstream bit: bundle.getBitstreams()) {
                        if (!AuthorizeManager.authorizeActionBoolean(context, bit, org.dspace.core.Constants.READ)) {
                            return true;
                        }
                    }
                }
            } catch (SQLException e) {
            }
            return false;
        }        
    },
    has_pdf_original("Item has PDF Original") {
        public boolean testItem(Context context, Item item) {
            try {
                for(Bundle bundle: item.getBundles("ORIGINAL")){
                    for(Bitstream bit: bundle.getBitstreams()) {
                        if (bit.getFormat().getMIMEType().equals("application/pdf")) {
                            return true;
                        }
                    }
                }
            } catch (SQLException e) {
            }
            return false;
        }        
    },
    has_image_original("Item has Image Original") {
        public boolean testItem(Context context, Item item) {
            try {
                for(Bundle bundle: item.getBundles("ORIGINAL")){
                    for(Bitstream bit: bundle.getBitstreams()) {
                        if (bit.getFormat().getMIMEType().startsWith("image")) {
                            return true;
                        }
                    }
                }
            } catch (SQLException e) {
            }
            return false;
        }        
    },
    has_jpg_original("Item has JPG Original") {
        public boolean testItem(Context context, Item item) {
            try {
                for(Bundle bundle: item.getBundles("ORIGINAL")){
                    for(Bitstream bit: bundle.getBitstreams()) {
                        if (bit.getFormat().getMIMEType().equals("image/jpeg")) {
                            return true;
                        }
                    }
                }
            } catch (SQLException e) {
            }
            return false;
        }        
    },
    ;
    
    private String title = null;
    private String description = null;
    private ItemFilterDefs(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    private ItemFilterDefs(String title) {
        this(title, null);
    }
    
    private ItemFilterDefs() {
        this(null, null);
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
}
