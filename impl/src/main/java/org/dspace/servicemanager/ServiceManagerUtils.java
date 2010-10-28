package org.dspace.servicemanager;

import java.io.Serializable;
import java.util.Comparator;

class ServiceManagerUtils {
    /**
     * Compares objects by class name.
     */
    public static class ServiceComparator implements Comparator<Object>, Serializable {
        public final static long serialVersionUID = 1l;
        public int compare(Object o1, Object o2) {
            if (o1 != null && o2 != null) {
                return o1.getClass().getName().compareTo(o2.getClass().getName());
            }
            return 0;
        }
    }
}
