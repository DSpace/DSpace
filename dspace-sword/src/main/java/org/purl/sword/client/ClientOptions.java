/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * List of options that are parsed from the command line.
 *
 * @author Neil Taylor
 */
public class ClientOptions {
    /**
     * Label for the service operation.
     */
    public static final String TYPE_SERVICE = "service";

    /**
     * Label for the post operation.
     */
    public static final String TYPE_POST = "post";

    /**
     * Label for the multipost operation.
     */
    public static final String TYPE_MULTI_POST = "multipost";

    /**
     * The access type.
     */
    private String accessType = null;

    /**
     * Proxy host name.
     */
    private String proxyHost = null;

    /**
     * Proxy host port.
     */
    private int proxyPort = 8080;

    /**
     * Username to access the service/post server.
     */
    private String username = null;

    /**
     * Password to access the service/post server.
     */
    private String password = null;

    /**
     * HREF of the server to access.
     */
    private String href = null;

    /**
     * Filename to post.
     */
    private String filename = null;

    /**
     * File type.
     */
    private String filetype = null;

    /**
     * Specifies that the output streams are not to be captured by the GUI client.
     */
    private boolean noCapture = false;


    /**
     * SLUG Header field.
     */
    private String slug = null;

    /**
     * NoOp, used to indicate an operation on the server that does not
     * require the file to be stored.
     */
    private boolean noOp = false;

    /**
     * Request verbose output from the server.
     */
    private boolean verbose = false;

    /**
     * OnBehalfOf user id.
     */
    private String onBehalfOf = null;

    /**
     * Format namespace to be used for the posted file.
     */
    private String formatNamespace = null;

    /**
     * Introduce a checksum error. This is used to simulate an error with the
     * MD5 value.
     */
    private boolean checksumError = false;

    /**
     * Logger.
     */
    private static final Logger log = LogManager.getLogger();

    /**
     * List of multiple destination items. Used if the mode is set to multipost.
     */
    private final List<PostDestination> multiPost = new ArrayList<>();

    /**
     * Pattern string to extract the data from a destination parameter in multipost mode.
     */
    private static final Pattern MULTI_PATTERN
            = Pattern.compile("(.*?)(\\[(.*?)\\]) {0,1}(:(.*)) {0,1}@(http://.*)");

    /**
     * Flag that indicates if the GUI mode has been set. This is
     * true by default.
     */
    private boolean guiMode = true;

    /**
     * Flat that indicates if the MD5 option has been selected. This
     * is true by default.
     */
    private boolean md5 = false;

    /**
     * Parse the list of options contained in the specified array.
     *
     * @param args The array of options.
     * @return True if the options were parsed successfully.
     */
    public boolean parseOptions(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder().longOpt("md5").build())
                .addOption(Option.builder().longOpt("noOp").build())
                .addOption(Option.builder().longOpt("verbose").build())
                .addOption(Option.builder().longOpt("cmd").build())
                .addOption(Option.builder().longOpt("gui").build())
                .addOption(Option.builder().longOpt("help").build())
                .addOption(Option.builder().longOpt("nocapture").build());

        Option option;

        option = Option.builder().longOpt("host").hasArg().build();
        options.addOption(option);

        option = Option.builder().longOpt("port").hasArg().build();
        options.addOption(option);

        option = Option.builder("u").hasArg().build();
        options.addOption(option);

        option = Option.builder("p").hasArg().build();
        options.addOption(option);

        option = Option.builder().longOpt("href").hasArg().build();
        options.addOption(option);

        option = Option.builder("t").hasArg().build();
        options.addOption(option);

        option = Option.builder().longOpt("file").hasArg().build();
        options.addOption(option);

        option = Option.builder().longOpt("filetype").hasArg().build();
        options.addOption(option);

        option = Option.builder().longOpt("slug").hasArg().build();
        options.addOption(option);

        option = Option.builder().longOpt("onBehalfOf").hasArg().build();
        options.addOption(option);

        option = Option.builder().longOpt("formatNamespace").hasArg().build();
        options.addOption(option);

        option = Option.builder().longOpt("checksumError").build();
        options.addOption(option);

        option = Option.builder().longOpt("dest").hasArg().build();
        options.addOption(option);

        DefaultParser parser = new DefaultParser();
        CommandLine command;
        try {
            command = parser.parse(options, args);
        } catch (ParseException ex) {
            log.error(ex.getMessage());
            return false;
        }

        if (command.hasOption("help")) {
            return false; // force the calling code to display the usage information.
        }
        md5 = command.hasOption("md5");
        noOp = command.hasOption("noOp");
        verbose = command.hasOption("verbose");
        if (command.hasOption("cmd")) {
            guiMode = false;
        }
        if (command.hasOption("gui")) {
            guiMode = true;
        }
        proxyHost = command.getOptionValue("host");
        if (command.hasOption("port")) {
            proxyPort = Integer.parseInt(command.getOptionValue("port"));
        }
        username = command.getOptionValue("u");
        password = command.getOptionValue("p");
        href = command.getOptionValue("href");
        accessType = command.getOptionValue("t");
        filename = command.getOptionValue("file");
        filetype = command.getOptionValue("filetype");
        slug = command.getOptionValue("slug");
        onBehalfOf = command.getOptionValue("onBehalfOf");
        formatNamespace = command.getOptionValue("formatNamespace");
        checksumError = command.hasOption("checksumError");
        noCapture = command.hasOption("nocapture");
        if (command.hasOption("dest")) {
            String dest = command.getOptionValue("dest");
            Matcher m = MULTI_PATTERN.matcher(dest);
            if (!m.matches()) {
                log.debug("Error with dest parameter. Ignoring value: {}", dest);
            } else {
                int numGroups = m.groupCount();
                for (int g = 0; g <= numGroups; g++) {
                    log.debug("Group ({}) is: {}", g, m.group(g));
                }

                String group_username = m.group(1);
                String group_onBehalfOf = m.group(3);
                String group_password = m.group(5);
                String group_url = m.group(6);
                PostDestination destination = new PostDestination(group_url,
                        group_username, group_password, group_onBehalfOf);

                multiPost.add(destination);
            }
        }

        try {
            // apply any settings
            if (href == null && "service".equals(accessType)) {
                log.error("No href specified.");
                return false;
            }

            if (multiPost.isEmpty() && "multipost".equals(accessType)) {
                log.error("No destinations specified");
                return false;
            }

            if (accessType == null && !guiMode) {
                log.error("No access type specified");
                return false;
            }

            if ((username == null && password != null) || (username != null && password == null)) {
                log.error(
                    "The username and/or password are not specified. If one is specified, the other must also be " +
                        "specified.");
                return false;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            log.error("Error with parameters.");
            return false;
        }

        return true;
    }

    /**
     * Get the access type.
     *
     * @return The value, or <code>null</code> if the value is not set.
     */
    public String getAccessType() {
        return accessType;
    }

    /**
     * Set the access type.
     *
     * @param accessType The value, or <code>null</code> to clear the value.
     */
    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    /**
     * Get the proxy host.
     *
     * @return The value, or <code>null</code> if the value is not set.
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Set the proxy host.
     *
     * @param proxyHost The value, or <code>null</code> to clear the value.
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * Get the proxy port.
     *
     * @return The proxy port. Default value is 80.
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Set the proxy port.
     *
     * @param proxyPort The proxy port.
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * Get the username.
     *
     * @return The value, or <code>null</code> if the value is not set.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the username.
     *
     * @param username The value, or <code>null</code> to clear the value.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get the password.
     *
     * @return The value, or <code>null</code> if the value is not set.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password.
     *
     * @param password The value, or <code>null</code> to clear the value.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get the HREF of the service to access.
     *
     * @return The value, or <code>null</code> if the value is not set.
     */
    public String getHref() {
        return href;
    }

    /**
     * Set the HREF of the service to access.
     *
     * @param href The value, or <code>null</code> to clear the value.
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * Get the name of the file to post.
     *
     * @return The value, or <code>null</code> if the value is not set.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Set the name of the file to post.
     *
     * @param filename The value, or <code>null</code> to clear the value.
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Get the type of the file to post.
     *
     * @return The filetype, or <code>null</code> if the value is not set.
     */
    public String getFiletype() {
        return filetype;
    }

    /**
     * Set the type of the file to post.
     *
     * @param filetype The value, or <code>null</code> to clear the value.
     */
    public void setFiletype(String filetype) {
        this.filetype = filetype;
    }

    /**
     * Determine if the tool is to be run in GUI mode.
     *
     * @return True if the tool is set for GUI mode.
     */
    public boolean isGuiMode() {
        return guiMode;
    }

    /**
     * Set the tool to run in GUI mode.
     *
     * @param guiMode True if the tool is to run in gui mode.
     */
    public void setGuiMode(boolean guiMode) {
        this.guiMode = guiMode;
    }

    /**
     * Get the MD5 setting. True if the tool is to use MD5 for post operations.
     *
     * @return The MD5 setting.
     */
    public boolean isMd5() {
        return md5;
    }

    /**
     * Set the MD5 setting.
     *
     * @param md5 True if the tool should use MD5 for post operations.
     */
    public void setMd5(boolean md5) {
        this.md5 = md5;
    }

    /**
     * Determine if the NoOp header should be sent.
     *
     * @return True if the header should be sent.
     */
    public boolean isNoOp() {
        return noOp;
    }

    /**
     * Set the NoOp setting.
     *
     * @param noOp True if the NoOp header should be used.
     */
    public void setNoOp(boolean noOp) {
        this.noOp = noOp;
    }

    /**
     * Determine if the verbose option is set.
     *
     * @return True if verbose option is set.
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Set the verbose option.
     *
     * @param verbose True if verbose should be set.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Get the onBehalfOf value.
     *
     * @return The value, or <code>null</code> to clear the value.
     */
    public String getOnBehalfOf() {
        return onBehalfOf;
    }

    /**
     * Set the onBehalf of Value.
     *
     * @param onBehalfOf The value, or <code>null</code> to clear the value.
     */
    public void setOnBehalfOf(String onBehalfOf) {
        this.onBehalfOf = onBehalfOf;
    }

    /**
     * Get the format namespace value.
     *
     * @return The value, or <code>null</code> if the value is not set.
     */
    public String getFormatNamespace() {
        return formatNamespace;
    }

    /**
     * Set the format namespace value.
     *
     * @param formatNamespace The value, or <code>null</code> to clear the value.
     */
    public void setFormatNamespace(String formatNamespace) {
        this.formatNamespace = formatNamespace;
    }

    /**
     * Get the checksum error value.
     *
     * @return True if an error should be introduced into the checksum.
     */
    public boolean getChecksumError() {
        return checksumError;
    }

    /**
     * Set the checksum error value.
     *
     * @param checksumError True if the error should be introduced.
     */
    public void setChecksumError(boolean checksumError) {
        this.checksumError = checksumError;
    }

    /**
     * Get the current slug header.
     *
     * @return The slug value, or <code>null</code> if the value is not set.
     */
    public String getSlug() {
        return this.slug;
    }

    /**
     * Set the text that is to be used for the slug header.
     *
     * @param slug The value, or <code>null</code> to clear the value.
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * Get the list of post destinations.
     *
     * @return An iterator over the list of PostDestination objects.
     */
    public Iterator<PostDestination> getMultiPost() {
        return multiPost.iterator();
    }


    /**
     * Determine if the noCapture option is set. This indicates that the code
     * should not attempt to redirect stdout and stderr to a different output
     * destination. Intended for use in a GUI client.
     *
     * @return The noCapture setting. True if set.
     */
    public boolean isNoCapture() {
        return noCapture;
    }
}
