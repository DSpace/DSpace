/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.filter;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.rest.filter.ItemFilterUtil.BundleName;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Define the set of use cases for filtering items of interest through the REST API.
 * @author Terry Brady, Georgetown University
 *
 */

public class ItemFilterDefsMisc implements ItemFilterList {
	public static final String CAT_MISC = "Bitstream Bundle Filters";
	public static final String CAT_MIME_SUPP = "Supported MIME Type Filters";
	private enum EnumItemFilterDefs implements ItemFilterTest {
	    has_only_supp_image_type("Item Image Bitstreams are Supported", null, CAT_MIME_SUPP) {
	        public boolean testItem(Context context, Item item) {
	        	int imageCount = ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image/");
	        	if (imageCount == 0) {
	        		return false;
	        	}
	        	int suppImageCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getSupportedImageMimeTypes());
	        	return (imageCount == suppImageCount);
	        }        
	    },
	    has_unsupp_image_type("Item has Image Bitstream that is Unsupported", null, CAT_MIME_SUPP) {
	        public boolean testItem(Context context, Item item) {
	        	int imageCount = ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image/");
	        	if (imageCount == 0) {
	        		return false;
	        	}
	        	int suppImageCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getSupportedImageMimeTypes());
	        	return (imageCount - suppImageCount) > 0;
	        }        
	    },
	    has_only_supp_doc_type("Item Document Bitstreams are Supported", null, CAT_MIME_SUPP) {
	        public boolean testItem(Context context, Item item) {
	        	int docCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
	        	if (docCount == 0) {
	        		return false;
	        	}
	        	int suppDocCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getSupportedDocumentMimeTypes());
	        	return docCount == suppDocCount;
	        }        
	    },
	    has_unsupp_doc_type("Item has Document Bitstream that is Unsupported", null, CAT_MIME_SUPP) {
	        public boolean testItem(Context context, Item item) {
	        	int docCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
	        	if (docCount == 0) {
	        		return false;
	        	}
	        	int suppDocCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getSupportedDocumentMimeTypes());
	        	return (docCount - suppDocCount) > 0;
	        }        
	    },
	    has_small_pdf("Has unusually small PDF", null, ItemFilterDefs.CAT_MIME) {
	        public boolean testItem(Context context, Item item) {
	        	return ItemFilterUtil.countBitstreamSmallerThanMinSize(context, BundleName.ORIGINAL, item, ItemFilterDefs.MIMES_PDF, "rest.report-pdf-min-size") > 0;
	        }        
	    },
	    has_large_pdf("Has unusually large PDF", null, ItemFilterDefs.CAT_MIME) {
	        public boolean testItem(Context context, Item item) {
	        	return ItemFilterUtil.countBitstreamLargerThanMaxSize(context, BundleName.ORIGINAL,  item, ItemFilterDefs.MIMES_PDF, "rest.report-pdf-max-size") > 0;
	        }        
	    },
	    has_unsupported_bundle("Has bitstream in an unsuppored bundle", null, CAT_MISC) {
	        public boolean testItem(Context context, Item item) {
	        	String[] bundleList = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("rest.report-supp-bundles");
	        	return ItemFilterUtil.hasUnsupportedBundle(item, bundleList);
	        }        
	    },
	    has_small_thumbnail("Has unusually small thumbnail", null, CAT_MISC) {
	        public boolean testItem(Context context, Item item) {
	        	return ItemFilterUtil.countBitstreamSmallerThanMinSize(context, BundleName.THUMBNAIL, item, ItemFilterDefs.MIMES_JPG, "rest.report-thumbnail-min-size") > 0;
	        }        
	    },
	    has_doc_without_text("Has document bitstream without TEXT item", null, ItemFilterDefs.CAT_MIME) {
	        public boolean testItem(Context context, Item item) {
	        	int countDoc = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
	        	if (countDoc == 0) {
	        		return false;
	        	}
	        	int countText = ItemFilterUtil.countBitstream(BundleName.TEXT, item);
	        	return countDoc > countText;
	        }        
	    },
	    has_original_without_thumbnail("Has original bitstream without thumbnail", null, CAT_MISC) {
	        public boolean testItem(Context context, Item item) {
	        	int countBit = ItemFilterUtil.countOriginalBitstream(item);
	        	if (countBit == 0) {
	        		return false;
	        	}
	        	int countThumb = ItemFilterUtil.countBitstream(BundleName.THUMBNAIL, item);
	        	return countBit > countThumb;
	        }        
	    },
	    has_invalid_thumbnail_name("Has invalid thumbnail name (assumes one thumbnail for each original)", null, CAT_MISC) {
	        public boolean testItem(Context context, Item item) {
	        	List<String> originalNames = ItemFilterUtil.getBitstreamNames(BundleName.ORIGINAL, item);
	        	List<String> thumbNames = ItemFilterUtil.getBitstreamNames(BundleName.THUMBNAIL, item);
	        	if (thumbNames.size() != originalNames.size()) {
	        		return false;
	        	}
	        	for(String name: originalNames) {
	        		if (!thumbNames.contains(name+".jpg")) {
	        			return true;
	        		}
	        	}
	        	return false;
	        }        
	    },
	    has_non_generated_thumb("Has non generated thumbnail", null, CAT_MISC) {
	        public boolean testItem(Context context, Item item) {
	        	String[] generatedThumbDesc = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("rest.report-gen-thumbnail-desc");
	        	int countThumb = ItemFilterUtil.countBitstream(BundleName.THUMBNAIL, item);
	        	if (countThumb == 0) {
	        		return false;
	        	}
	        	int countGen = ItemFilterUtil.countBitstreamByDesc(BundleName.THUMBNAIL, item, generatedThumbDesc);
	        	return (countThumb > countGen);
	        }        
	    },
	    no_license("Doesn't have a license", null, CAT_MISC) {
	        public boolean testItem(Context context, Item item) {
	        	return ItemFilterUtil.countBitstream(BundleName.LICENSE, item) == 0;
	        }        
	    },
	    has_license_documentation("Has documentation in the license bundle", null, CAT_MISC) {
	        public boolean testItem(Context context, Item item) {
	        	List<String> names = ItemFilterUtil.getBitstreamNames(BundleName.LICENSE, item);
	        	for(String name: names) {
	        		if (!name.equals("license.txt")) {
	        			return true;
	        		}
	        	}
	        	return false;
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

	public ItemFilterDefsMisc() {
	}
	public ItemFilterTest[] getFilters() {
		return EnumItemFilterDefs.values();
	}	
}
