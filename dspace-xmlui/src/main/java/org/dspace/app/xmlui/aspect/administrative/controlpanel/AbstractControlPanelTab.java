/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.controlpanel;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.configuration.Settings;
import org.apache.excalibur.store.Store;
import org.apache.excalibur.store.StoreJanitor;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;

/**
 * Abstract tab, implementations of tabs should extend this class.
 * Based on the original ControlPanel class by Jay Paz and Scott Phillips
 * @author LINDAT/CLARIN dev team (http://lindat.cz)
 */
public abstract class AbstractControlPanelTab extends AbstractDSpaceTransformer implements Serviceable, Disposable, ControlPanelTab {

    /** 
     * The service manager allows us to access the continuation's 
     * manager.  It is obtained from the Serviceable API
     */
    protected ServiceManager serviceManager;

    /**
     * The Cocoon Settings (used to display Cocoon info)
     */
    protected Settings settings;

    /**
     * The Cocoon StoreJanitor (used for cache statistics)
     */
    protected StoreJanitor storeJanitor;

     /**
     * The Cocoon Default Store (used for cache statistics)
     */
    protected Store storeDefault;

    /**
     * The Cocoon Persistent Store (used for cache statistics)
     */
    protected Store storePersistent;
    
    
    /**
     * Link of the tab
     */
    protected String web_link;
	
	
	@Override
	public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        
        this.settings = (Settings) this.serviceManager.lookup(Settings.ROLE);
        
        if(this.serviceManager.hasService(StoreJanitor.ROLE))
            this.storeJanitor = (StoreJanitor) this.serviceManager.lookup(StoreJanitor.ROLE);
        
        if (this.serviceManager.hasService(Store.ROLE))
            this.storeDefault = (Store) this.serviceManager.lookup(Store.ROLE);
        
        if(this.serviceManager.hasService(Store.PERSISTENT_STORE))
            this.storePersistent = (Store) this.serviceManager.lookup(Store.PERSISTENT_STORE);
	}
	
    /**
     * Release all Cocoon resources.
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    @Override
    public void dispose() 
    {
        if (this.serviceManager != null) 
        {
            this.serviceManager.release(this.storePersistent);
            this.serviceManager.release(this.storeJanitor);
            this.serviceManager.release(this.storeDefault);
            this.serviceManager.release(this.settings);
            this.storePersistent = null;
            this.storeJanitor = null; 	
            this.storeDefault = null;
            this.settings = null;
        }
        super.dispose();
    }
    
    public void setWebLink(String web_link) {
    	this.web_link = web_link;
    }

}

