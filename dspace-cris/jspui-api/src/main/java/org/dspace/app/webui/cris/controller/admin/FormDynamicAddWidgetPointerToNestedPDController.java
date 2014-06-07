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

import it.cilea.osd.jdyna.controller.FormAddWidgetPointerToNestedPDController;
import it.cilea.osd.jdyna.value.PointerValue;
import it.cilea.osd.jdyna.widget.WidgetPointer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicTypeNested;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

public class FormDynamicAddWidgetPointerToNestedPDController<VPO extends PointerValue> extends
        FormAddWidgetPointerToNestedPDController<DynamicNestedPropertiesDefinition, DecoratorDynamicNestedPropertiesDefinition, DynamicTypeNestedObject, DecoratorDynamicTypeNested, BoxDynamicObject, TabDynamicObject, VPO>
{

    public FormDynamicAddWidgetPointerToNestedPDController(
            Class<DynamicNestedPropertiesDefinition> targetModel,
            Class<WidgetPointer> renderingModel,
            Class<BoxDynamicObject> boxModel,
            Class<DecoratorDynamicTypeNested> typeModel, Class<VPO> pValueClass)
    {
        super(targetModel, renderingModel, boxModel, typeModel, pValueClass);
    }
    
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {
        DecoratorDynamicNestedPropertiesDefinition object = (DecoratorDynamicNestedPropertiesDefinition)command;
        String shortName = object.getShortName();
        
        String renderingparent = request.getParameter("renderingparent");
        
        if(renderingparent!=null && !renderingparent.isEmpty()) {
            DecoratorDynamicTypeNested rPd = getApplicationService().get(DecoratorDynamicTypeNested.class, Integer.parseInt(renderingparent));            
            if(!shortName.startsWith(rPd.getShortName())) {
                object.getReal().setShortName(rPd.getShortName() + shortName);   
            }            
        }
        return super.onSubmit(request, response, command, errors);
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
