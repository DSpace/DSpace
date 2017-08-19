

package org.dspace.rest.common.search;

import org.apache.log4j.Logger;
import org.dspace.discovery.DiscoverResult;


import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

 /** 
 * @author Hamed Yousefi Nasab
 */

@XmlRootElement(name = "facets")
public class FacetResult {

	Logger log = Logger.getLogger(FacetResult.class);
	
	@XmlElement(required = true)
	private String result;
	
	
	public void setResult(String result){
		this.result = result;
	}
	
	
	public String getFacetResult(){
		return result;
	}

}