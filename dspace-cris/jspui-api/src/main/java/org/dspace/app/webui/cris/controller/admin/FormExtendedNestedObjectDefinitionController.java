/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;

import it.cilea.osd.jdyna.controller.FormNestedObjectDefinitionController;
import it.cilea.osd.jdyna.model.ADecoratorTypeDefinition;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.IPropertyHolder;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicTypeNested;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

public class FormExtendedNestedObjectDefinitionController<H extends IPropertyHolder<Containable>, PD extends PropertiesDefinition, NPD extends ANestedPropertiesDefinition, TNO extends ATypeNestedObject<NPD>, ATD extends ADecoratorTypeDefinition<TNO, NPD>>extends
        FormNestedObjectDefinitionController<H, PD, NPD, TNO, ATD>
{

    
    @Override
    protected Map referenceData(HttpServletRequest request) throws Exception
    {
        Map map = super.referenceData(request);
        List<DynamicObjectType> researchobjects = getApplicationService().getList(DynamicObjectType.class);
        map.put("researchobjects", researchobjects);
        return map;
    }
    
    
}
