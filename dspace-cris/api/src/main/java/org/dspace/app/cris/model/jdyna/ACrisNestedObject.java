/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ICrisObject;
import org.dspace.app.cris.model.IExportableDynamicObject;
import org.dspace.app.cris.model.SourceReference;
import org.dspace.app.cris.model.export.ExportConstants;

import it.cilea.osd.jdyna.model.ANestedObjectWithTypeSupport;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

@MappedSuperclass
public abstract class ACrisNestedObject<P extends ANestedProperty<TP>, TP extends ANestedPropertiesDefinition, PP extends Property<PTP>, PTP extends PropertiesDefinition>
        extends ANestedObjectWithTypeSupport<P, TP, PP, PTP> implements ICrisObject<P, TP>, IExportableDynamicObject<TP, P, AnagraficaSupport<P,TP>>, Cloneable
{
    
    @Embedded    
    private SourceReference sourceReference;
    
    public abstract int getType();
    
    public String getTypeText() {
    	return "nested";
    }
    
    public int getID() {
        return super.getId();
    }

    public SourceReference getSourceReference()
    {
        if(this.sourceReference==null) {
            this.sourceReference = new SourceReference();
        }
        return sourceReference;
    }

    public void setSourceReference(SourceReference sourceReference)
    {
        this.sourceReference = sourceReference;
    }  

    @Override
    public ACrisNestedObject<P, TP, PP, PTP> clone() throws CloneNotSupportedException
    {
        return (ACrisNestedObject<P, TP, PP, PTP>)super.clone();
    }
    
	public String getNamePublicIDAttribute() {
		return ExportConstants.NAME_PUBLICID_ATTRIBUTE;
	}

	public String getValuePublicIDAttribute() {
		return "" + this.getId();
	}

	public String getNameIDAttribute() {
		return ExportConstants.NAME_ID_ATTRIBUTE;
	}

	public String getValueIDAttribute() {
		if (this.getUuid() == null) {
			return "";
		}
		return "" + this.getUuid().toString();
	}

	public String getNameBusinessIDAttribute() {
		return ExportConstants.NAME_BUSINESSID_ATTRIBUTE;
	}

	public String getValueBusinessIDAttribute() {
		return ((ACrisObject)this.getParent()).getCrisID();
	}

	public String getNameTypeIDAttribute() {
		return ExportConstants.NAME_TYPE_ATTRIBUTE;
	}

	public String getValueTypeIDAttribute() {
		return "" + getType();
	}

	public String getNameSingleRowElement() {
		return ExportConstants.ELEMENT_SINGLEROW;
	}

	public ANestedObjectWithTypeSupport<P, TP, PP, PTP> getAnagraficaSupport() {
		return this;
	}
    
}
