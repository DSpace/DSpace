/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.filter;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Define the set of use cases for filtering items of interest through the REST API.
 * @author Terry Brady, Georgetown University
 *
 */

public class ItemFilterDefs implements ItemFilterList {
    public static final String CAT_ITEM = "Item Property Filters";
    public static final String CAT_BASIC = "Basic Bitstream Filters";
    public static final String CAT_MIME = "Bitstream Filters by MIME Type";
    
    public static final String[] MIMES_PDF = {"application/pdf"};
    public static final String[] MIMES_JPG = {"image/jpeg"};
    
    
    private enum EnumItemFilterDefs implements ItemFilterTest {
        is_item("Is Item - always true", null, CAT_ITEM) {
            public boolean testItem(Context context, Item item) {
                return true;
            }        
        },
        is_withdrawn("Withdrawn Items", null, CAT_ITEM) {
            public boolean testItem(Context context, Item item) {
                return item.isWithdrawn();
            }        
        },
        is_not_withdrawn("Available Items - Not Withdrawn", null, CAT_ITEM) {
            public boolean testItem(Context context, Item item) {
                return !item.isWithdrawn();
            }        
        },
        is_discoverable("Discoverable Items - Not Private", null, CAT_ITEM) {
            public boolean testItem(Context context, Item item) {
                return item.isDiscoverable();
            }        
        },
        is_not_discoverable("Not Discoverable - Private Item", null, CAT_ITEM) {
            public boolean testItem(Context context, Item item) {
                return !item.isDiscoverable();
            }        
        },
        has_multiple_originals("Item has Multiple Original Bitstreams", null, CAT_BASIC) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.countOriginalBitstream(item) > 1;
            }        
        },
        has_no_originals("Item has No Original Bitstreams", null, CAT_BASIC) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.countOriginalBitstream(item) == 0;
            }        
        },
        has_one_original("Item has One Original Bitstream", null, CAT_BASIC) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.countOriginalBitstream(item) == 1;
            }        
        },
        has_doc_original("Item has a Doc Original Bitstream (PDF, Office, Text, HTML, XML, etc)", null, CAT_MIME) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes()) > 0;
            }
        },
        has_image_original("Item has an Image Original Bitstream", null, CAT_MIME) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image") > 0;
            }        
        },
        has_unsupp_type("Has Other Bitstream Types (not Doc or Image)", null, ItemFilterDefs.CAT_MIME) {
            public boolean testItem(Context context, Item item) {
                int bitCount = ItemFilterUtil.countOriginalBitstream(item);
                if (bitCount == 0) {
                    return false;
                }
                int docCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
                int imgCount = ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image");
                return (bitCount - docCount - imgCount) > 0;
            }        
        },
        has_mixed_original("Item has multiple types of Original Bitstreams (Doc, Image, Other)", null, CAT_MIME) {
            public boolean testItem(Context context, Item item) {
                int countBit = ItemFilterUtil.countOriginalBitstream(item);
                if (countBit <= 1) return false;
                int countDoc = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
                if (countDoc > 0) {
                    return countDoc != countBit;
                }
                int countImg = ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image");
                if (countImg > 0) {
                    return countImg != countBit;
                }
                return false;
            }            
        },
        has_pdf_original("Item has a PDF Original Bitstream", null, CAT_MIME) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.countOriginalBitstreamMime(context, item, MIMES_PDF) > 0;
            }        
        },
        has_jpg_original("Item has JPG Original Bitstream", null, CAT_MIME) {
            public boolean testItem(Context context, Item item) {
                return ItemFilterUtil.countOriginalBitstreamMime(context, item, MIMES_JPG) > 0;
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

    public ItemFilterDefs() {
    }
    public ItemFilterTest[] getFilters() {
        return EnumItemFilterDefs.values();
    }    
}
