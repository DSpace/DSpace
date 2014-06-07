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

import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.jdyna.BoxProject;
import org.dspace.app.cris.model.jdyna.DecoratorProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorProjectTypeNested;
import org.dspace.app.cris.model.jdyna.ProjectNestedObject;
import org.dspace.app.cris.model.jdyna.ProjectNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectNestedProperty;
import org.dspace.app.cris.model.jdyna.ProjectTypeNestedObject;
import org.dspace.app.cris.model.jdyna.TabProject;
import org.dspace.app.webui.cris.web.tag.ResearcherTagLibraryFunctions;

public class ProjectNavigationController
        extends
        AjaxJSONNavigationController<BoxProject, TabProject>
{

    
    public ProjectNavigationController()
    {
        super(TabProject.class);      
    }
       

    @Override
    public int countBoxPublicMetadata(Integer objectID,
            BoxProject box, boolean b)
    {        
        int result = 0;
        
        Project p = getApplicationService().get(Project.class, objectID);
        for (IContainable cont : box.getMask())
        {

            if (cont instanceof DecoratorProjectTypeNested)
            {
                DecoratorProjectTypeNested decorator = (DecoratorProjectTypeNested) cont;
                ProjectTypeNestedObject real = (ProjectTypeNestedObject)decorator.getReal();
                List<ProjectNestedObject> results = getApplicationService()
                        .getNestedObjectsByParentIDAndTypoID(Integer
                                .parseInt(p.getIdentifyingValue()),
                                (real.getId()), ProjectNestedObject.class);
                
                external: for (ProjectNestedObject object : results)
                {
                    for (ProjectNestedPropertiesDefinition rpp : real
                            .getMask())
                    {                   
                        
                        
                            for (ProjectNestedProperty pp : object.getAnagrafica4view().get(rpp.getShortName()))
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

             
            if (cont instanceof DecoratorProjectPropertiesDefinition)
            {
                DecoratorProjectPropertiesDefinition decorator = (DecoratorProjectPropertiesDefinition) cont;
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
            BoxProject box)
    {
        return ResearcherTagLibraryFunctions.isBoxHidden(
                getApplicationService().get(Project.class, objectID), box);
    }

    
}
