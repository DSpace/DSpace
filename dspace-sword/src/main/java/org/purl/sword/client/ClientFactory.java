/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

import org.apache.log4j.PropertyConfigurator;

/**
 * Entry point for the SWORD Demonstration Client. This will parse the list of
 * command line options and load either a Command Line client or a GUI client.
 * 
 * @author Neil Taylor
 */
public class ClientFactory {

    /**
     * Create a new instance.
     */
    public ClientFactory() {
        // configure the logger from the property file. The GUI client will
        // reload these properties if it is set to capture the output and
        // display it in a panel.
        PropertyConfigurator.configure(this.getClass().getClassLoader()
            .getResource(ClientConstants.LOGGING_PROPERTY_FILE));
    }

    /**
     * Generate a string that specifies the command line options for this
     * program.
     * 
     * @return A list of the options for this program.
     */
    public static String usage() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("swordclient: version ");
        buffer.append(ClientConstants.CLIENT_VERSION);
        buffer.append("\n");

        buffer.append("GUI Mode: ");
        buffer.append("swordclient [-gui] [-nocapture]");
        buffer.append("\n\n");

        buffer.append("Command Mode: Service - Request a Service Document\n");
        buffer.append("swordclient -cmd -t service [user-options] [proxy-options] -href url [-onBehalfOf name] ");
        buffer.append("\n\n");

        buffer.append("Command Mode: Post - Post a file to a remote service.\n");
        buffer.append("swordclient -cmd -t post [user-options] [proxy-options] [post-options] \n");
        buffer.append("            [-file file] [-filetype type] [-onBehalfOf name]");
        buffer.append("\n\n");

        buffer.append("Command Mode: MultiPost - Post a file to multiple remote services.\n");
        buffer.append("swordclient -cmd -t multipost [user-options] [proxy-options] [post-options] \n");
        buffer.append("            [-dest dest]");

        buffer.append("\n\n");
        buffer.append("User options: \n");
        buffer.append("   -u username          Specify a username to access the remote service.\n");
        buffer.append("   -p password          Specify a password to access the remote service.\n");
        buffer.append("                        Required if -u option is used.");

        buffer.append("\n\n");
        buffer.append("Proxy options: \n");
        buffer.append("   -host host           Hostname of a proxy, wwwproxy.aber.ac.uk.\n");
        buffer.append("   -port port           Proxy port number, e.g. 8080.\n");

        buffer.append("\n\n");
        buffer.append("Post options: \n");
        buffer.append("   -noOp                Specified to indicate that the post is a test operation.\n");
        buffer.append("   -md5                 Use an MD5 checksum in the message header.\n");
        buffer.append("   -checksumError       Mis-calculate the file checksum for server test purposes.\n");
        buffer.append("   -formatNamespace ns  The format namespace value.\n");
        buffer.append("   -slug name           The slug value.\n");
        buffer.append("   -verbose             Request a verbose response from the server.\n");

        buffer.append("\n\n");
        buffer.append("Other options: \n");
        buffer.append("    -help               Show this message.\n");
        buffer.append("    -t type             The type of operation: service, post or multipost.\n");
        buffer.append("    -href url           The URL for the service or post document.\n");
        buffer.append("                        Required for service. The post and multipost operations \n");
        buffer.append("                        will prompt you if the value is not provided.\n");
        buffer.append("    -filetype type      The filetype, e.g. application/zip. The post and multipost\n");
        buffer.append("                        will prompt you for the value if it is not provided.\n");
        buffer.append("    -onBehalfOf name    Specify this parameter to set the On Behalf Of value.\n");
        buffer.append("    -dest dest          Specify the destination for a deposit. This can be repeated\n");
        buffer.append("                        multiple times. The format is: \n");
        buffer.append("                        <username>[<onbehalfof>]:<password>@<href>\n");
        buffer.append("                        e.g. sword[nst]:swordpass@http://sword.aber.ac.uk/post/\n");
        buffer.append("                             nst:pass@http://sword.aber.ac.uk/post\n");
        buffer.append("    -nocapture          Do not capture System.out and System.err to a debug panel\n");
        buffer.append("                        in the GUI panel.");

        return buffer.toString();
    }

    /**
     * Create a client. If GUI mode is set, a GUI client is created. Otherwise,
     * a command line client is created.
     * 
     * @param options
     *            The list of options extracted from the command line.
     * 
     * @return A new client.
     */
    public ClientType createClient(ClientOptions options) {
        return new CmdClient();
    }

    /**
     * Start the application and determine which client should be loaded. The
     * application currently has two modes: GUI and client. The GUI mode is the
     * default option.
     * 
     * @param args the command line arguments given
     */
    public static void main(String[] args) {
        ClientFactory factory = new ClientFactory();

        ClientOptions options = new ClientOptions();
        if (options.parseOptions(args)) {
            ClientType client = factory.createClient(options);
            client.run(options);
        } else {
            System.out.println(usage());
        }
    }
}
