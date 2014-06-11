/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;

import it.cilea.osd.jdyna.controller.FormAddToNestedDefinitionController;
import it.cilea.osd.jdyna.model.AWidget;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicTypeNested;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

public class FormDynamicAddToNestedDefinitionController<W extends AWidget> extends
        FormAddToNestedDefinitionController<W, DynamicNestedPropertiesDefinition, DecoratorDynamicNestedPropertiesDefinition, DynamicTypeNestedObject, DecoratorDynamicTypeNested, BoxDynamicObject, TabDynamicObject>
{

    public FormDynamicAddToNestedDefinitionController(
            Class<DynamicNestedPropertiesDefinition> targetModel,
            Class<W> renderingModel, Class<BoxDynamicObject> boxModel,
            Class<DecoratorDynamicTypeNested> typeModel)
    {
        super(targetModel, renderingModel, boxModel, typeModel);        
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

}
