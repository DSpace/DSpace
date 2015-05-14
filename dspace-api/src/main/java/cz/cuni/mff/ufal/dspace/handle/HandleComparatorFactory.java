/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.handle;

import java.util.Comparator;

import org.dspace.sort.SortOption;

/**
 * Class for creation of handle comparators
 * 
 * @author Michal Josífko 
 */
public class HandleComparatorFactory {
	
	public static Comparator<Handle> createComparatorByID(final String order) {
		return new Comparator<Handle>() {
			@Override
			public int compare(Handle o1, Handle o2) {					
				if(order.equals(SortOption.ASCENDING.toString())) {
					return o1.getID() > o2.getID() ? 1 : (o1.getID() < o2.getID() ? -1 : 0);
				} else {
					return o1.getID() > o2.getID() ? -1 : (o1.getID() < o2.getID() ? 1 : 0);
				}
			}
		};
	}
	
	public static Comparator<Handle> createComparatorByHandle(final String order) {
		return new Comparator<Handle>() {
			@Override
			public int compare(Handle o1, Handle o2) {					
				if(order.equals(SortOption.ASCENDING.toString())) {
					return o1.getHandle().compareTo(o2.getHandle());
				} else {
					return -o1.getHandle().compareTo(o2.getHandle());
				}
			}
		};
	}
	
	public static Comparator<Handle> createComparatorByResourceID(final String order) {	    
		return new Comparator<Handle>() {
			@Override
			public int compare(Handle o1, Handle o2) {					
				if(order.equals(SortOption.ASCENDING.toString())) {
					return o1.getResourceID() > o2.getResourceID() ? 1 : (o1.getResourceID() < o2.getResourceID() ? -1 : 0);
				} else {
					return o1.getResourceID() > o2.getResourceID() ? -1 : (o1.getResourceID() < o2.getResourceID() ? 1 : 0);
				}
			}
		};			
	}	

}
