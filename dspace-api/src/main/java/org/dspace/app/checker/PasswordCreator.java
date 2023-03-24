/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.checker;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dspace.eperson.*;
import org.dspace.core.Utils;

/**
 * Command line access to the checksum checker. Options are listed in the 
 * documentation for the main method.</p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 */
public final class PasswordCreator
{

    /**
     * Blanked off constructor, this class should be used as a command line
     * tool.
     * 
     */
    private PasswordCreator()
    {
    }

    /**
     * Command line access to the checksum package.
     * 
     * @param args
     *            <dl>
     *            <dt>-h</dt>
     *            <dd>Print help on command line options</dd>
     *            <dt>-l</dt>
     *            <dd>loop through bitstreams once</dd>
     *            <dt>-L</dt>
     *            <dd>loop continuously through bitstreams</dd>
     *            <dt>-d</dt>
     *            <dd>specify duration of process run</dd>
     *            <dt>-b</dt>
     *            <dd>specify bitstream IDs</dd>
     *            <dt>-a [handle_id]</dt>
     *            <dd>check anything by handle</dd>
     *            <dt>-e</dt>
     *            <dd>Report only errors in the logs</dd>
     *            <dt>-p</dt>
     *            <dd>Don't prune results before running checker</dd>
     *            </dl>
     */
    public static void main(String[] args)
    {
        // set up command line parser
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;

        // create an options object and populate it
        Options options = new Options();

        options.addOption("p", "password", true, "Specify the password");

        try
        {
            line = parser.parse(options, args);
        }
        catch (ParseException e)
        {
            System.exit(1);
        }

        String s = line.getOptionValue('p');

        //toHex returns a string.

        PasswordHash hash = new PasswordHash(s);

        String password = Utils.toHex(hash.getHash());

        String salt = Utils.toHex(hash.getSalt());
        String algo = hash.getAlgorithm();

        System.out.println("password requested for " + s + " got " + password);
        System.out.println("salt " + salt);
        System.out.println("algo " + algo);
        System.exit(0);

    }

}
