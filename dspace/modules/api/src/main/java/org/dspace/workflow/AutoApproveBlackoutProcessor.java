/*
 */
package org.dspace.workflow;

import java.sql.SQLException;
import java.util.Date;
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
public class AutoApproveBlackoutProcessor extends AutoWorkflowProcessor {
    private static final Logger log = Logger.getLogger(AutoApproveBlackoutProcessor.class);

    public static void main(String args[]) throws ParseException {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

	options.addOption("i", "wfitemid", true, "workflowitem id for an unclaimed item in "
                + "'pendingPublicationStep' or 'pendingPublicationReauthorizationPaymentStep'.\n"
                + "Item must have a dc.date.blackoutUntil metadata value in the past.");
        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, args);
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("AutoApproveBlackoutProcessor\n", options);
        }

        if(line.hasOption('i')) {
	    // get a WorkflowItem using a workflow ID
	    Integer wfItemId = Integer.parseInt(line.getOptionValue('i'));
            Context c = null;
            int result = 0;
            try {
                c = new Context();
                AutoApproveBlackoutProcessor processor = new AutoApproveBlackoutProcessor(c);
                if(processor.processWorkflowItem(wfItemId)) {
                    System.out.println("Successfully approved workflowitem " + wfItemId + " from blackout");
                } else {
                    System.out.println("Did not lift blackout on " + wfItemId + ", check logs for details");
                }
            } catch (AutoWorkflowProcessorException ex) {
                System.err.println("Exception approving blackout item: " + ex);
                result = 1;
            } catch (ItemIsNotEligibleForStepException ex) {
                System.err.println("Item is not in blackout: " + ex);
                result = 1;
            } catch (SQLException ex) {
                System.err.println("Exception approving blackout item: " + ex);
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

    public AutoApproveBlackoutProcessor(Context c) {
        super(c);
    }

    @Override
    Boolean isMyStep(final String stepId) throws SQLException {
        return (stepId.equals("pendingPublicationStep") ||
                stepId.equals("pendingPublicationReAuthorizationPaymentStep"));
    }

    @Override
    Boolean canProcessClaimedTask() throws SQLException {
        // Verify date is in the past
        Date now = new Date();
        Date blackoutUntilDate = getDataPackage().getBlackoutUntilDate();
        if(blackoutUntilDate == null) {
            log.error("Attempted to lift blackout on item: " + getDataPackage().getItem().getID() + " but no blackoutUntilDate present");
            return Boolean.FALSE;
        }

        if(now.before(blackoutUntilDate)) {
            // current date is before the blackout until date
            log.error("Attempted to lift blackout early on item " + getWfi().getItem().getID() +
                    ". Current date: " + now + " blackoutUntilDate: " + blackoutUntilDate);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    @Override
    String getActionID() {
        // returns the action ID we can process
        return "afterPublicationAction";
    }

    @Override
    HttpServletRequest getRequest() {
        return new DummyApproveFromBlackoutRequest();
    }

}
