/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.cris.model.jdyna.DynamicObjectType;

import it.cilea.osd.jdyna.controller.FormAddToNestedDefinitionController;
import it.cilea.osd.jdyna.model.ADecoratorNestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ADecoratorTypeDefinition;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.web.IPropertyHolder;
import it.cilea.osd.jdyna.web.Tab;

public class FormExtendedAddToNestedDefinitionController<W extends AWidget, TP extends ANestedPropertiesDefinition, 
DTP extends ADecoratorNestedPropertiesDefinition<TP>, ATN extends ATypeNestedObject<TP>, 
DTT extends ADecoratorTypeDefinition<ATN, TP>, H extends IPropertyHolder<Containable>, T extends Tab<H>>
    extends FormAddToNestedDefinitionController<W, TP, DTP, ATN, DTT, H, T>{

	public FormExtendedAddToNestedDefinitionController(Class<TP> targetModel, Class<W> renderingModel,
			Class<H> boxModel, Class<DTT> typeModel) {
		super(targetModel, renderingModel, boxModel, typeModel);
	}

    @Override
    protected Map referenceData(HttpServletRequest request) throws Exception
    {
        
        Map map = super.referenceData(request);
        List<DynamicObjectType> researchobjects = getApplicationService().getList(DynamicObjectType.class);
        map.put("researchobjects", researchobjects);        
        return map;
        
    }

}
