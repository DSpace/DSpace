/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.Serializable;

/**
 * This is the basic interface that a data model entity need to implement to be indexable in Discover
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <PK>
 *            the Class of the primary key
 */
public interface IndexableObject<PK extends Serializable> extends FindableObject<PK> {
}
