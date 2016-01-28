/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemmarking;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * This is an item marking Strategy class that tries to mark an item availability
 * based on the existence of bitstreams within the ORIGINAL bundle.
 * 
 * @author Kostas Stamatis
 * 
 */
public class ItemMarkingAvailabilityBitstreamStrategy implements ItemMarkingExtractor {

	private String availableImageName;
	private String nonAvailableImageName;
	
	public ItemMarkingAvailabilityBitstreamStrategy() {
	
	}

	@Override
	public ItemMarkingInfo getItemMarkingInfo(Context context, Item item)
			throws SQLException {
		
		Bundle[] bundles = item.getBundles("ORIGINAL");
		if (bundles.length == 0){
			ItemMarkingInfo markInfo = new ItemMarkingInfo();
			markInfo.setImageName(nonAvailableImageName);	
			
			return markInfo;
		}
		else {
			Bundle originalBundle = bundles[0];
			if (originalBundle.getBitstreams().length == 0){
				ItemMarkingInfo markInfo = new ItemMarkingInfo();
				markInfo.setImageName(nonAvailableImageName);
				
				return markInfo;
			}
			else {
				Bitstream bitstream = originalBundle.getBitstreams()[0];
				
				ItemMarkingInfo signInfo = new ItemMarkingInfo();
				signInfo.setImageName(availableImageName);
				signInfo.setTooltip(bitstream.getName());
				
				
				
				String bsLink = "";

                bsLink = bsLink + "bitstream/"
                            + item.getHandle() + "/"
                            + bitstream.getSequenceID() + "/";
                
                try {
					bsLink = bsLink + Util.encodeBitstreamName(bitstream.getName(),  Constants.DEFAULT_ENCODING);
				} catch (UnsupportedEncodingException e) {
					
					e.printStackTrace();
				}
                
				signInfo.setLink(bsLink);
				
				return signInfo;
			}
		}
	}

	public void setAvailableImageName(String availableImageName) {
		this.availableImageName = availableImageName;
	}

	public void setNonAvailableImageName(String nonAvailableImageName) {
		this.nonAvailableImageName = nonAvailableImageName;
	}	
}