/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletResponse;

import org.purl.sword.atom.Author;
import org.purl.sword.atom.Content;
import org.purl.sword.atom.Contributor;
import org.purl.sword.atom.Generator;
import org.purl.sword.atom.InvalidMediaTypeException;
import org.purl.sword.atom.Link;
import org.purl.sword.atom.Summary;
import org.purl.sword.atom.Title;
import org.purl.sword.base.AtomDocumentRequest;
import org.purl.sword.base.AtomDocumentResponse;
import org.purl.sword.base.Collection;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.ErrorCodes;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.Service;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.base.Workspace;

import org.apache.log4j.Logger;

/**
 * A 'dummy server' which acts as dumb repository which implements the
 * SWORD ServerInterface. It accepts any type of deposit, and tries to
 * return appropriate responses.
 * 
 * It supports authentication: if the username and password match
 * (case sensitive) it authenticates the user, if not, the authentication 
 * fails.
 * 
 * @author Stuart Lewis
 */
public class DummyServer implements SWORDServer {

    /** A counter to count submissions, so the response to a deposit can increment */
    private static int counter = 0;

    /** Logger */
    private static Logger log = Logger.getLogger(ServiceDocumentServlet.class);

    /**
     * Provides a dumb but plausible service document - it contains
     * an anonymous workspace and collection, and one personalised
     * for the onBehalfOf user.
     * 
     * @param sdr The request
     * @throws SWORDAuthenticationException If the credentials are bad
     * @throws SWORDErrorException If something goes wrong, such as 
     */
    public ServiceDocument doServiceDocument(ServiceDocumentRequest sdr)
            throws SWORDAuthenticationException, SWORDErrorException,
            SWORDException
    {
        // Authenticate the user
        String username = sdr.getUsername();
        String password = sdr.getPassword();
        if ((username != null) && (password != null) && 
            (((username.equals("")) && (password.equals(""))) || 
             (!username.equalsIgnoreCase(password))) ) {
                // User not authenticated
                throw new SWORDAuthenticationException("Bad credentials");
        }
        
        // Allow users to force the throwing of a SWORD error exception by setting
        // the OBO user to 'error'
        if ((sdr.getOnBehalfOf() != null) && (sdr.getOnBehalfOf().equals("error"))) {
            // Throw the error exception
            throw new SWORDErrorException(ErrorCodes.MEDIATION_NOT_ALLOWED, "Mediated deposits not allowed");
        }
        
        // Create and return a dummy ServiceDocument
        ServiceDocument document = new ServiceDocument();
        Service service = new Service("1.3", true, true);
        document.setService(service);
        log.debug("sdr.getLocation() is: " + sdr.getLocation());
        String location = sdr.getLocation().substring(0, sdr.getLocation().length() - 16);
        log.debug("location is: " + location);
        
        if (sdr.getLocation().contains("?nested=")) {
            Workspace workspace = new Workspace();
            workspace.setTitle("Nested service document workspace");
            Collection collection = new Collection();
            collection.setTitle("Nested collection: " + sdr.getLocation().substring(sdr.getLocation().indexOf('?') + 1));
            collection.setLocation(location + "/deposit/nested");
            collection.addAcceptPackaging("http://purl.org/net/sword-types/METSDSpaceSIP");
            collection.addAcceptPackaging("http://purl.org/net/sword-types/bagit");
            collection.addAccepts("application/zip");
            collection.addAccepts("application/xml");
            collection.setAbstract("A nested collection that users can deposit into");
            collection.setTreatment("This is a dummy server");
            collection.setCollectionPolicy("No guarantee of service, or that deposits will be retained for any length of time.");
            workspace.addCollection(collection);
            service.addWorkspace(workspace);
        } else {
            Workspace workspace = new Workspace();
            workspace.setTitle("Anonymous submitters workspace");
            Collection collection = new Collection(); 
            collection.setTitle("Anonymous submitters collection");
            collection.setLocation(location + "/deposit/anon");
            collection.addAcceptPackaging("http://purl.org/net/sword-types/METSDSpaceSIP");
            collection.addAcceptPackaging("http://purl.org/net/sword-types/bagit");
            collection.addAccepts("application/zip");
            collection.addAccepts("application/xml");
            collection.setAbstract("A collection that anonymous users can deposit into");
            collection.setTreatment("This is a dummy server");
            collection.setCollectionPolicy("No guarantee of service, or that deposits will be retained for any length of time.");
            collection.setService(location + "/client/servicedocument?nested=anon");
            workspace.addCollection(collection);
            collection = new Collection(); 
            collection.setTitle("Anonymous submitters other collection");
            collection.setLocation(location + "/deposit/anonymous");
            collection.addAcceptPackaging("http://purl.org/net/sword-types/METSDSpaceSIP");
            collection.addAcceptPackaging("http://purl.org/net/sword-types/bagit");
            collection.addAccepts("application/zip");
            collection.addAccepts("application/xml");
            collection.setAbstract("Another collection that anonymous users can deposit into");
            collection.setTreatment("This is a dummy server");
            collection.setCollectionPolicy("No guarantee of service, or that deposits will be retained for any length of time.");
            workspace.addCollection(collection);
            service.addWorkspace(workspace);
            
            if (sdr.getUsername() != null) {
                workspace = new Workspace();
                workspace.setTitle("Authenticated workspace for " + username);
                collection = new Collection(); 
                collection.setTitle("Authenticated collection for " + username);
                collection.setLocation(location + "/deposit/" + username);
                collection.addAccepts("application/zip");
                collection.addAccepts("application/xml");
                collection.addAcceptPackaging("http://purl.org/net/sword-types/METSDSpaceSIP");
                collection.addAcceptPackaging("http://purl.org/net/sword-types/bagit", 0.8f);
                collection.setAbstract("A collection that " + username + " can deposit into");
                collection.setTreatment("This is a dummy server");
                collection.setCollectionPolicy("No guarantee of service, or that deposits will be retained for any length of time.");
                collection.setService(location + "/client/servicedocument?nested=authenticated");
                workspace.addCollection(collection);
                collection = new Collection(); 
                collection.setTitle("Second authenticated collection for " + username);
                collection.setLocation(location + "/deposit/" + username + "-2");
                collection.addAccepts("application/zip");
                collection.addAccepts("application/xml");
                collection.addAcceptPackaging("http://purl.org/net/sword-types/bagit", 0.123f);
                collection.addAcceptPackaging("http://purl.org/net/sword-types/METSDSpaceSIP");
                collection.setAbstract("A collection that " + username + " can deposit into");
                collection.setTreatment("This is a dummy server");
                collection.setCollectionPolicy("No guarantee of service, or that deposits will be retained for any length of time.");
                workspace.addCollection(collection);
            }
            service.addWorkspace(workspace);
        }
        
        String onBehalfOf = sdr.getOnBehalfOf();
        if ((onBehalfOf != null) && (!onBehalfOf.equals(""))) {
            Workspace workspace = new Workspace();
            workspace.setTitle("Personal workspace for " + onBehalfOf);
            Collection collection = new Collection(); 
            collection.setTitle("Personal collection for " + onBehalfOf);
            collection.setLocation(location + "/deposit?user=" + onBehalfOf);
            collection.addAccepts("application/zip");
            collection.addAccepts("application/xml");
            collection.addAcceptPackaging("http://purl.org/net/sword-types/METSDSpaceSIP");
            collection.addAcceptPackaging("http://purl.org/net/sword-types/bagit", 0.8f);
            collection.setAbstract("An abstract goes in here");
            collection.setCollectionPolicy("A collection policy");
            collection.setMediation(true);
            collection.setTreatment("treatment in here too");
            workspace.addCollection(collection);
            service.addWorkspace(workspace);
        }
        
        return document;
    }

    public DepositResponse doDeposit(Deposit deposit) 
                 throws SWORDAuthenticationException, SWORDErrorException, SWORDException {
        // Authenticate the user
        String username = deposit.getUsername();
        String password = deposit.getPassword();
        if ((username != null) && (password != null) && 
            (((username.equals("")) && (password.equals(""))) || 
             (!username.equalsIgnoreCase(password))) ) {
            // User not authenticated
            throw new SWORDAuthenticationException("Bad credentials");
        }
        
        // Check this is a collection that takes obo deposits, else thrown an error
        if (((deposit.getOnBehalfOf() != null) && (!deposit.getOnBehalfOf().equals(""))) && 
            (!deposit.getLocation().contains("deposit?user="))) {
            throw new SWORDErrorException(ErrorCodes.MEDIATION_NOT_ALLOWED,
                                          "Mediated deposit not allowed to this collection");
        }
        
        // Get the filenames
        StringBuffer filenames = new StringBuffer("Deposit file contained: ");
        if (deposit.getFilename() != null) {
            filenames.append("(filename = " + deposit.getFilename() + ") ");
        }
        if (deposit.getSlug() != null) {
            filenames.append("(slug = " + deposit.getSlug() + ") ");
        }

        ZipInputStream zip = null;
        try {
            File depositFile = deposit.getFile();
            zip = new ZipInputStream(new FileInputStream(depositFile));
            ZipEntry ze;
            while ((ze = zip.getNextEntry()) != null) {
                filenames.append(" ").append(ze.toString());
            }
        } catch (IOException ioe) {
            throw new SWORDException("Failed to open deposited zip file", ioe, ErrorCodes.ERROR_CONTENT);
        } finally {
            if (zip != null)
            {
                try
                {
                    zip.close();
                }
                catch (IOException e)
                {
                    log.error("Unable to close zip stream", e);
                }
            }
        }
        
        // Handle the deposit
        if (!deposit.isNoOp()) {
            counter++;
        }
        DepositResponse dr = new DepositResponse(Deposit.CREATED);
        SWORDEntry se = new SWORDEntry();
        
        Title t = new Title();
        t.setContent("DummyServer Deposit: #" + counter);
        se.setTitle(t);
        
        se.addCategory("Category");
        
        if (deposit.getSlug() != null) {
            se.setId(deposit.getSlug() + " - ID: " + counter);
        } else {
            se.setId("ID: " + counter);
        }
         
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        TimeZone utc = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone (utc);
        String milliFormat = sdf.format(new Date());
        se.setUpdated(milliFormat);
            
        Summary s = new Summary();
        s.setContent(filenames.toString());
        se.setSummary(s);
        Author a = new Author();
        if (username != null) {
            a.setName(username);
        } else {
            a.setName("unknown");
        }
        se.addAuthors(a);
        
        Link em = new Link();
        em.setRel("edit-media");
        em.setHref("http://www.myrepository.ac.uk/sdl/workflow/my deposit");
        se.addLink(em);
        
        Link e = new Link();
        e.setRel("edit");
        e.setHref("http://www.myrepository.ac.uk/sdl/workflow/my deposit.atom");
        se.addLink(e);
        
        if (deposit.getOnBehalfOf() != null) {
            Contributor c = new Contributor();
            c.setName(deposit.getOnBehalfOf());
            c.setEmail(deposit.getOnBehalfOf() + "@myrepository.ac.uk");
            se.addContributor(c);
        }
        
        Generator generator = new Generator();
        generator.setContent("Stuart's Dummy SWORD Server");
        generator.setUri("http://dummy-sword-server.example.com/");
        generator.setVersion("1.3");
        se.setGenerator(generator);
        
        Content content = new Content();
        try {
            content.setType("application/zip");
        } catch (InvalidMediaTypeException ex) {
            ex.printStackTrace();
        }
        content.setSource("http://www.myrepository.ac.uk/sdl/uploads/upload-" + counter + ".zip");
        se.setContent(content);
        
        se.setTreatment("Short back and sides");
        
        if (deposit.isVerbose()) {
            se.setVerboseDescription("I've done a lot of hard work to get this far!");
        }
        
        se.setNoOp(deposit.isNoOp());
        
        dr.setEntry(se);
        
        dr.setLocation("http://www.myrepository.ac.uk/atom/" + counter);
        
        return dr;
    }
    
    public AtomDocumentResponse doAtomDocument(AtomDocumentRequest adr) 
                  throws SWORDAuthenticationException, SWORDErrorException, SWORDException {
        // Authenticate the user
        String username = adr.getUsername();
        String password = adr.getPassword();
        if ((username != null) && (password != null) && 
            (((username.equals("")) && (password.equals(""))) || 
            (!username.equalsIgnoreCase(password))) ) {
            // User not authenticated
            throw new SWORDAuthenticationException("Bad credentials");
        }
        
        return new AtomDocumentResponse(HttpServletResponse.SC_OK);
    }

}
