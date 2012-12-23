/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.dspace.core.Constants;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */


@Entity
@Table(name = "metadatafieldregistry")
public class MetadataFieldRegistry implements IDSpaceObject{
    private int id;
    private Integer metadataSchema;
    private String element;
    private String qualifier;
    
    
    @Id
    @Column(name = "metadata_field_id")
    @GeneratedValue
    public int getID() {
        return id;
    }
    
    public int setID(int id) {
        return this.id= id;
    }
    
    @Override
    @Transient
    public int getType()
    {
    	return Constants.METADATA;
    }


    @Column(name = "metadata_schema_id", nullable = true)
	public Integer getMetadataSchema() {
		return metadataSchema;
	}

	public void setMetadataSchema(Integer metadataField) {
		this.metadataSchema = metadataField;
	}


    @Column(name = "element", nullable = true)
	public String getElement() {
		return element;
	}

	public void setElement(String element) {
		this.element = element;
	}

    @Column(name = "qualifier", nullable = true)
	public String getQualifier() {
		return qualifier;
	}

	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

  
}
