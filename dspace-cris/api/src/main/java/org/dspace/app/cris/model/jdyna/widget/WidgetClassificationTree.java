/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.widget;

import java.beans.PropertyEditor;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.editor.PointerPropertyEditor;
import org.dspace.app.cris.model.jdyna.value.Classification;

import it.cilea.osd.jdyna.service.IPersistenceDynaService;
import it.cilea.osd.jdyna.widget.AWidgetClassificationTree;

@Entity
@Table(name = "jdyna_widget_classification")
public class WidgetClassificationTree extends AWidgetClassificationTree<Classification>
{
	@ManyToOne
	private ResearchObject rootResearchObject;
	
	@ManyToOne
	private DynamicPropertiesDefinition metadataBuilderTree;
	
	@Override
	public Classification getInstanceValore() {		
		return new Classification();
	}

	@Override
	public Class<Classification> getValoreClass() {
		return Classification.class;
	}

	@Override
	public Class<ResearchObject> getTargetValoreClass() {
		return getInstanceValore().getTargetClass();
	}

	public ResearchObject getRootResearchObject() {
		return rootResearchObject;
	}

	public void setRootResearchObject(ResearchObject rootResearchObject) {
		this.rootResearchObject = rootResearchObject;
	}

	public DynamicPropertiesDefinition getMetadataBuilderTree() {
		return metadataBuilderTree;
	}

	public void setMetadataBuilderTree(DynamicPropertiesDefinition metadataBuilderTree) {
		this.metadataBuilderTree = metadataBuilderTree;
	}
	
    @Override
    public PropertyEditor getImportPropertyEditor(
            IPersistenceDynaService applicationService, String service)
    {
        PointerPropertyEditor propertyEditor = new PointerPropertyEditor(getTargetValoreClass(), service);
        propertyEditor.setApplicationService(applicationService);
        return propertyEditor;
    }


}
