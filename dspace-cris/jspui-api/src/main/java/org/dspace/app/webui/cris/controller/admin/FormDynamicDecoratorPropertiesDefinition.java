/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import it.cilea.osd.jdyna.controller.FormDecoratorPropertiesDefinitionController;
import it.cilea.osd.jdyna.model.AWidget;

import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

public class FormDynamicDecoratorPropertiesDefinition<W extends AWidget> extends
        FormDecoratorPropertiesDefinitionController<W, DynamicPropertiesDefinition, DecoratorDynamicPropertiesDefinition, BoxDynamicObject, TabDynamicObject>
{

    public FormDynamicDecoratorPropertiesDefinition(Class<DynamicPropertiesDefinition> targetModel,
            Class<W> renderingModel, Class<BoxDynamicObject> boxModel)
    {
        super(targetModel, renderingModel, boxModel);
    }
    
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {
        DecoratorDynamicPropertiesDefinition object = (DecoratorDynamicPropertiesDefinition)command;
        String shortName = object.getShortName();
        
        String boxId = request.getParameter("boxId");
                        
        if(boxId!=null && !boxId.isEmpty()) {
            BoxDynamicObject box = getApplicationService().get(BoxDynamicObject.class, Integer.parseInt(boxId));
            if(!shortName.startsWith(box.getTypeDef().getShortName())) {
                object.getReal().setShortName(box.getTypeDef().getShortName() + shortName);   
            }            
        }  
        
        return super.onSubmit(request, response, object, errors);
    }

}
