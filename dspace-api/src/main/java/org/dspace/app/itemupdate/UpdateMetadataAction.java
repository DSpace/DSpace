/*
 * UpdateMetadataAction.java
 *
 * Version: $Revision: 3984 $
 *
 * Date: $Date: 2009-06-29 22:33:25 -0400 (Mon, 29 Jun 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.itemupdate;

import java.util.HashSet;
import java.util.Set;

/**
 *   This abstract subclass for metadata actions
 *   maintains a collection for the target metadata fields
 *   expressed as a string in the compound notation ( <schema>.<element>.<qualifier> )
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
	 * @param targetFields
	 */
	public void addTargetFields(Set<String> targetFields) {
		for (String tf : targetFields)
		{
			this.targetFields.add(tf);
		}
		
	}

	/**
	 *    Add array of target fields to update
	 * @param targetFields
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
	 * @param targetField
	 */
	public void addTargetField(String targetField) {
			this.targetFields.add(targetField);
	}

}
