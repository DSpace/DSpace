/*
 */
package org.dspace.workflow;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AutoCurateToBlackoutProcessor extends AutoWorkflowProcessor {
    private static final Logger log = Logger.getLogger(AutoCurateToBlackoutProcessor.class);

    public static void main(String args[]) throws ParseException {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

	options.addOption("i", "wfitemid", true, "workflowitem id for an unclaimed item in "
                + "'dryadAcceptEditReject'.\n"
                + "Item will be approved and sent to publication blackout");
        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, args);
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("AutoCurateToBlackoutProcessor\n", options);
        }

        if(line.hasOption('i')) {
	    // get a WorkflowItem using a workflow ID
	    Integer wfItemId = Integer.parseInt(line.getOptionValue('i'));
            Context c = null;
            int result = 0;
            try {
                c = new Context();
                AutoCurateToBlackoutProcessor processor = new AutoCurateToBlackoutProcessor(c);
                if(processor.processWorkflowItem(wfItemId)) {
                    System.out.println("Successfully curated workflowitem " + wfItemId + " into blackout");
                } else {
                    System.out.println("Did not approve " + wfItemId + ", check logs for details");
                }
            } catch (AutoWorkflowProcessorException ex) {
                System.err.println("Exception approving item: " + ex);
                result = 1;
            } catch (ItemIsNotEligibleForStepException ex) {
                System.err.println("Item is not in step: " + ex);
                result = 1;
            } catch (SQLException ex) {
                System.err.println("Exception curating item: " + ex);
                result = 1;
            } finally {
                if(c != null) {
                    try {
                        c.complete();
                    } catch (SQLException ex) {

                    }
                }
            }
            System.exit(result);
	} else {
            System.out.println("No workflow ID was given. This must be provided to identify the item in the workflow");
            System.exit(1);
        }
    }

    public AutoCurateToBlackoutProcessor(Context c) {
        super(c);
    }

    @Override
    Boolean canProcessClaimedTask() throws SQLException {
        // We can process any claimed tasks, we're just approving them to blackout
        return Boolean.TRUE;
    }

    @Override
    String getActionID() {
        // returns the action ID we can process
        return "dryadAcceptEditRejectAction";
    }

    @Override
    HttpServletRequest getRequest() {
        return new DummyCurateToBlackoutRequest();
    }

    @Override
    Boolean isMyStep(final String stepId) throws SQLException {
        return (stepId.equals("dryadAcceptEditReject"));
    }

}
