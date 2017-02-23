/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.launcher;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Document;

/**
 *
 * @author mwood
 */
public class CommandRunner
{
    /**
     * 
     * @param args the command line arguments given
     * @throws IOException if IO error
     * @throws FileNotFoundException if file doesn't exist
     */
    public static void main(String[] args)
            throws FileNotFoundException, IOException
    {
        if (args.length > 0)
        {
            runManyCommands(args[0]);
        }
        else
        {
            runManyCommands("-");
        }
        // There is no sensible way to use the status returned by runManyCommands().
        // If called from the command line then we would want to return it
        // through System.exit().  But if called (normally) from ScriptLauncher,
        // there is no way to return it and we don't want to interrupt
        // ScriptLauncher.
        //
        // "'tis a puzzlement." -- the King of Siam
    }

    /**
     * Read a file of command lines and execute each in turn.
     *
     * @param script the file of command lines to be executed.
     * @return status code
     * @throws IOException if IO error
     * @throws FileNotFoundException if file doesn't exist
     */
    static int runManyCommands(String script)
            throws FileNotFoundException, IOException
    {
        Reader input;
        if ("-".equals(script))
        {
            input = new InputStreamReader(System.in);
        }
        else
        {
            input = new FileReader(script);
        }

        StreamTokenizer tokenizer = new StreamTokenizer(input);

        tokenizer.eolIsSignificant(true);

        tokenizer.ordinaryChar('-');
        tokenizer.wordChars('-', '-');

        tokenizer.ordinaryChars('0', '9');
        tokenizer.wordChars('0', '9');

        tokenizer.ordinaryChar('.');
        tokenizer.wordChars('.', '.');

        tokenizer.ordinaryChar('@');
        tokenizer.wordChars('@', '@');

        int status = 0;
        List<String> tokens = new ArrayList<String>();
        Document commandConfigs = ScriptLauncher.getConfig();
        while (StreamTokenizer.TT_EOF != tokenizer.nextToken())
        {
            if (StreamTokenizer.TT_EOL == tokenizer.ttype)
            {
                if (tokens.size() > 0)
                {
                    status = ScriptLauncher.runOneCommand(commandConfigs, tokens.toArray(new String[tokens.size()]));
                    if (status > 0)
                    {
                        break;
                    }
                    tokens.clear();
                }
            }
            else
            {
                tokens.add(tokenizer.sval);
            }
        }

        return status;
    }
}
