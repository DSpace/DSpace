/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network.dto;

import java.util.LinkedList;
import java.util.List;

public class JsGraph {
	
	private String id;
	
	private String name;
	
	private List<JsGraphAdjacence> adjacencies;
	
	private JsGraphNodeData data;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JsGraphNodeData getData() {
		return data;
	}

	public void setData(JsGraphNodeData data) {
		this.data = data;
	}

	public List<JsGraphAdjacence> getAdjacencies() {
		if(this.adjacencies==null) {
			this.adjacencies = new LinkedList<JsGraphAdjacence>();
		}
		return adjacencies;
	}

	public void setAdjacencies(List<JsGraphAdjacence> adjacencies) {
		this.adjacencies = adjacencies;
	}


}
