/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.json;

import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.web.controller.json.AjaxJSONNavigationController;

import java.util.List;

import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorDynamicTypeNested;
import org.dspace.app.cris.model.jdyna.DynamicNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicNestedProperty;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
import org.dspace.app.webui.cris.web.tag.ResearcherTagLibraryFunctions;

public class DONavigationController
        extends
        AjaxJSONNavigationController<BoxDynamicObject, TabDynamicObject>
{

    
    public DONavigationController()
    {
        super(TabDynamicObject.class);      
    }


 
    @Override
    public int countBoxPublicMetadata(Integer objectID,
            BoxDynamicObject box, boolean b)
    {        
        int result = 0;
        
        ResearchObject p = getApplicationService().get(ResearchObject.class, objectID);
        for (IContainable cont : box.getMask())
        {


            if (cont instanceof DecoratorDynamicTypeNested)
            {
                DecoratorDynamicTypeNested decorator = (DecoratorDynamicTypeNested) cont;
                DynamicTypeNestedObject real = (DynamicTypeNestedObject)decorator.getReal();
                List<DynamicNestedObject> results = getApplicationService()
                        .getNestedObjectsByParentIDAndTypoID(Integer
                                .parseInt(p.getIdentifyingValue()),
                                (real.getId()), DynamicNestedObject.class);
                
                external: for (DynamicNestedObject object : results)
                {
                    for (DynamicNestedPropertiesDefinition rpp : real
                            .getMask())
                    {                   
                        
                        
                            for (DynamicNestedProperty pp : object.getAnagrafica4view().get(rpp.getShortName()))
                            {
                                if (pp.getVisibility() == 1)
                                {
                                    result++;
                                    break external;
                                }
                            } 

                        
                        
                    }
                }

            }

             
            if (cont instanceof DecoratorDynamicPropertiesDefinition)
            {
                DecoratorDynamicPropertiesDefinition decorator = (DecoratorDynamicPropertiesDefinition) cont;
                result += ResearcherTagLibraryFunctions.countDynamicPublicMetadata(
                        p.getDynamicField(), decorator.getShortName(),
                        decorator.getRendering(), decorator.getReal(),
                        false);
            }
             


        }

        
        return result;
    }

    @Override
    public boolean isBoxHidden(Integer objectID,
            BoxDynamicObject box)
    {
        return ResearcherTagLibraryFunctions.isBoxHidden(
                getApplicationService().get(ResearchObject.class, objectID), box);
    }

    
}
