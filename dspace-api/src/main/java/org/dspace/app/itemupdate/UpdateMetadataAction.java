/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.util.HashSet;
import java.util.Set;

/**
 *   This abstract subclass for metadata actions
 *   maintains a collection for the target metadata fields
 *   expressed as a string in the compound notation ( {@code <schema>.<element>.<qualifier>} )
 *   on which to apply the action when the method execute is called.
 * 
 *   Implemented as a Set to avoid problems with duplicates
 * 
 *
 */
public abstract class UpdateMetadataAction implements UpdateAction {

	protected Set<String> targetFields = new HashSet<String>();
	
    /**
     *   Get target fields
     *   
     * @return set of fields to update
     */
	public Set<String> getTargetFields() {
		return targetFields;
	}

	/**
	 *   Set target fields
	 *   
	 * @param targetFields Set of target fields to update
	 */
	public void addTargetFields(Set<String> targetFields) {
		for (String tf : targetFields)
		{
			this.targetFields.add(tf);
		}
		
	}

	/**
	 *    Add array of target fields to update
	 * @param targetFields array of target fields to update
	 */
	public void addTargetFields(String[] targetFields) {
		for (String tf : targetFields)
		{
			this.targetFields.add(tf);
		}
		
	}

	/**
	 *   Add single field to update
	 * 
	 * @param targetField target field to update
	 */
	public void addTargetField(String targetField) {
			this.targetFields.add(targetField);
	}

}
