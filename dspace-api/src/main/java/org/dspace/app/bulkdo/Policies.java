package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by monikam on 4/2/14.
 */
public class Policies {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(Policies.class);

    Context c;

    DSpaceObject who;

    Boolean verbose;
    String property;
    String propertyExists;
    String propertyBefore;

    char action;
    int dspaceActionId;

    Policies(PolicyArguments args) throws SQLException {
        c = args.getContext();
        who = args.whoObj;
        verbose = args.getVerbose();

        action = args.getAction();
        dspaceActionId = args.getDSpaceAction();

        propertyExists = ActionTarget.POLICY + "."  + Constants.actionText[dspaceActionId];
        property = propertyExists + "." + args.getActionString();
        propertyBefore = propertyExists + "." + "before";
    }

    void apply(Printer p, ArrayList<ActionTarget> targets) throws SQLException {
        if (targets == null || targets.size() == 0) {
            log.info("Empty target/entity list");
        } else {
            System.out.println("# " + action + " policy." + Constants.actionText[dspaceActionId] +
                    " for " + targets.size() + " DSPaceObjects");
            switch (action) {
                case Arguments.DO_ADD:
                case Arguments.DO_DEL:
                    changePolicy(p, targets);
                    break;
                default:
                    log.error("DO WHICH ACTION ?");
            }
        }
    }

    private void addPolicyInfo(ActionTarget target, String prop) throws SQLException {
        List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(c, target.getObject(), dspaceActionId);
        target.put(prop, policies.toArray());
    }

    private void changePolicy(Printer p, ArrayList<ActionTarget> targets) throws SQLException {
        try {
            p.addKey(propertyExists);
            if (verbose) {
                p.addKey(propertyBefore);
                p.addKey(property);
            }
            for (int i = 0; i < targets.size(); i++) {
                ActionTarget target = targets.get(i);

                addPolicyInfo(targets.get(i),propertyBefore);
                Object[] pols = (Object[]) target.get(propertyBefore);

                if (action == Arguments.DO_ADD) {
                    // see whether there is already a policy in place
                    int pi = 0;
                    for (; pi < pols.length; pi++) {
                        ResourcePolicy pol = (ResourcePolicy) pols[pi];
                        if (pol.getGroup() == who || pol.getEPerson() == who) {
                            // already have a policy in place - don't add again
                            break;
                        }
                    }
                    if (pi == pols.length) {
                        // policy does not yet exist
                        if (who.getType() == Constants.EPERSON) {
                            AuthorizeManager.addPolicy(c, targets.get(i).getObject(), dspaceActionId, (EPerson) who);
                        } else {
                            AuthorizeManager.addPolicy(c, targets.get(i).getObject(), dspaceActionId, (Group) who);
                        }
                        target.put(property, who);
                    }
                } else if (action == Arguments.DO_DEL) {
                    for (int pi = 0; pi < pols.length; pi++) {
                        ResourcePolicy pol = (ResourcePolicy) pols[pi];
                        if (pol.getGroup() == who || pol.getEPerson() == who) {
                            // found a matching policy ==> delete it
                            pol.delete();
                            target.put(property, who);
                            // keep going there may be duplicate policies
                        }
                    }
                } else {
                    target.put(property + "???should-never-happen!!!", who);
                }
                addPolicyInfo(targets.get(i),propertyExists);
                p.println(targets.get(i));
            }
            c.complete();
        } catch (AuthorizeException e) {
            e.printStackTrace();
        } finally {
            // TODO p.delKey(propertyExists);
            // TODO p.delKey(propertyAfter);
            // TODO p.delKey(property);
        }
    }


    public static void main(String argv[]) {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%c: %m%n"));
        log.addAppender(ca);

        PolicyArguments args = new PolicyArguments();
        try {
            if (args.parseArgs(argv)) {
                Policies pols = new Policies(args);
                Lister lister = new Lister(args.getContext(), args.getRoot(), args.getType());
                Printer p = args.getPrinter();
                pols.apply(p, lister.getTargets(args.getType(), false));
            }
        } catch (SQLException se) {
            System.err.println("ERROR: " + se.getMessage() + "\n");
            System.exit(1);
        } catch (ParseException pe) {
            System.err.println("ERROR: " + pe.getMessage() + "\n");
            args.usage();
            System.exit(1);
        }
    }


}

class PolicyArguments extends Arguments {
    PolicyArguments() {
        super(new char[]{Arguments.DO_ADD, Arguments.DO_DEL});
    }

    @Override
    public Boolean parseArgs(String[] argv) throws ParseException, SQLException {
        if (super.parseArgs(argv)) {
            if (whoObj == null) {
                    throw new ParseException("Missing " + WHO_LONG + " option");
            }
            return true;
        }
        return false;
    }

}