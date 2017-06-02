/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.crosswalk;

import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by jonas - jonas@atmire.com on 21/04/17.
 * Implementation of the {@link DisseminationCrosswalk} interface that enables the ability to set a Context manually
 */
public abstract class ContextAwareDisseminationCrosswalk  implements DisseminationCrosswalk{

    private Context context;
    private boolean contextCreatedInternally = false;

    public void setContext(Context context){
        this.context = context;
    }
    public Context getContext() throws SQLException {
        if(context == null|| !context.isValid()){
            context=new Context();
            contextCreatedInternally = true;
        }
        return context;
    }

    public void handleContextCleanup() throws SQLException {
        if(contextCreatedInternally){
            context.complete();
        }else{
            context.commit();
        }
    }


}
