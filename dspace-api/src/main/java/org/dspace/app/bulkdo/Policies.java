package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dspace.app.util.Util;
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
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.deepToString;

/**
 * Created by monikam on 4/2/14.
 */
public class Policies {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(Policies.class);

    Context c;

    char action;
    int dspaceActionId;
    DSpaceObject who;

    Boolean verbose;
    String property;
    String propertyExists;
    String propertyBefore;

    Policies(PolicyArguments args) throws SQLException {
        c = args.getContext();
        dspaceActionId = args.dspaceActionid;
        who = args.whoObj;
        verbose = args.getVerbose();

        action = args.getAction();

        propertyExists = "policy." + Constants.actionText[dspaceActionId];
        if (args.getAction() != Arguments.DO_LIST) {
            property = propertyExists + "." + action;
            propertyBefore = propertyExists + "." + "before";
        }

        switch (action) {
            case Arguments.DO_DEL:
            case Arguments.DO_ADD:
                if (who == null || (who.getType() != Constants.EPERSON && who.getType() != Constants.GROUP)) {
                    log.error(Util.toString(who, "null") + " is not a group or eperson");
                }
                break;
        }
    }

    void apply(Printer p, ActionTarget[] targets) throws SQLException {
        if (targets == null || targets.length == 0) {
            log.info("Empty target/entity list");
        } else {
            System.out.println("# " + action + " policy." + Constants.actionText[dspaceActionId] +
                    " for " + targets.length + " DSPaceObjects");
            switch (action) {
                case Arguments.DO_ADD:
                case Arguments.DO_DEL:
                    changePolicy(p, targets);
                    break;
                case Arguments.DO_LIST:
                    doList(p, targets);
                    break;
                default:
                    log.error("DO WHICH ACTION ?");
            }
        }
    }

    public void doList(Printer p, ActionTarget[] targets) throws SQLException {
        p.addKey(propertyExists);
        for (int i = 0; i < targets.length; i++) {
            addPolicyInfo(targets[i], propertyExists);
            p.println(targets[i]);
        }
        // TODO  p.delKey(propertyExists);
    }

    private void addPolicyInfo(ActionTarget target, String prop) throws SQLException {
        List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(c, target.getObject(), dspaceActionId);
        HashMap<String, Object> map = target.toHashMap();
        map.put(prop, policies.toArray());
    }


    private void changePolicy(Printer p, ActionTarget[] targets) throws SQLException {
        try {
            p.addKey(propertyExists);
            if (verbose) {
                p.addKey(propertyBefore);
                p.addKey(property);
            }
            for (int i = 0; i < targets.length; i++) {
                HashMap map = targets[i].toHashMap();

                addPolicyInfo(targets[i],propertyBefore);
                Object[] pols = (Object[]) map.get(propertyBefore);

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
                            AuthorizeManager.addPolicy(c, targets[i].getObject(), dspaceActionId, (EPerson) who);
                        } else {
                            AuthorizeManager.addPolicy(c, targets[i].getObject(), dspaceActionId, (Group) who);
                        }
                        map.put(property, who);
                    }
                } else if (action == Arguments.DO_DEL) {
                    for (int pi = 0; pi < pols.length; pi++) {
                        ResourcePolicy pol = (ResourcePolicy) pols[pi];
                        if (pol.getGroup() == who || pol.getEPerson() == who) {
                            // found a matching policy ==> delete it
                            pol.delete();
                            map.put(property, who);
                            // keep going there may be duplicate policies
                        }
                    }
                } else {
                    map.put(property + "???should-never-happen!!!", who);
                }
                addPolicyInfo(targets[i],propertyExists);
                p.println(targets[i]);
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

    public static String DSPACE_ACTION = "d";
    public static String DSPACE_ACTION_LONG = "dspace_action";

    public static String WHO = "w";
    public static String WHO_LONG = "who";

    String dspaceAction  = Constants.actionText[Constants.READ];
    int dspaceActionid;

    String who = "GROUP." + Group.ANONYMOUS_ID;
    DSpaceObject whoObj; // EPerson or Group;

    PolicyArguments() {
        super(new char[]{Arguments.DO_LIST, Arguments.DO_ADD, Arguments.DO_DEL});
        String available = deepToString(Constants.actionText);
        options.addOption(DSPACE_ACTION, DSPACE_ACTION_LONG, true, "one of " + available);
        options.addOption(WHO, WHO_LONG, true, "group/eperson (ignored if doing LIST)");
    }

    @Override
    public Boolean parseArgs(String[] argv) throws ParseException, SQLException {
        if (super.parseArgs(argv)) {
            if (getAction() != Arguments.DO_LIST) {
                if (line.hasOption(WHO)) {
                    who = line.getOptionValue(WHO);
                    whoObj = DSpaceObject.fromString(getContext(), who);
                    System.out.println((whoObj == null) ? "null" : whoObj);
                    if (whoObj == null || (whoObj.getType() != Constants.GROUP && whoObj.getType() != Constants.EPERSON)) {
                        throw new ParseException(who + " is not a known Group or EPerson");
                    }
                } else {
                    throw new ParseException("Missing " + WHO_LONG + " option");
                }
            }
            if (line.hasOption(DSPACE_ACTION)) {
                dspaceAction = line.getOptionValue(DSPACE_ACTION);
                dspaceActionid = Constants.getActionID(dspaceAction);
                if (dspaceActionid < 0) {
                    throw new ParseException(dspaceAction + " is not a valid action");
                }
            }

            return true;
        }
        return false;
    }

}