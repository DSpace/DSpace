/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;

import it.cilea.osd.jdyna.model.ADecoratorTypeDefinition;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.IPropertyHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicTypeNested;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

public class FormDynamicExtendedNestedObjectDefinitionController<H extends IPropertyHolder<Containable>, PD extends PropertiesDefinition, NPD extends ANestedPropertiesDefinition, TNO extends ATypeNestedObject<NPD>, ATD extends ADecoratorTypeDefinition<TNO, NPD>>extends
    FormExtendedNestedObjectDefinitionController<H, PD, NPD, TNO, ATD>
{

    
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {
        DecoratorDynamicTypeNested object = (DecoratorDynamicTypeNested)command;        
        String shortName = object.getReal().getShortName();
        
        String boxId = request.getParameter("boxId");
                        
        if(boxId!=null && !boxId.isEmpty()) {
            BoxDynamicObject box = getApplicationService().get(BoxDynamicObject.class, Integer.parseInt(boxId));
            if(!shortName.startsWith(box.getTypeDef().getShortName())) {
                object.getReal().setShortName(box.getTypeDef().getShortName() + shortName);   
            }            
        }  
        return super.onSubmit(request, response, command, errors);
    }
    
}
