package uk.ac.edina.datashare.sword;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.DSpaceMETSIngester;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.core.Context;
import org.jdom.Element;

import uk.ac.edina.datashare.utils.VirusChecker;

/**
 * DataShare specific METS ingester.
 */
public class METSIngester extends DSpaceMETSIngester{
	
	private static final Logger LOG = Logger.getLogger(METSIngester.class);
    
    @Override
    public DSpaceObject replace(
    		Context context,
    		DSpaceObject dsoToReplace,
            File pkgFile,
            PackageParameters params)
            throws PackageValidationException, CrosswalkException,
            AuthorizeException, SQLException, IOException
    {
    	LOG.info(pkgFile);
    	LOG.info(pkgFile.exists());
    	LOG.info(pkgFile.canRead());
        if(!new VirusChecker(pkgFile).isVirusFree()){
            throw new PackageValidationException("Deposit has failed virus check.");
        }
        
    	return super.replace(context, dsoToReplace, pkgFile, params);
    }
    /*
     * (non-Javadoc)
     * @see org.dspace.content.packager.AbstractMETSIngester#ingest(org.dspace.core.Context, org.dspace.content.DSpaceObject, java.io.File, org.dspace.content.packager.PackageParameters, java.lang.String)
     */
    @Override
    public DSpaceObject ingest(
            Context context,
            DSpaceObject parent,
            File pkgFile,
            PackageParameters params,
            String license)
            throws PackageValidationException, CrosswalkException,
            AuthorizeException, SQLException, IOException
    {
        if(!new VirusChecker(pkgFile).isVirusFree()){
            throw new PackageValidationException("Deposit has failed virus check.");
        }
        
        return super.ingest(context, parent, pkgFile, params, license);
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.content.packager.DSpaceMETSIngester#crosswalkObjectDmd(org.dspace.core.Context, org.dspace.content.DSpaceObject, org.dspace.content.packager.METSManifest, org.dspace.content.packager.AbstractMETSIngester.MdrefManager, org.jdom.Element[], org.dspace.content.packager.PackageParameters)
     */
    @Override
    public void crosswalkObjectDmd(
            Context context,
            DSpaceObject dso,
            METSManifest manifest,
            MdrefManager callback,
            Element dmds[],
            PackageParameters params)
        throws CrosswalkException, PackageValidationException,
        AuthorizeException, SQLException, IOException
    {
        super.crosswalkObjectDmd(context, dso, manifest, callback, dmds, params);
        Item item = (Item)dso;
        SwordUtil.isMetadataValid(context, item);
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.content.packager.DSpaceMETSIngester#addLicense(org.dspace.core.Context, org.dspace.content.Item, java.lang.String, org.dspace.content.Collection, org.dspace.content.packager.PackageParameters)
     */
    @Override
    public void addLicense(Context context, Item item, String license,
            Collection collection, PackageParameters params)
                    throws PackageValidationException,  AuthorizeException, SQLException, IOException
    {
        // do not create depositor agreement
    }
}
