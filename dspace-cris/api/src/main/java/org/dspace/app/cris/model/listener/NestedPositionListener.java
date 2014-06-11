/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.listener;

import it.cilea.osd.common.listener.NativePreInsertEventListener;
import it.cilea.osd.common.model.Identifiable;
import it.cilea.osd.jdyna.model.ANestedObject;

import org.dspace.app.cris.util.ResearcherPageUtils;

public class NestedPositionListener implements NativePreInsertEventListener
{
        
    private Integer INCREMENT_POSITION = 10000;
        
    @Override
    public <T extends Identifiable> void onPreInsert(T entity)
    {
        
        if (entity instanceof ANestedObject)
        {
            
            ANestedObject object = (ANestedObject) entity;           
            object.setPositionDef(ResearcherPageUtils.getNestedMaxPosition(object)+INCREMENT_POSITION);
            
        }
                
    }

}
