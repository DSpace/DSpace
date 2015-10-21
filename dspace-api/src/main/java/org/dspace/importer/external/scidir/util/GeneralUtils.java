/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scidir.util;

import org.dspace.importer.external.scidir.util.helper.Stringifier;
import org.dspace.importer.external.scidir.util.helper.ToStringStringifier;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 16 Sep 2015
 */
public class GeneralUtils {

    public static <T> String join(String separator, T... objects) {
        return join(Arrays.asList(objects), new ToStringStringifier<T>(), separator);
    }

    public static <T> String join(Stringifier<T> stringifier, String separator, T... objects) {
        return join(Arrays.asList(objects), stringifier, separator);
    }

    public static <T> String join(Iterable<? extends T> objects, String separator) {
        return join(objects, new ToStringStringifier<T>(), separator);
    }

    public static <T> String join(Iterable<? extends T> objects, Stringifier<T> stringifier, String separator) {
        Iterator<? extends T> iterator = objects.iterator();
        return join(iterator, stringifier, separator);
    }

    public static <T> String join(Iterator<? extends T> iterator, Stringifier<T> stringifier, String separator) {
        StringBuilder builder = new StringBuilder();
        while (iterator.hasNext()) {
            T next = iterator.next();
            String string = stringifier.stringify(next);
            builder.append(string);
            if (iterator.hasNext()) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }
}
