/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.integration.NameResearcherPage;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.DCPersonName;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
/*
 * @author: Sergio Bilello
 * 
 */
public class RPSurnameSolrIndexer implements CrisServiceIndexPlugin{

	
	@Override
	public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
			ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
			SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters) {

		ResearcherPage rp = null;
		if (crisObject instanceof ResearcherPage){
			rp = (ResearcherPage) crisObject;
			
			for (String name : rp.getAllNames()){
				DCPersonName dcpersona = new DCPersonName(name);  
				document.addField("rpsurnames",dcpersona.getLastName());
			}
				
			// retrieving surnames for variants and adding on rpsurnamesIndex
			for (NameResearcherPage rpn : ResearcherPageUtils.getAllVariantsName(null, rp)){
	            String rawValue = rpn.getName();
	            DCPersonName dcpersona = new DCPersonName(rawValue);  
	    		document.addField("rpsurnames",dcpersona.getLastName());
	        }		
			
		}
	}
	
	@Override
	public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
			ACNO dso, SolrInputDocument sorlDoc, Map<String, List<DiscoverySearchFilter>> searchFilters) {
		// FIXME NOT SUPPORTED OPERATION
	}

}

