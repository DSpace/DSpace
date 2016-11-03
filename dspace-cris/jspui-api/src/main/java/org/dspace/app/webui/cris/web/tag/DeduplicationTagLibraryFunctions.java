/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.web.tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.webui.cris.servlet.DTODCValue;

public class DeduplicationTagLibraryFunctions {
	/**
	 * log4j category
	 */
	public static final Log log = LogFactory
			.getLog(DeduplicationTagLibraryFunctions.class);

	public static boolean checkContentEquality(List elements) {

		boolean result = true;
		outer: for (DTODCValue element : (List<DTODCValue>) elements) {
			inner: for (DTODCValue innerElement : (List<DTODCValue>) elements) {
				if (!(element.getValue().equals(innerElement.getValue())
						&& element.getAuthority().equals(innerElement.getAuthority()))) {
					result = false;
					break outer;
				}
			}
		}
		return result;
	}

	public static List<String> getIDContentEquality(List elements, String currentValue, DTODCValue element) {
		List<String> result = new ArrayList<String>();
		for (DTODCValue innerElement : (List<DTODCValue>) elements) {
			if ((element.getValue().equals(innerElement.getValue()) && element
					.getAuthority().equals(innerElement.getAuthority()))) {
				result.add((String) innerElement.getOwner().toString());
			}
		}
		return result;
	}

	static class DTODCValueComparator implements Comparator<DTODCValue>{
		private Integer masterID;
		
		public DTODCValueComparator(Integer masterID) {
			this.masterID = masterID;
		}
		
		public int compare(DTODCValue o1, DTODCValue o2) {			
			if (o1.getOwner() == masterID)
			{
                return -1; // -1 jdk7 || 1 jdk6
			}
            return 1; // 1 jdk7 || -1 jdk6
			
		}
	}
	
	
	/**	Find similar value and group it.
	 * 
	 * @param elements
	 * @param masterID
	 * @return
	 */
	public static List<DTODCValue> groupDeduplication(List elements, Integer masterID) {

		Comparator<DTODCValue> mycom = new DeduplicationTagLibraryFunctions.DTODCValueComparator(masterID);
		//WARNING different behaviour between JDK6 vs JDK7 (if use JDK7 reverse the value returned from compare method in DTODCValueComparator)   
		java.util.Collections.sort(elements, mycom);
		for (int i = 0; i < elements.size(); i++) {			
			DTODCValue element = (DTODCValue) elements.get(i);			
			for (int j = 0; j < elements.size(); j++) {
				DTODCValue innerElement = (DTODCValue) elements.get(j);
				if (!(element.getOwner().equals(innerElement.getOwner()))) {
					if ((element.getValue() != null)
							&& (element.isMasterDuplicate() == null || (element.isMasterDuplicate() != null && element
									.isMasterDuplicate() != false))) {
						if (element.getValue().equals(innerElement.getValue())
								&& element.getLanguage().equals(
										innerElement.getLanguage())
								&& element.getAuthority().equals(
										innerElement.getAuthority())) {
							
							
							if (!(element.getDuplicates().contains(innerElement
									.getOwner()))) {
								element.setMasterDuplicate(true);
								element.getDuplicates().add(
										innerElement.getOwner());
							}
							if (!(innerElement.getDuplicates().contains(element
									.getOwner()))) {
								innerElement.setMasterDuplicate(false);
								innerElement.getDuplicates().add(
										element.getOwner());
							}
						}
					}
				}
			}
		}
		return elements;
	}
}
