/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;

import it.cilea.osd.jdyna.controller.FormWidgetPointerDecoratorPDController;
import it.cilea.osd.jdyna.widget.WidgetPointer;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
import org.dspace.app.cris.model.jdyna.value.DOPointer;

public class FormWidgetPointerDODecoratorPDController extends
        FormWidgetPointerDecoratorPDController<DynamicPropertiesDefinition, DecoratorDynamicPropertiesDefinition, BoxDynamicObject, TabDynamicObject, DOPointer>
{

    


    public FormWidgetPointerDODecoratorPDController(
            Class<DynamicPropertiesDefinition> targetModel,
            Class<WidgetPointer> renderingModel,
            Class<BoxDynamicObject> boxModel, Class<DOPointer> crisModel)
    {
        super(targetModel, renderingModel, boxModel, crisModel);
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
