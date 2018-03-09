/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 	 Container for UpdateActions      
 *    Order of actions is very import for correct processing.  This implementation
 *     supports an iterator that returns the actions in the order in which they are
 *     put in.  Adding the same action a second time has no effect on this order.
 * 
 *
 */
public class ActionManager implements Iterable<UpdateAction> {
	
	protected Map<Class<? extends UpdateAction>, UpdateAction> registry
                     = new LinkedHashMap<Class<? extends UpdateAction>, UpdateAction>();
	
        /**
         * Get update action
         * @param actionClass UpdateAction class
         * @return instantiation of UpdateAction class
         * @throws InstantiationException if instantiation error
         * @throws IllegalAccessException if illegal access error
         */
	public UpdateAction getUpdateAction(Class<? extends UpdateAction> actionClass) 
	throws InstantiationException, IllegalAccessException
	{
		UpdateAction action =  registry.get(actionClass);
		
		if (action == null)
		{
			action = actionClass.newInstance();
			registry.put(actionClass, action);
		}
		
		return action;		
	}
	
	/**
	 * 
	 * @return whether any actions have been registered with this manager
	 */
	public boolean hasActions()
	{
		return !registry.isEmpty();
	}
	
	/**
	 * 	 This implementation guarantees the iterator order is the same as the order
	 *   in which updateActions have been added
	 * 
	 * @return iterator for UpdateActions
	 */
	@Override
    public Iterator<UpdateAction> iterator()
	{
		return new Iterator<UpdateAction>() 
		{ 			
			private Iterator<Class<? extends UpdateAction>> itr = registry.keySet().iterator();
			
			@Override
            public boolean hasNext()
			{
				return itr.hasNext();
			}
			
			@Override
            public UpdateAction next()
			{
				return registry.get(itr.next());
			}
			
			//not supported
			@Override
            public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
		
	}
}
