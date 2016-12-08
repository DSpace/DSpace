/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import java.util.List;
import org.apache.log4j.Logger;
import org.purl.sword.atom.Author;
import org.purl.sword.atom.Content;
import org.purl.sword.atom.Contributor;
import org.purl.sword.atom.Generator;
import org.purl.sword.atom.Link;
import org.purl.sword.atom.Rights;
import org.purl.sword.atom.Summary;
import org.purl.sword.atom.Title;
import org.purl.sword.base.Collection;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.Workspace;
import org.purl.sword.base.SwordAcceptPackaging;

/**
 * Example implementation of a command line client. This can send out service 
 * document requests and print out the results and process posting a file to 
 * either a single or multiple destinations. The command line options are 
 * initialised prior to calling the class. The options are passed into the 
 * run(ClientOptions) method. 
 *  
 * @author Neil Taylor
 */
public class CmdClient implements ClientType 
{
    /**
     * The client that is used to process the service and post requests. 
     */
    private SWORDClient client; 

    /**
     * List of the options that can be specified on the command line.
     */
    private ClientOptions options; 
    
    /**
     * The logger.
     */
    private static Logger log = Logger.getLogger(CmdClient.class);
    
    /**
     *  Create a new instance of the class and create an instance of the 
     *  client. 
     */
    public CmdClient( ) 
    {
        client = new Client( );
    }    

    /**
     * Process the options that have been initialised from the command line. 
     * This will call one of service(), post() or multiPost(). 
     */
    public void process()
    {
        if (options.getProxyHost() != null)
        {
            client.setProxy(options.getProxyHost(), options.getProxyPort());
        }

        try
        {
            String accessType = options.getAccessType();
            if (ClientOptions.TYPE_SERVICE.equals(accessType))
            {
                service();
            } 
            else if (ClientOptions.TYPE_POST.equals(accessType))
            {
                post();
            } 
            else if (ClientOptions.TYPE_MULTI_POST.equals(accessType) )
            {
                System.out.println("checking multi-post");
                multiPost();
            }
            else
            {
                System.out.println("Access type not recognised.");
            }

        } 
        catch ( MalformedURLException mex )
        {
            System.out.println("The specified href was not valid: " + options.getHref() + " message: " + mex.getMessage());
        }
        catch (SWORDClientException ex)
        {
            System.out.println("Exception: " + ex.getMessage());
            log.error("Unable to process request", ex);
        }
    }

    /**
     * Process the service operation. Output the results of the service request. 
     * 
     * @throws SWORDClientException if there is an error processing the service request. 
    * @throws MalformedURLException if there is an error with the URL for the service request.
     */
    private void service()
    throws SWORDClientException, MalformedURLException 
    {
        String href = options.getHref();
        initialiseServer(href, options.getUsername(), options.getPassword());

        ServiceDocument document = client.getServiceDocument(href, options.getOnBehalfOf());
        Status status = client.getStatus();
        System.out.println("The status is: " + status);

        if (status.getCode() == 200)
        {
            log.debug("message is: " + document.marshall());
            
            System.out.println("\nThe following Details were retrieved: ");
            System.out.println("SWORD Version: "
                    + document.getService().getVersion());
            System.out.println("Supports NoOp? " + document.getService().isNoOp());
            System.out.println("Supports Verbose? "
                    + document.getService().isVerbose());
            System.out.println("Max Upload File Size "
                    + document.getService().getMaxUploadSize() +" kB");

            Iterator<Workspace> workspaces = document.getService().getWorkspaces();
            for (; workspaces.hasNext();)
            {
                Workspace workspace = workspaces.next();
                System.out.println("\nWorkspace Title: '"
                        + workspace.getTitle() + "'");

                System.out.println("\n+ Collections ---");
                // process the collections
                Iterator<Collection> collections = workspace
                .collectionIterator();
                for (; collections.hasNext();)
                {
                    Collection collection = collections.next();
                    System.out.println("\nCollection location: "
                            + collection.getLocation());
                    System.out.println("Collection title: "
                            + collection.getTitle());
                    System.out
                    .println("Abstract: " + collection.getAbstract());
                    System.out.println("Collection Policy: "
                            + collection.getCollectionPolicy());
                    System.out.println("Treatment: "
                            + collection.getTreatment());
                    System.out.println("Mediation: "
                            + collection.getMediation());

                    String[] accepts = collection.getAccepts();
                    if ( accepts != null && accepts.length == 0 ) 
                    {
                        System.out.println("Accepts: none specified");
                    }
                    else
                    {
                        for (String s : accepts)
                        {
                            System.out.println("Accepts: " + s);
                        }
                    }
                     List<SwordAcceptPackaging> acceptsPackaging = collection.getAcceptPackaging();

                     StringBuilder acceptPackagingList = new StringBuilder();
                     for (Iterator i = acceptsPackaging.iterator();i.hasNext();)
                     {
                        SwordAcceptPackaging accept = (SwordAcceptPackaging) i.next();
                        acceptPackagingList.append(accept.getContent()).append(" (").append(accept.getQualityValue()).append("), ").toString();
                     }

                     System.out.println("Accepts Packaging: "+ acceptPackagingList.toString());
                }
                System.out.println("+ End of Collections ---");
            }
        }
    }
    
    /**
     * Perform a post. If any of the destination URL, the filename and the 
     * filetype are missing, the user will be prompted to enter the values. 
     * 
     * @throws SWORDClientException if there is an error processing the post for a requested 
    *                              destination. 
    * @throws MalformedURLException if there is an error with the URL for the post.
     */
    private void post()
    throws SWORDClientException, MalformedURLException 
    {
        String url = options.getHref();
        if ( url == null )
        {
            url = readLine("Please enter the URL for the deposit: ");
        }

        initialiseServer(url, options.getUsername(), options.getPassword());
        String file = options.getFilename();
        if ( file == null ) 
        {
            file = readLine("Please enter the filename to deposit: ");
        }
        String type = options.getFiletype(); 
        if ( type == null ) 
        {
            type = readLine("Please enter the file type, e.g. application/zip: ");
        }

        PostMessage message = new PostMessage(); 
        message.setFilepath(file);
        message.setDestination(url);
        message.setFiletype(type);
        message.setUseMD5(options.isMd5());
        message.setVerbose(options.isVerbose());
        message.setNoOp(options.isNoOp());
        message.setFormatNamespace(options.getFormatNamespace());
        message.setOnBehalfOf(options.getOnBehalfOf());
        message.setChecksumError(options.getChecksumError());
        message.setUserAgent(ClientConstants.SERVICE_NAME);
        
        processPost(message);

    }

    /**
     * Perform a multi-post. Iterate over the list of -dest arguments in the command line 
     * options. For each -dest argument, attempt to post the file to the server. 
     * 
     * @throws SWORDClientException if there is an error processing the post for a requested 
     *                              destination. 
     * @throws MalformedURLException if there is an error with the URL for the post. 
     */
    private void multiPost()
    throws SWORDClientException, MalformedURLException 
    {
        // request the common information 
        String file = options.getFilename();
        if ( file == null ) 
        {
            file = readLine("Please enter the filename to deposit: ");
        }
        String type = options.getFiletype(); 
        if ( type == null ) 
        {
            type = readLine("Please enter the file type, e.g. application/zip: ");
        }
        
        // process this information for each of the specified destinations
        PostDestination destination;
        String url = null; 
        
        Iterator<PostDestination> iterator = options.getMultiPost();
        while ( iterator.hasNext() )
        {
           destination = iterator.next();
           url = destination.getUrl();
           initialiseServer(url, destination.getUsername(), destination.getPassword());
           
           String onBehalfOf = destination.getOnBehalfOf();
           if ( onBehalfOf == null ) 
           {
              onBehalfOf = "";
           }
           else
           {
               onBehalfOf = " on behalf of: " + onBehalfOf;
           } 
           
           System.out.println("Sending file to: " + url + " for: " + destination.getUsername() + 
                  onBehalfOf );
           PostMessage message = new PostMessage(); 
           message.setFilepath(file);
           message.setDestination(url);
           message.setFiletype(type);
           message.setUseMD5(options.isMd5());
           message.setVerbose(options.isVerbose());
           message.setNoOp(options.isNoOp());
           message.setFormatNamespace(options.getFormatNamespace());
           message.setOnBehalfOf(destination.getOnBehalfOf());
           message.setChecksumError(options.getChecksumError());
           message.setUserAgent(ClientConstants.SERVICE_NAME);
            
           processPost(message);
        }
        
    }

    /**
     * Process the post response. The message contains the list of arguments
     * for the post. The method will then print out the details of the
     * response.
     *
     * @param message The post options.
     * 
     * @throws SWORDClientException if there is an error accessing the
     *                                 post response.
     */
    protected void processPost(PostMessage message)
    throws SWORDClientException
    {
        DepositResponse response = client.postFile(message);

        System.out.println("The status is: " + client.getStatus());
        
        if ( response != null)
        {
            log.debug("message is: " + response.marshall());
            
            // iterate over the data and output it 
            SWORDEntry entry = response.getEntry(); 
            

            System.out.println("Id: " + entry.getId());
            Title title = entry.getTitle(); 
            if ( title != null ) 
            {
                System.out.print("Title: " + title.getContent() + " type: " ); 
                if ( title.getType() != null )
                {
                    System.out.println(title.getType().toString());
                }
                else
                {
                    System.out.println("Not specified.");
                }
            }

            // process the authors
            Iterator<Author> authors = entry.getAuthors();
            while ( authors.hasNext() )
            {
               Author author = authors.next();
               System.out.println("Author - " + author.toString() ); 
            }
            
            Iterator<String> categories = entry.getCategories();
            while ( categories.hasNext() )
            {
               System.out.println("Category: " + categories.next()); 
            }
            
            Iterator<Contributor> contributors = entry.getContributors();
            while ( contributors.hasNext() )
            {
               Contributor contributor = contributors.next(); 
               System.out.println("Contributor - " + contributor.toString());
            }
            
            Iterator<Link> links = entry.getLinks();
            while ( links.hasNext() )
            {
               Link link = links.next();
               System.out.println(link.toString());
            }

            Generator generator = entry.getGenerator();
            if ( generator != null )
            {
                System.out.println("Generator - " + generator.toString());
            }
            else
            {
                System.out.println("There is no generator");
            }

            System.out.println( "Published: " + entry.getPublished());
            
            Content content = entry.getContent();
            if ( content != null ) 
            {
               System.out.println(content.toString());
            }
            else
            {
               System.out.println("There is no content element.");
            }
            
            Rights right = entry.getRights();
            if ( right != null ) 
            {
               System.out.println(right.toString());
            }
            else
            {
               System.out.println("There is no right element.");
            }
               
            Summary summary = entry.getSummary();
            if ( summary != null )
            {
               
               System.out.println(summary.toString());
            }
            else
            {
               System.out.println("There is no summary element.");
            }
           
            System.out.println("Update: " + entry.getUpdated() );
            System.out.println("Published: " + entry.getPublished());
            System.out.println("Verbose Description: " + entry.getVerboseDescription());
            System.out.println("Treatment: " + entry.getTreatment());
            System.out.println("Packaging: " + entry.getPackaging());

            if ( entry.isNoOpSet() )
            {
                System.out.println("NoOp: " + entry.isNoOp());
            }
        }
        else
        {
            System.out.println("No valid Entry document was received from the server");
        }    
    }
    
    /**
     * Initialise the server. Set the server that will be connected to and 
     * initialise any username and password. If the username and password are 
     * either null or contain empty strings, the user credentials will be cleared. 
     *  
     * @param location The location to connect to. This is a URL, of the format, 
    *                 http://a.host.com:port/. The host name and port number will
    *                 be extracted. If the port is not specified, a default port of 
    *                 80 will be used. 
    * @param username The username. If this is null or an empty string, the basic
    *                 credentials will be cleared. 
    * @param password The password. If this is null or an empty string, the basic
    *                 credentials will be cleared.
    *                 
    * @throws MalformedURLException if there is an error processing the URL.
     */
    private void initialiseServer(String location, String username, String password)
    throws MalformedURLException
    {
        URL url = new URL(location);
        int port = url.getPort();
        if ( port == -1 ) 
        {
            port = 80;
        }

        client.setServer(url.getHost(), port);

        if (username != null && username.length() > 0 && 
            password != null && password.length() > 0 )
        {
            log.info("Setting the username/password: " + username + " "
                    + password);
            client.setCredentials(username, password);
        }
        else
        {
            client.clearCredentials();
        }
    }

    /**
     * Read a line of text from System.in. If there is an error reading
     * from the input, the prompt will be redisplayed and the user asked
     * to try again. 
     * 
     * @param prompt The prompt to display before the prompt. 
     * @return The string that is read from the line.
     */
    private String readLine( String prompt )
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        String result = null;

        boolean ok = false;
        while (!ok) 
        {
            try 
            {
                System.out.print(prompt);
                System.out.flush();
                result = reader.readLine();
                ok = true;
            } catch (IOException ex) {
                System.out.println("There was an error with your input. Please try again.");
            }
        }

        return result;
    }

   /**
    * Run the client and process the specified options. 
    * 
    * @param options The command line options. 
    */
    public void run( ClientOptions options )
    {
        this.options = options; 
        process( );
    }
}
