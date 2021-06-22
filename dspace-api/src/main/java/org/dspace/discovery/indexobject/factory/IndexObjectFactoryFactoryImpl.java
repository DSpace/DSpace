/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract factory implementation to get the IndexFactory objects
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class IndexObjectFactoryFactoryImpl extends IndexObjectFactoryFactory {

    @Autowired
    List<IndexFactory> indexableObjectServices;

    @Override
    public List<IndexFactory> getIndexFactories() {
        return indexableObjectServices;
    }
}