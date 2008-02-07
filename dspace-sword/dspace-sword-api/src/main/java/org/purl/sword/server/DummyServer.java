package org.purl.sword.server;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.purl.sword.base.Collection;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.ErrorCodes;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.Service;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.base.ServiceLevel;
import org.purl.sword.base.Workspace;
import org.w3.atom.Author;
import org.w3.atom.Content;
import org.w3.atom.Contributor;
import org.w3.atom.Generator;
import org.w3.atom.InvalidMediaTypeException;
import org.w3.atom.Link;
import org.w3.atom.Source;
import org.w3.atom.Summary;
import org.w3.atom.Title;

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

	/** A counter to count submissions, so the response to a deposito can increment */
	private static int counter = 0;

	/**
	 * Provides a dumb but plausible service document - it contains
	 * an anonymous workspace and collection, and one personalised
	 * for the onBehalfOf user.
	 * 
	 * @param onBehalfOf The user that the client is acting on behalf of
	 * @throws SWORDAuthenticationException 
	 */
	public ServiceDocument doServiceDocument(ServiceDocumentRequest sdr) throws SWORDAuthenticationException {
		// Authenticate the user
		String username = sdr.getUsername();
		String password = sdr.getPassword();
		if ((username != null) && (password != null) && 
			(((username.equals("")) && (password.equals(""))) || 
		     (!username.equalsIgnoreCase(password))) ) {
				// User not authenticated
				throw new SWORDAuthenticationException("Bad credentials");
		}
		
		// Create and return a dummy ServiceDocument
		ServiceDocument document = new ServiceDocument();
		Service service = new Service(ServiceLevel.ZERO, true, true);
	    document.setService(service);
	    
	    Workspace workspace = new Workspace();
	    workspace.setTitle("Anonymous submitters workspace");
	    Collection collection = new Collection(); 
	    collection.setTitle("Anonymous submitters collection");
	    collection.setLocation("http://sword.aber.ac.uk/sword/deposit?user=anon");
	    workspace.addCollection(collection);
	    collection = new Collection(); 
	    collection.setTitle("Anonymous submitters other collection");
	    collection.setLocation("http://sword.aber.ac.uk/sword/deposit?user=anonymous");
	    workspace.addCollection(collection);
	    service.addWorkspace(workspace);
	     
	    if (sdr.getUsername() != null) {
	    	workspace = new Workspace();
		    workspace.setTitle("Authenticated workspace for " + username);
		    collection = new Collection(); 
		    collection.setTitle("Authenticated collection for " + username);
		    collection.setLocation("http://sword.aber.ac.uk/sword/deposit?user=" + username);
		    workspace.addCollection(collection);
		    collection = new Collection(); 
		    collection.setTitle("Second authenticated collection for " + username);
		    collection.setLocation("http://sword.aber.ac.uk/sword/deposit?user=" + username + "-2");
		    workspace.addCollection(collection);
		    service.addWorkspace(workspace);
	    }
	    
	    String onBehalfOf = sdr.getOnBehalfOf();
	    if ((onBehalfOf != null) && (!onBehalfOf.equals(""))) {
		    workspace = new Workspace();
		    workspace.setTitle("Personal workspace for " + onBehalfOf);
		    collection = new Collection(); 
		    collection.setTitle("Personal collection for " + onBehalfOf);
		    collection.setLocation("http://sword.aber.ac.uk/sword/deposit?user=" + onBehalfOf);
		    collection.addAccepts("application/zip");
		    collection.addAccepts("application/xml");
		    collection.setAbstract("An abstract goes in here");
		    collection.setCollectionPolicy("A collection policy");
		    collection.setMediation(true);
		    collection.setTreatment("treatment in here too");
		    workspace.addCollection(collection);
		    service.addWorkspace(workspace);
	    }
	    
	    return document;
	}

	public DepositResponse doDeposit(Deposit deposit) throws SWORDAuthenticationException, SWORDException {
		// Authenticate the user
		String username = deposit.getUsername();
		String password = deposit.getPassword();
		if ((username != null) && (password != null) && 
			(((username.equals("")) && (password.equals(""))) || 
			 (!username.equalsIgnoreCase(password))) ) {
			// User not authenticated
			throw new SWORDAuthenticationException("Bad credentials");
		}
		
		// Get the filenames
		StringBuffer filenames = new StringBuffer("Deposit file contained: ");
		if (deposit.getFilename() != null) {
			filenames.append("(filename = " + deposit.getFilename() + ") ");
		}
		if (deposit.getSlug() != null) {
			filenames.append("(slug = " + deposit.getSlug() + ") ");
		}
		try {
			ZipInputStream zip = new ZipInputStream(deposit.getFile());
			ZipEntry ze;
			while ((ze = zip.getNextEntry()) != null) {
				filenames.append(" " + ze.toString());
			}
		} catch (IOException ioe) {
			throw new SWORDException("Failed to open deposited zip file", null, ErrorCodes.ERROR_CONTENT);
		}
		
		// Handle the deposit
		if (!deposit.isNoOp()) {
			counter++;
		}
		DepositResponse dr = new DepositResponse(Deposit.ACCEPTED);
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
		try {
			se.setUpdated(milliFormat);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
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
		
		Source source = new Source();
		Generator generator = new Generator();
		generator.setContent("org.purl.sword.server.DummyServer");
		source.setGenerator(generator);
		se.setSource(source);
		
		Content content = new Content();
		try {
			content.setType("application/zip");
		} catch (InvalidMediaTypeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		content.setSource("http://www.myrepository.ac.uk/sdl/uploads/upload-" + counter + ".zip");
		se.setContent(content);
		
		se.setTreatment("Short back and sides");
		
		if (deposit.isVerbose()) {
			se.setVerboseDescription("I've done a lot of hard work to get this far!");
		}
		
		se.setNoOp(deposit.isNoOp());
		
		se.setFormatNamespace("http://www.standards-body.com/standardXYZ/v1/");
		
		dr.setEntry(se);
		
		return dr;
	}
}
