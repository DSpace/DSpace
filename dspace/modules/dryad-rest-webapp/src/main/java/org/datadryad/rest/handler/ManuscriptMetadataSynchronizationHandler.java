/*
 */
package org.datadryad.rest.handler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.apache.lucene.util.ArrayUtil;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

/**
 * Extracts metadata from manuscript objects and places in corresponding Dryad
 * Data Package submission (if DOI present)
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptMetadataSynchronizationHandler implements HandlerInterface<Manuscript>{
    private static final Logger log = Logger.getLogger(ManuscriptMetadataSynchronizationHandler.class);

    // Move to an abstract base class
    private static Context getContext() {
        Context context = null;
        try {
            context = new Context();
        } catch (SQLException ex) {
            log.error("Unable to instantiate DSpace context", ex);
        }
        return context;
    }

    private static void completeContext(Context context) throws SQLException {
        try {
            context.complete();
        } catch (SQLException ex) {
            // Abort the context to force a new connection
            abortContext(context);
            throw ex;
        }
    }

    private static void abortContext(Context context) {
        context.abort();
    }

    @Override
    public void handleCreate(StoragePath path, Manuscript manuscript) throws HandlerException {
        processChange(manuscript);
    }

    @Override
    public void handleUpdate(StoragePath path, Manuscript manuscript) throws HandlerException {
        processChange(manuscript);
    }

    @Override
    public void handleDelete(StoragePath path, Manuscript object) throws HandlerException {
        // TODO: Anything for deletions?
    }


    private static IdentifierService getIdentifierService() {
        DSpace dspace = new DSpace();
        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;
        return manager.getServiceByName(IdentifierService.class.getName(), IdentifierService.class);
    }


    private static DryadDataPackage findDataPackage(Manuscript manuscript, Context context) throws HandlerException {
        String doi = manuscript.dryadDataDOI;
        DryadDataPackage dataPackage = null;
        try {
            // First look up by DOI
            if(manuscript.dryadDataDOI != null) {
                DSpaceObject object = getIdentifierService().resolve(context, doi);
                if(object.getType() == Constants.ITEM) {
                    dataPackage = new DryadDataPackage((Item)object);
                } else {
                    throw new HandlerException("DOI " + doi + " does not resolve to an item");
                }
            } else {
                try {
                    dataPackage = DryadDataPackage.findByManuscriptNumber(context, manuscript.manuscriptId);
                } catch (SQLException ex) {
                    throw new HandlerException("SQLException finding package by manuscript ID", ex);
                }
            }
        } catch (IdentifierNotFoundException ex) {
            throw new HandlerException("Unable to find data package with DOI: " + doi, ex);
        } catch (IdentifierNotResolvableException ex) {
            throw new HandlerException("Unable to resolve DOI: " + doi, ex);
        }
        return dataPackage;
    }


    private void processChange(Manuscript manuscript) throws HandlerException {
        try {
            Context context = getContext();
            DryadDataPackage dataPackage = findDataPackage(manuscript, context);
            if(dataPackage == null) {
                throw new HandlerException("Data package not found for manuscript: " + manuscript.manuscriptId);
            }
            // Check if rejected or accepted
            if(manuscript.isRejected()) {
                disassociateFromManuscript(dataPackage, manuscript);
            } else {
                associateWithManuscript(dataPackage, manuscript);
            }
            completeContext(context);
        } catch (SQLException ex) {
            throw new HandlerException("SQLException updating publicationDate", ex);
        }
    }

    /**
     * Copies manuscript metadata into a dryad data package
     * @param dataPackage
     * @param manuscript
     * @throws SQLException
     */
    private void associateWithManuscript(DryadDataPackage dataPackage, Manuscript manuscript) throws SQLException {
        // set publication DOI
        dataPackage.setPublicationDOI(manuscript.publicationDOI);
        // set Manuscript ID
        dataPackage.setManuscriptNumber(manuscript.manuscriptId);
        // union keywords
        List<String> packageKeywords = dataPackage.getKeywords();
        List<String> manuscriptKeywords = manuscript.keywords.keyword;
        List<String> unionKeywords = unionLists(packageKeywords, manuscriptKeywords);
        dataPackage.setKeywords(unionKeywords);
        // set title
        dataPackage.setTitle(manuscript.title);
        // set abstract
        dataPackage.setAbstract(manuscript.manuscript_abstract);
        // set publicationDate
        dataPackage.setBlackoutUntilDate(manuscript.publicationDate);
    }

    private void disassociateFromManuscript(DryadDataPackage dataPackage, Manuscript manuscript) throws SQLException {
        // clear publication DOI
        dataPackage.setPublicationDOI(null);
        // clear Manuscript ID
        dataPackage.setManuscriptNumber(null);
        // disjoin keywords
        List<String> packageKeywords = dataPackage.getKeywords();
        List<String> manuscriptKeywords = manuscript.keywords.keyword;
        List<String> prunedKeywords = subtractList(packageKeywords, manuscriptKeywords);
        
        dataPackage.setKeywords(prunedKeywords);
        // clear publicationDate
        dataPackage.setBlackoutUntilDate(null);
    }

    private static List<String> unionLists(List<String> list1, List<String> list2) {
        Set<String> set = new HashSet<String>();
        set.addAll(list1);
        set.addAll(list2);
        return new ArrayList<String>(set);
    }

    private static List<String> subtractList(List<String> list1, List<String> list2) {
        List<String> list = new ArrayList<String>(list1);
        for(String string : list2) {
            if(list.contains(string)) {
                list.remove(string);
            }
        }
        return list;
    }
}
