/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.dataloader.FileDataLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




/**
 * This class acts as a Service in the procedure to batch import using the Biblio-Transformation-Engine
 */
public class BTEBatchImportService
{

	TransformationEngine transformationEngine;
    Map<String, DataLoader> dataLoaders = new HashMap<String, DataLoader>();
    Map<String, String> outputMap = new HashMap<String,String>();
    
    /**
     * Default constructor
     */
    public BTEBatchImportService()
    {
        super();
    }

    /**
     * Setter method for dataLoaders parameter
     * @param dataLoaders map of data loaders
     */
    public void setDataLoaders(Map<String, DataLoader> dataLoaders)
    {
        this.dataLoaders = dataLoaders;
    }

    /**
     * Get data loaders
     * @return the map of DataLoaders
     */
    public Map<String, DataLoader> getDataLoaders()
    {
        return dataLoaders;
    }

    /**
     * Get output map
     * @return the outputMapping
     */
	public Map<String, String> getOutputMap() {
		return outputMap;
	}

	/**
	 * Setter method for the outputMapping
	 * @param outputMap the output mapping
	 */
	public void setOutputMap(Map<String, String> outputMap) {
		this.outputMap = outputMap;
	}

        /**
         * Get transformation engine
         * @return transformation engine
         */
	public TransformationEngine getTransformationEngine() {
		return transformationEngine;
	}

        /**
         * set transformation engine
         * @param transformationEngine transformation engine
         */
	public void setTransformationEngine(TransformationEngine transformationEngine) {
		this.transformationEngine = transformationEngine;
	}
	
        /**
         * Getter of file data loaders
         * @return List of file data loaders
         */
	public List<String> getFileDataLoaders(){
		List<String> result = new ArrayList<String>();
				
		for (String key : dataLoaders.keySet()){
			DataLoader dl = dataLoaders.get(key);
			if (dl instanceof FileDataLoader){
				result.add(key);
			}
		}
		return result;
	}
}