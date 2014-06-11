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

import it.cilea.osd.jdyna.controller.FormAddWidgetPointerToNestedPDController;
import it.cilea.osd.jdyna.widget.WidgetPointer;

import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicTypeNested;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
import org.dspace.app.cris.model.jdyna.value.DOPointer;

public class FormAddWidgetPointerDOToNestedPDController
        extends
        FormAddWidgetPointerToNestedPDController<DynamicNestedPropertiesDefinition, DecoratorDynamicNestedPropertiesDefinition, DynamicTypeNestedObject, DecoratorDynamicTypeNested, BoxDynamicObject, TabDynamicObject, DOPointer>
{

    public FormAddWidgetPointerDOToNestedPDController(
            Class<DynamicNestedPropertiesDefinition> targetModel,
            Class<WidgetPointer> renderingModel,
            Class<BoxDynamicObject> boxModel,
            Class<DecoratorDynamicTypeNested> typeModel,
            Class<DOPointer> pValueClass)
    {
        super(targetModel, renderingModel, boxModel, typeModel, pValueClass);        
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
