/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import java.util.HashMap;
import java.util.Map;

import gr.ekt.transformationengine.core.DataLoader;




/**
 * This class acts as a Service in the procedure ot batch import using the Biblio-Transformation-Engine
 */
public class DataLoaderService
{

    Map<String, DataLoader> dataLoaders = new HashMap<String, DataLoader>();
    
    /**
     * Default constructor
     */
    public DataLoaderService()
    {
        super();
    }

    /**
     * Setter method for dataLoaders parameter
     * @param dataLoaders
     */
    public void setDataLoaders(Map<String, DataLoader> dataLoaders)
    {
        this.dataLoaders = dataLoaders;
    }

    /**
     * 
     * @return the map of DataLoaders
     */
    public Map<String, DataLoader> getDataLoaders()
    {
        return dataLoaders;
    }
    
}
