/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

/**
 * 	  Base class for Bitstream actions
 * 
 *
 */
public abstract class UpdateBitstreamsAction implements UpdateAction {

	protected boolean alterProvenance = true;

	/**
	 *        Set variable to indicate that the dc.description.provenance field may 
	 *        be changed as a result of Bitstream changes by ItemUpdate
	 * @param alterProvenance
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
