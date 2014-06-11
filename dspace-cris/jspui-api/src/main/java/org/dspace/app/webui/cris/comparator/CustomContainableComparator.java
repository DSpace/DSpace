/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.comparator;

import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.util.Comparator;

public class CustomContainableComparator<P extends Property<TP>, TP extends PropertiesDefinition,C extends Containable<P>> implements Comparator<C> {

	@Override
	public int compare(C o1, C o2) {
		if(o2==null) return -1;
		if(o1.getShortName().equals(o2.getShortName())) {
			if(o1.getLabel()!=null && o2.getLabel()!=null) {
				return o1.getLabel().trim().compareTo(o2.getLabel().trim());
			}
		}
		return o1.getShortName().compareTo(o2.getShortName());
	}
	
}
