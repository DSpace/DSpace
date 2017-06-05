/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
 package org.dspace.rest.filter;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;

import com.ibm.icu.util.Calendar;

public class ItemFilterUtil {
    static Logger log = Logger.getLogger(ItemFilterUtil.class);
	public enum BundleName{ORIGINAL,TEXT,LICENSE,THUMBNAIL;}
	
	static String getDocumentMimeTypes() {
		return ConfigurationManager.getProperty("rest", "rest-report-mime-document");
	}
	
	static String getSupportedDocumentMimeTypes() {
		return ConfigurationManager.getProperty("rest", "rest-report-mime-document-supported");
	}

	static String getSupportedImageMimeTypes() {
		return ConfigurationManager.getProperty("rest", "rest-report-mime-document-image");
	}

	static int countOriginalBitstream(Item item) {
		return countBitstream(BundleName.ORIGINAL, item);
	}
	static int countBitstream(BundleName bundleName, Item item) {
		int count = 0;
        try {
            for(Bundle bundle: item.getBundles(bundleName.name())){
            	count += bundle.getBitstreams().length;
            }
        } catch (SQLException e) {
        }
		return count;
	}

	static List<String> getBitstreamNames(BundleName bundleName, Item item) {
		ArrayList<String> names = new ArrayList<String>();
        try {
            for(Bundle bundle: item.getBundles(bundleName.name())){
            	for(Bitstream bit: bundle.getBitstreams()) {
            		names.add(bit.getName());
            	}
            }
        } catch (SQLException e) {
        }
		return names;
	}

	
	static int countOriginalBitstreamMime(Item item, String mimeList) {
		return countBitstreamMime(BundleName.ORIGINAL, item, mimeList);
	}
	static int countBitstreamMime(BundleName bundleName, Item item, String mimeList) {
		int count = 0;
        try {
            for(Bundle bundle: item.getBundles(bundleName.name())){
                for(Bitstream bit: bundle.getBitstreams()) {
                	for(String mime: mimeList.split(",")) {
                        if (bit.getFormat().getMIMEType().equals(mime)) {
                        	count++;
                        }
                	}
                }
            }
        } catch (SQLException e) {
        }
		return count;
	}

	static int countBitstreamByDesc(BundleName bundleName, Item item, String descList) {
		int count = 0;
        try {
            for(Bundle bundle: item.getBundles(bundleName.name())){
                for(Bitstream bit: bundle.getBitstreams()) {
                	for(String desc: descList.split(",")) {
                		String bitDesc = bit.getDescription();
                		if (bitDesc == null) {
                			continue;
                		}
                        if (bitDesc.equals(desc)) {
                        	count++;
                        }
                	}
                }
            }
        } catch (SQLException e) {
        }
		return count;
	}

	static int countBitstreamSmallerThanMinSize(BundleName bundleName, Item item, String mimeList, String prop) {
		long size = ConfigurationManager.getLongProperty("rest", prop);
		int count = 0;
        try {
            for(Bundle bundle: item.getBundles(bundleName.name())){
                for(Bitstream bit: bundle.getBitstreams()) {
                	for(String mime: mimeList.split(",")) {
                        if (bit.getFormat().getMIMEType().equals(mime)) {
                        	if (bit.getSize() < size) {
                            	count++;                        		
                        	}
                        }
                	}
                }
            }
        } catch (SQLException e) {
        }
		return count;
	}
	
	static int countBitstreamLargerThanMaxSize(BundleName bundleName, Item item, String mimeList, String prop) {
		long size = ConfigurationManager.getLongProperty("rest", prop);
		int count = 0;
        try {
            for(Bundle bundle: item.getBundles(bundleName.name())){
                for(Bitstream bit: bundle.getBitstreams()) {
                	for(String mime: mimeList.split(",")) {
                        if (bit.getFormat().getMIMEType().equals(mime)) {
                        	if (bit.getSize() > size) {
                            	count++;                        		
                        	}
                        }
                	}
                }
            }
        } catch (SQLException e) {
        }
		return count;
	}
	
	static int countOriginalBitstreamMimeStartsWith(Item item, String prefix) {
		return countBitstreamMimeStartsWith(BundleName.ORIGINAL, item, prefix);
	}
	static int countBitstreamMimeStartsWith(BundleName bundleName, Item item, String prefix) {
		int count = 0;
        try {
            for(Bundle bundle: item.getBundles(bundleName.name())){
                for(Bitstream bit: bundle.getBitstreams()) {
                    if (bit.getFormat().getMIMEType().startsWith(prefix)) {
                    	count++;
                    }
                }
            }
        } catch (SQLException e) {
        }
		return count;
	}

	static boolean hasUnsupportedBundle(Item item, String bundleList) {
		if (bundleList == null) {
			return false;
		}
		ArrayList<String> bundles = new ArrayList<String>();
		for(String bundleName: bundleList.split(",")) {
			bundles.add(bundleName);
		}
		try {
			for(Bundle bundle: item.getBundles()) {
				if (!bundles.contains(bundle.getName())) {
					return true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	static boolean hasOriginalBitstreamMime(Item item, String mimeList) {
		return hasBitstreamMime(BundleName.ORIGINAL, item, mimeList);
	}
	static boolean hasBitstreamMime(BundleName bundleName, Item item, String mimeList) {
		return countBitstreamMime(bundleName, item, mimeList) > 0;
	}

	static boolean hasMetadataMatch(Item item, String fieldList, Pattern regex) {
		if (fieldList.equals("*")) {
			for(Metadatum md: item.getMetadata(org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY)){
				if (regex.matcher(md.value).matches()) {
					return true;
				}				
			}
		} else {
			for(String field: fieldList.split(",")) {
				for(Metadatum md: item.getMetadataByMetadataString(field)){
					if (regex.matcher(md.value).matches()) {
						return true;
					}
				}			
			}			
		}
		
		return false;
	}

	static boolean hasOnlyMetadataMatch(Item item, String fieldList, Pattern regex) {
		boolean matches = false;
		if (fieldList.equals("*")) {
			for(Metadatum md: item.getMetadata(org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY)){
				if (regex.matcher(md.value).matches()) {
					matches = true;
				} else {
					return false;
				}
			}
		} else {
			for(String field: fieldList.split(",")) {
				for(Metadatum md: item.getMetadataByMetadataString(field)){
					if (regex.matcher(md.value).matches()) {
						matches = true;
					} else {
						return false;
					}
				}			
			}
		}
		return matches;
	}

	static boolean recentlyModified(Item item, int days) {
		String accDate = item.getMetadata("dc.date.accessioned");
		if (accDate == null) {
			return false;
		}
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -days);
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");
		String cmp = sdf.format(cal.getTime());
		return accDate.compareTo(cmp) >= 0;
	}
}
