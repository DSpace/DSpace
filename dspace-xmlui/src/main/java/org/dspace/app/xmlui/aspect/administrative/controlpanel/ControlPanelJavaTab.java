/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.controlpanel;

import java.util.Iterator;
import java.util.Map;

import org.apache.excalibur.store.Store;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;

/**
 * Control panel tab that displays info about java and system.
 * Based on the original ControlPanel class by Jay Paz and Scott Phillips
 * @author LINDAT/CLARIN dev team (http://lindat.cz)
 */
public class ControlPanelJavaTab extends AbstractControlPanelTab
{

    private static final Message T_JAVA_HEAD = message("xmlui.administrative.ControlPanel.java_head");

    private static final Message T_JAVA_VERSION = message("xmlui.administrative.ControlPanel.java_version");

    private static final Message T_JAVA_VENDOR = message("xmlui.administrative.ControlPanel.java_vendor");

    private static final Message T_OS_NAME = message("xmlui.administrative.ControlPanel.os_name");

    private static final Message T_OS_ARCH = message("xmlui.administrative.ControlPanel.os_arch");

    private static final Message T_OS_VERSION = message("xmlui.administrative.ControlPanel.os_version");

    private static final Message T_RUNTIME_HEAD = message("xmlui.administrative.ControlPanel.runtime_head");

    private static final Message T_RUNTIME_PROCESSORS = message("xmlui.administrative.ControlPanel.runtime_processors");

    private static final Message T_RUNTIME_MAX = message("xmlui.administrative.ControlPanel.runtime_max");

    private static final Message T_RUNTIME_TOTAL = message("xmlui.administrative.ControlPanel.runtime_total");

    private static final Message T_RUNTIME_USED = message("xmlui.administrative.ControlPanel.runtime_used");

    private static final Message T_RUNTIME_FREE = message("xmlui.administrative.ControlPanel.runtime_free");

    private static final Message T_COCOON_HEAD = message("xmlui.administrative.ControlPanel.cocoon_head");

    private static final Message T_COCOON_VERSION = message("xmlui.administrative.ControlPanel.cocoon_version");

    private static final Message T_COCOON_CACHE_DIR = message("xmlui.administrative.ControlPanel.cocoon_cache_dir");

    private static final Message T_COCOON_WORK_DIR = message("xmlui.administrative.ControlPanel.cocoon_work_dir");

    private static final Message T_COCOON_MAIN_CACHE_SIZE = message("xmlui.administrative.ControlPanel.cocoon_main_cache_size");

    private static final Message T_COCOON_PERSISTENT_CACHE_SIZE = message("xmlui.administrative.ControlPanel.cocoon_persistent_cache_size");

    private static final Message T_COCOON_TRANS_CACHE_SIZE = message("xmlui.administrative.ControlPanel.cocoon_transient_cache_size");

    private static final Message T_COCOON_CACHE_CLEAR = message("xmlui.administrative.ControlPanel.cocoon_cache_clear");

    @Override
    public void addBody(Map objectModel, Division div) throws WingException
    {
        // Get memory statistics
        int processors = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        // Convert bytes into MiB
        maxMemory = maxMemory / 1024 / 1024;
        totalMemory = totalMemory / 1024 / 1024;
        usedMemory = usedMemory / 1024 / 1024;
        freeMemory = freeMemory / 1024 / 1024;

        // LIST: Java
        List list = div.addList("javaOs");
        list.setHead(T_JAVA_HEAD);
        list.addLabel(T_JAVA_VERSION);
        list.addItem(System.getProperty("java.version"));
        list.addLabel(T_JAVA_VENDOR);
        list.addItem(System.getProperty("java.vm.name"));
        list.addLabel(T_OS_NAME);
        list.addItem(System.getProperty("os.name"));
        list.addLabel(T_OS_ARCH);
        list.addItem(System.getProperty("os.arch"));
        list.addLabel(T_OS_VERSION);
        list.addItem(System.getProperty("os.version"));

        // LIST: memory
        List runtime = div.addList("runtime");
        runtime.setHead(T_RUNTIME_HEAD);
        runtime.addLabel(T_RUNTIME_PROCESSORS);
        runtime.addItem(String.valueOf(processors));
        runtime.addLabel(T_RUNTIME_MAX);
        runtime.addItem(String.valueOf(maxMemory) + " MiB");
        runtime.addLabel(T_RUNTIME_TOTAL);
        runtime.addItem(String.valueOf(totalMemory) + " MiB");
        runtime.addLabel(T_RUNTIME_USED);
        runtime.addItem(String.valueOf(usedMemory) + " MiB");
        runtime.addLabel(T_RUNTIME_FREE);
        runtime.addItem(String.valueOf(freeMemory) + " MiB");

        // List: Cocoon Info & Cache
        addCocoonInformation(div);
    }

    /**
     * Add specific Cocoon information, especially related to the Cocoon Cache.
     * <P>
     * For more information about Cocoon Caches/Stores, see:
     * http://wiki.apache.org/cocoon/StoreComponents
     */
    private void addCocoonInformation(Division div) throws WingException
    {
        // List: Cocoon Info & Caches
        List cocoon = div.addList("cocoon");
        cocoon.setHead(T_COCOON_HEAD);

        cocoon.addLabel(T_COCOON_VERSION);
        cocoon.addItem(org.apache.cocoon.Constants.VERSION);

        // attempt to Display some basic info about Cocoon's Settings & Caches

        // Get access to basic Cocoon Settings
        if (this.settings != null)
        {
            // Output Cocoon's Work Directory & Cache Directory
            cocoon.addLabel(T_COCOON_WORK_DIR);
            cocoon.addItem(this.settings.getWorkDirectory());
            cocoon.addLabel(T_COCOON_CACHE_DIR);
            cocoon.addItem(this.settings.getCacheDirectory());
        }

        // Check if we have access to Cocoon's Default Cache
        // Cocoon's Main (Default) Store is used to store objects that are
        // serializable
        if (this.storeDefault != null)
        {
            // Store name is just the className (remove the package info though,
            // just to save space)
            String storeName = this.storeDefault.getClass().getName();
            storeName = storeName.substring(storeName.lastIndexOf(".") + 1);

            // display main store's cache info
            cocoon.addLabel(T_COCOON_MAIN_CACHE_SIZE.parameterize(storeName
                    + ", 0x"
                    + Integer.toHexString(this.storeDefault.hashCode())));

            // display cache size & link to clear Cocoon's main cache
            Item defaultSize = cocoon.addItem();
            defaultSize.addContent(String.valueOf(this.storeDefault.size())
                    + "  ");
            defaultSize.addXref(this.web_link
                    + "&clearcache=true",
                    T_COCOON_CACHE_CLEAR);
        }

        // Check if we have access to Cocoon's Persistent Cache
        // Cocoon's Persistent Store may be used by the Default Cache/Store to
        // delegate persistent storage
        // (it's an optional store which may not exist)
        if (this.storePersistent != null)
        {
            // Store name is just the className (remove the package info though,
            // just to save space)
            String storeName = this.storeDefault.getClass().getName();
            storeName = storeName.substring(storeName.lastIndexOf(".") + 1);

            // display persistent store's cache size info
            cocoon.addLabel(T_COCOON_PERSISTENT_CACHE_SIZE
                    .parameterize(storeName
                            + ", 0x"
                            + Integer.toHexString(this.storePersistent
                                    .hashCode())));
            cocoon.addItem(String.valueOf(this.storePersistent.size()));
        }

        // Check if we have access to Cocoon's StoreJanitor
        // The Store Janitor manages all of Cocoon's "transient caches/stores"
        // These "transient" stores are used for non-serializable objects or
        // objects whose
        // storage doesn't make sense across a server restart.
        if (this.storeJanitor != null)
        {
            // For each Cache Store in Cocoon's StoreJanitor
            Iterator i = this.storeJanitor.iterator();
            while (i.hasNext())
            {
                // get the Cache Store
                Store store = (Store) i.next();

                // Store name is just the className (remove the package info
                // though, just to save space)
                String storeName = store.getClass().getName();
                storeName = storeName.substring(storeName.lastIndexOf(".") + 1);

                // display its size information
                cocoon.addLabel(T_COCOON_TRANS_CACHE_SIZE
                        .parameterize(storeName + ", 0x"
                                + Integer.toHexString(store.hashCode())));
                cocoon.addItem(String.valueOf(store.size()));
            }
        }
    }

}
