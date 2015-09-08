package uk.ac.edina.datashare.doi;

import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;

import uk.ac.edina.datashare.utils.DSpaceUtils;
import uk.ac.edina.datashare.utils.MetaDataUtil;

/**
 * Datashare doi utility.
 */
public class DoiUpdate{
    private Context context;
    
    public DoiUpdate(Context context){
        this.context = context;
    }
    
    public static void main(String[] argv)
    {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("d", "register-dois", false,
                "Register dois for items that have no doi");
        options.addOption("c", "create citations", false,
                "Create citation for items that have a newly created doi");

        try{
            DoiUpdate du = new DoiUpdate(new Context());
            HelpFormatter helpformater = new HelpFormatter();
            try{
                CommandLine line = parser.parse(options, argv);
                if (line.hasOption('d'))
                {
                    du.registerDios();
                }
                else if(line.hasOption('c')){
                    du.createCitations();
                }
                else{
                    helpformater.printHelp("\nDataShare DOI\n", options);
                }
            }
            catch(ParseException ex){
                System.out.println(ex);
                helpformater.printHelp("\nDataShare DOI\n", options);
            }
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Register a doi for each item that doesn't have a doi.  
     */
    private void registerDios(){
        this.context.turnOffAuthorisationSystem();
        
        try{
            DOIIdentifierProvider doiProvider = new DSpace().getSingletonService(DOIIdentifierProvider.class);
            
            ItemIterator iter = Item.findAll(this.context);
            while(iter.hasNext()){
                Item item = iter.next(); 
                
                if(!DSpaceUtils.hasEmbargo(context, item)){
                    String doi = null;

                    try{
                        doi = doiProvider.lookup(this.context, item);
                    }
                    catch(IdentifierNotResolvableException ex){

                    }
                    catch(IdentifierNotFoundException ex){

                    }

                    if(doi == null){
                        try{
                            doiProvider.register(context, item);
                        }
                        catch(IdentifierException ex){
                            System.err.println("*** Unable to register doi for " + item.getID());
                        }
                    }
                    else{
                        System.out.println("Item " + item.getID() + " has " + doi);
                    }
                }
            }
            
            this.context.complete();
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }
        finally{
            this.context.restoreAuthSystemState();
        }

    }
    
    /**
     * Create a citation for all items that have a new doi.  
     */
    private void createCitations(){
        context.turnOffAuthorisationSystem();
        
        try{
            ItemIterator iter = Item.findAll(context);
            while(iter.hasNext()){
                Item item = iter.next();

                if(!DSpaceUtils.hasEmbargo(context, item)){
                    String citation = MetaDataUtil.getCitation(item);

                    if(citation == null){
                        DSpaceUtils.updateCitation(item);
                    }
                    else if(!citation.contains(DSpaceUtils.DOI_URL) &&
                            DSpaceUtils.hasDoi(item)){
                        MetaDataUtil.clearCitation(item);
                        DSpaceUtils.updateCitation(item);
                        System.out.println("Item " + item.getID() + " has new citation: " +
                                MetaDataUtil.getCitation(item));
                    }
                    else{
                        if(!DSpaceUtils.hasDoi(item)){
                            System.out.println("Ignoring " + item.getID() +
                                    ". contains doi: " + citation.contains(DSpaceUtils.DOI_URL) +
                                    ". hasDoi: " + DSpaceUtils.hasDoi(item));
                        }
                        continue;
                    }

                    try{
                        item.update();
                    }
                    catch(AuthorizeException ex){
                        throw new RuntimeException(ex);
                    }
                }
            }
            
            context.complete();
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }
        finally{
            context.restoreAuthSystemState();
        }
    }
}
