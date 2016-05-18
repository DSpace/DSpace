/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scidir.util.helper;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 16 Sep 2015
 */
public interface Stringifier<T> {
    String stringify(T t);
}
