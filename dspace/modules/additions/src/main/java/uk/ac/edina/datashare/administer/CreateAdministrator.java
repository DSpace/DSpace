package uk.ac.edina.datashare.administer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.core.Context;

import uk.ac.edina.datashare.db.DbUpdate;

/**
 * DataShare specific CreateAdministrator.
 */
public class CreateAdministrator
{
    /**
     * For invoking via the command line
     * 
     * @param argv
     *            command-line arguments
     */
    public static void main(String[] argv)
    {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        
        // get manditory args -e and -u
        options.addOption("u", "uun", true, "University User Name");
        options.addOption("e", "email", true, "Email address");
        
        try
        {
            CommandLine line = parser.parse(options, argv);
            
            if(line.hasOption('u') && line.hasOption('e'))
            {
                // got values - update database with UUN/email values
                Context context = new Context();
                DbUpdate.insertUunEntry(
                        context,
                        line.getOptionValue('u'),
                        line.getOptionValue('e'));
                
                context.complete();
                
                System.exit(0);
            }
            else
            {
                // manditory fields not found
                System.out.println("Syntax: -u <UUN> -e <email>");
                System.exit(1);
            }
        }
        catch(Exception ex)
        {
            System.err.println("Problem parsing command args " +
                    ex.getMessage());
        }
    }
}
