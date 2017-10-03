package org.dspace.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;

import uk.ac.edina.datashare.db.DbQuery;
import uk.ac.edina.datashare.db.DbUpdate;
import uk.ac.edina.datashare.utils.DSpaceUtils;

/**
 * DataShare item dataset. That is a zip file that contains all item bitstreams.
 */
public class ItemDataset {
    private static final Logger LOG = Logger.getLogger(ItemDataset.class);
    private Context context = null;
    private Item item = null;
    private String handle = null;
    private static final String TMP_FILE_NAME_EXT = ".tmp";
    private static final String DIR_PROP = "datasets.path";
    private static String dir = null;
    
    /**
     * Initialise dataset with DSpace context and item.
     * @param context
     * @param item
     */
    public ItemDataset(Context context, Item item){
        this.context = context;
        this.item = item;
        this.init();
    }

    /**
     * Initialise dataset with DSpace context and item handle.
     * @param context
     * @param handle
     */
    public ItemDataset(Context context, String handle){
        this.context = context;
        this.handle = handle;
        this.init();
    }
    
    /**
     * Initialise dataset with DSpace context and dataset zip file.
     * @param context
     * @param ds
     */
    public ItemDataset(Context context, File ds){
        this.context = context;
        this.setHandle(ds);
        this.init();
    }

    public ItemDataset(Item item){
        this.item = item;
        this.init();
    }
    
    public ItemDataset(String handle){
        this.handle = handle;
        this.init();
    }
    
    public ItemDataset(Context context, Bitstream bitstream){
        try{
            DSpaceObject ob = bitstream.getParentObject();
            if(ob instanceof Item){
                this.context = context;
                this.item = (Item)ob;
                this.init();
            }
            else{
                throw new RuntimeException("Only items can be datasets.");
            }
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Initialise dataset.
     */
    private void init(){
        dir = org.dspace.core.ConfigurationManager.getProperty(DIR_PROP);
        if(dir == null){
            throw new RuntimeException(DIR_PROP + " needs to be defined");
        }
    
        if(!new File(dir).exists()){
            throw new RuntimeException(dir + " doesn't exist");
        }
    }
    
    /**
     * Check if item has been put under embargo or tombstoned.  If so, delete dataset.
     */
    public void checkDataset(){
        if (this.exists()){
            if(DSpaceUtils.hasEmbargo(this.context, this.item) ||
                    DSpaceUtils.showTombstone(context, item)){
                LOG.info("Delete dataset for " + item.getHandle());
                this.delete();
            }
        }
        else{
            LOG.warn("No dataset exists to check " + this.item.getHandle());
        }
    }
        
    /**
     * Create dataset zip file.
     * @return Thread that dataset is created on. 
     */
    public Thread createDataset(){
        Thread th = new Thread(new DatasetZip());
        th.start();
        return th;
    }
    
    /**
     * Delete dataset from system.
     */
    public void delete(){
        File zip = null;
        if(this.item != null){
            zip = new File(this.getFullPath());
        }
        else{
            zip = new File(dir + File.separator + ItemDataset.getFileName(this.handle));
        }

        if(!zip.delete()){
            LOG.warn("Problem deleting " + zip);
        }
        else{
            String fp = zip.toString();
            String fname = fp.substring(fp.lastIndexOf('/') + 1);
            DbUpdate.deleteDataset(context, fname);
        }
    }
    
    public boolean exists(){
        return new File(getFullPath()).exists();
    }
    
    public static boolean exists(String handle){
        return new File(dir + getFileName(handle)).exists();
    }
 
    /**
     * @param bitstream The bitstream to check.
     * @return True is bitstream should be added to the zip file.
     */
    private boolean includeBitstream(Context context, Bitstream bitstream)
    {
        final String WBFF = "Written by FormatFilter";
        return (bitstream.getSource() != null &&
                !bitstream.getSource().startsWith(WBFF) &&
                !bitstream.getName().equals(Constants.LICENSE_BITSTREAM_NAME));
    }
    
    public String getChecksum(){
        return DbQuery.fetchDatasetChecksum(context, item);
    }
    
    private String getFileName(){
        return ItemDataset.getFileName(this.item.getHandle());
    }

    private String getHandle(){
        return this.handle;
    }

    public static String getFileName(String handle){
        String aHandle[] = handle.split("/");
        return "DS_" + aHandle[0] + "_" + aHandle[1] + ".zip";
    }

    private String getFullPath(){
        return dir + File.separator + getFileName();
    }
    
    /**
     * @return size in bytes of dataset zip file.
     */
    public long getSize(){
        return new File(getFullPath()).length();
    }

    /**
     * @return Temporary dataset file name.
     */
    public String getTmpFileName(){
        return getFullPath() + TMP_FILE_NAME_EXT;
    }

    /**
     * @return size in bytes of dataset tmp zip file.
     */
    public long getTmpSize(){
        return new File(getTmpFileName()).length();
    }    
    
    public URL getURL(){
        URL url;
        try{
            String bUrl[] = ConfigurationManager.getProperty("dspace.baseUrl").split("://");
            String protocol = bUrl[0];
            String host = bUrl[1];
            String fPath = "/download/" + getFileName();
            if(host.contains(":")){
                String aHost[] = host.split(":");
                host = aHost[0];
                url = new URL(protocol, host, Integer.parseInt(aHost[1]), fPath);
            }
            else{
                url = new URL(protocol, host, fPath);
            }

        }
        catch(MalformedURLException ex){
            throw new RuntimeException(ex);
        }
        
        return url;
    }
    
    private boolean itemIsAvailable(Context context, Item item){
        return !DSpaceUtils.hasEmbargo(context, item) &&
                !item.isWithdrawn() &&
                !DSpaceUtils.showTombstone(context, item); 
    }
    
    /**
     * Create a monitor on dataset creation, to track progress.
     * @return Thread that monitor is created on.
     */
    public Thread monitorDataset(){
        Thread th = new Thread(new DatasetMonitor());
        th.start();
        return th;
    }
    
    /**
     * Given a dataset file object, set handle. 
     */
    private void setHandle(File ds){
        Pattern p = Pattern.compile(".+DS_(\\d+)_(\\d+).zip");
        Matcher matcher = p.matcher(ds.getAbsolutePath());
        while (matcher.find()) {
            this.handle = matcher.group(1) + "/" + matcher.group(2);
        }
    }

    /**
     * Create dataset zip file in a seperate thread.
     */
    private class DatasetZip implements Runnable {
        /**
         * Start thread.
         */
        public void run() {
            Context context = null;
            try{
                context = new Context();
                
                // get item using new context, otherwise potential
                // for context out-with our control to be closed 
                item = Item.find(context, item.getID());
                
                if(itemIsAvailable(context, item)){
                    LOG.info("create zip for " + item.getHandle());
                    createZip(context);                    
                    String cksum = createChecksum(context);
                    
                    LOG.info("zip complete");
                    DbUpdate.insertDataset(
                            context, item.getID(), getFileName(), cksum);
                }
                else{
                    ItemDataset.LOG.warn("Zip creation for " + item.getHandle() + " not allowed.");
                }
            }
            catch(SQLException ex){
                LOG.error(ex);
                throw new RuntimeException(ex);
            }
            finally {
                try{
                    context.complete();
                }
                catch(SQLException ex){
                    LOG.warn(ex);
                }
            }
        }
        
        private String createChecksum(Context context){
            String cksum = null;
            try{
                FileInputStream fis = new FileInputStream(new File(getFullPath()));
                cksum = DigestUtils.md5Hex(fis);
                fis.close();
            }
            catch(IOException ex){
                throw new RuntimeException(ex);
            }
            
            return cksum;
        }
        
        private void createZip(Context context){
            String tmpZip = getTmpFileName();
            
            try{
                final byte[] BUFFER = new byte[8192];
                
                FileOutputStream fos = new FileOutputStream(tmpZip);
                ZipOutputStream zos = new ZipOutputStream(fos);
                zos.setLevel(0);
                
                Bundle bundle[] = item.getBundles();
                
                // loop round bundles, there should be two - files and licences
                for(int i = 0; i < bundle.length; i++){
                    // now get the actual bitstreams
                    Bitstream bitstreams[] = bundle[i].getBitstreams();
                    for(int j = 0; j < bitstreams.length; j++){
                        // only add bitstream if valid 
                        if(includeBitstream(context, bitstreams[j])){ 
                            LOG.info("do " + bitstreams[j].getName());
                            ZipEntry entry = new ZipEntry(bitstreams[j].getName());

                            zos.putNextEntry(entry);
                            InputStream in = bitstreams[j].retrieve();
                            int length = -1;
                            while ((length = in.read(BUFFER)) > -1){
                                zos.write(BUFFER, 0, length);
                            }

                            zos.closeEntry();
                            in.close();
                        }
                    }
                }
                
                zos.close();
                fos.close();
                
                // rename zip with temporary file to final name 
                if(!new File(tmpZip).renameTo(new File(getFullPath()))){
                    LOG.error("Problem renaming " + tmpZip + " to " + getFullPath());
                }
                LOG.info(getFileName() + " complete");
            }
            catch(AuthorizeException ex){
                LOG.error(ex);
                throw new RuntimeException(ex);
            }
            catch(SQLException ex){
                LOG.error(ex);
                throw new RuntimeException(ex);
            }
            catch(FileNotFoundException ex){
                LOG.error(ex);
                throw new RuntimeException(ex);
            }
            catch(IOException ex){
                final String msg = "Problem with " + tmpZip + ": " + ex.getMessage(); 
                System.err.println(msg);
                LOG.error(msg);
                throw new RuntimeException(msg);
            }            
        }
    }
    
    /**
     * This will monitor the progress of a creation of a dataset printing out
     * its size.
     */
    private class DatasetMonitor implements Runnable {
        public void run() {
            boolean cont = true;
            int sleep = 5000;
            System.out.println("Checking dataset " + item.getHandle() + " ...");
            while(cont){
                if(exists()){
                    System.out.println("dataset exists");
                    cont = false;
                }
                else if(!itemIsAvailable(context, item)){
                    System.out.println("dataset creation not allowed");
                    cont = false;
                }
                else{
                    try{
                        Thread.sleep(sleep);
                        System.out.println("size: " + getTmpSize());
                    }
                    catch(InterruptedException ex){
                        System.err.println(ex);
                    }
                }
            }
        }
    }
    
    /**
     * Process all datasets in the system.
     */
    public static void main(String[] args){
        Context context = null;
        try{    
            DSpaceKernelImpl kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning())
            {
                kernelImpl.start(ConfigurationManager.getProperty("dspace.dir"));
            }
            context = new Context();
                        
            List <String> itemHandles = new ArrayList<String>(10000);
            ItemIterator iter = Item.findAll(context);
            while(iter.hasNext()){
                Item item = iter.next();
                if(item.isArchived()){
                    String handle = item.getHandle();
                    if(handle == null){
                        System.err.println("*** Item with id " + item.getID() + " has no handle");
                        continue;
                    }
                    itemHandles.add(item.getHandle());
                    ItemDataset ds = new ItemDataset(context, item);
                    if(ds.exists()){
                        if(DSpaceUtils.showTombstone(context, item)){
                            System.out.println("Delete tombstoned dataset: " + item.getHandle());
                            ds.delete();
                        }
                        else{
                            System.out.println("Dataset already exists " + item.getHandle());
                        }
                    }
                    else{
                        if(ds.itemIsAvailable(context, item)){
                            System.out.println("Create dataset for " + ds.getFullPath() +
                                    " for " + item.getHandle() + ", id: " + item.getID());
                            Thread th = ds.createDataset();
                            try{
                                th.join();
                            }
                            catch(InterruptedException ex){
                                System.out.println(ex);
                            } 
                        }
                        else{
                            System.out.println("Item is currently unavailable: " + item.getHandle());
                        }
                    }
                }
            }
            
            // now see if any datasets are orphaned, just in case
            File datasets[] = new File(dir).listFiles();
            for (File zip : datasets) {
                if(zip.getName().endsWith(TMP_FILE_NAME_EXT)){
                    // if file is a temporary file delete it if more than one day old
                    long diff = new Date().getTime() - zip.lastModified();
                    if (diff > 24 * 60 * 60 * 1000) {
                        zip.delete();
                    } 
                }
                else if(!zip.getName().equals("README.txt")){
                    ItemDataset ds = new ItemDataset(context, zip);
                    if(!itemHandles.contains(ds.getHandle())){
                        System.err.println("*** dataset " + zip + " exists with no item. Delete it.");
                        ds.delete();
                    }
                }
            }            
            
        }
        catch(SQLException ex){
            System.out.println(ex);
        }
        finally{
            try{
                context.complete();
            }
            catch(SQLException ex){
            }
        }
        
        System.out.println("exit");
    }
}
