package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.*;
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

    static final String[] todo = {"ADD", "DEL", "LIST"};

    public static final char DO_ADD = 'A';
    public static final char DO_DEL = 'D';
    public static final char DO_LIST = 'L';

    Context c;

    char doit;
    int action_id;
    DSpaceObject who;

    Boolean verbose;
    String property;
    String propertyExists;
    String propertyBefore;

    Policies(PolicyArguments args) throws SQLException {
        c = args.getContext();
        doit = args.doit;
        action_id = args.action_id;
        who = args.whoObj;
        verbose = args.getVerbose();

        propertyExists = "policy." + Constants.actionText[action_id];
        if (doit != DO_LIST) {
            property = propertyExists + "." + doit;
            propertyBefore = propertyExists + "." + "before";
        }

        switch (doit) {
            case DO_DEL:
            case DO_ADD:
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
            System.out.println("# " + doit + " policy." + Constants.actionText[action_id] +
                    " for " + targets.length + " DSPaceObjects");
            switch (doit) {
                case DO_ADD:
                case DO_DEL:
                    changePolicy(p, targets);
                    break;
                case DO_LIST:
                    doList(p, targets);
                    break;
                default:
                    log.error("DO WHAT ?");
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
        List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(c, target.getObject(), action_id);
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

                if (doit == DO_ADD) {
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
                            AuthorizeManager.addPolicy(c, targets[i].getObject(), action_id, (EPerson) who);
                        } else {
                            AuthorizeManager.addPolicy(c, targets[i].getObject(), action_id, (Group) who);
                        }
                        map.put(property, who);
                    }
                } else if (doit == DO_DEL) {
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

    public static String DO = "d";
    public static String DO_LONG = "do";

    public static String ACTION = "a";
    public static String ACTION_LONG = "action";

    public static String WHO = "w";
    public static String WHO_LONG = "who";

    String doitStr = "LIST";
    char doit;

    String action = Constants.actionText[Constants.READ];
    int action_id;

    String who = "GROUP." + Group.ANONYMOUS_ID;
    DSpaceObject whoObj; // EPerson or Group;

    PolicyArguments() {
        super();
        String available = deepToString(Constants.actionText);
        options.addOption(ACTION, ACTION_LONG, true, "one of " + available);
        available = StringUtils.join(Policies.todo, ", ");
        options.addOption(DO, DO_LONG, true, "one of " + available);
        options.addOption(WHO, WHO_LONG, true, "group/eperson (ignored if doing LIST)");
    }

    @Override
    public Boolean parseArgs(String[] argv) throws ParseException, SQLException {
        if (super.parseArgs(argv)) {

            if (line.hasOption(DO)) {
                doitStr = line.getOptionValue(DO).toUpperCase();
            }
            doit = doitStr.charAt(0);
            if (doit != 'L') {
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
            if (line.hasOption(ACTION)) {
                action = line.getOptionValue(ACTION);
                action_id = Constants.getActionID(action);
                if (action_id < 0) {
                    throw new ParseException(action + " is not a valid action");
                }
            }

            return true;
        }
        return false;
    }

}