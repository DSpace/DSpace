/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;

/**
 * 	  Base class for Bitstream actions
 * 
 *
 */
public abstract class UpdateBitstreamsAction implements UpdateAction {

	protected boolean alterProvenance = true;


    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

	/**
	 *        Set variable to indicate that the dc.description.provenance field may 
	 *        be changed as a result of Bitstream changes by ItemUpdate
	 * @param alterProvenance whether to alter provenance
	 */
	public void setAlterProvenance(boolean alterProvenance)
	{
		this.alterProvenance = alterProvenance;
	}
	
	/**
	 * 
	 * @return boolean value to indicate whether the dc.description.provenance field may 
	 *        be changed as a result of Bitstream changes by ItemUpdate
	 */
	public boolean getAlterProvenance()
	{
		return alterProvenance;
	}

}
