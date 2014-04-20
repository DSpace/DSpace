package org.dspace.app.bulkdo;

import oracle.jdbc.driver.Const;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.xpath.Arg;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.awt.*;
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

    public static void main(String argv[]) {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%c: %m%n"));
        log.addAppender(ca);

        PolicyArguments args = new PolicyArguments();
        try {
            if (args.parseArgs(argv)) {
                Lister lister = new Lister(args.getContext(), args.getRoot(), args.getType());
                ActionTarget[] targets = lister.getTargets(args.getType());
                if (targets == null || targets.length == 0) {
                    log.info("empty entity array");
                    return;
                }
                Printer p = args.getPrinter();
                System.out.println("# " + args.doitStr + " policy." + Constants.actionText[args.action_id] +
                        " for " + targets.length + " " + Constants.typeText[args.getType()] + "s");
                if (args.doit == 'L') {
                    listPolicyInfo(p, args.getContext(), targets, args.action_id);
                } else {
                    System.out.println("# who " + args.whoObj);
                    changePolicy(args.doitStr, p, args.getContext(), targets, args.action_id, args.whoObj);
                }
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

    public static void changePolicy(String changeHow, Printer p, Context c, ActionTarget[] targets, int action_id, DSpaceObject who) throws SQLException {
        if (who == null || (who.getType() != Constants.EPERSON && who.getType() != Constants.GROUP)) {
            log.error(Util.toString(who, "null") + " is not a group or eperson");
            return;
        }
        if (targets == null || targets.length == 0) {
            log.debug("changePolicy: empty target list");
        } else {
            char mode = changeHow.charAt(0);
            String property = "policy." + Constants.actionText[action_id] + "." + changeHow;
            try {
                for (int i = 0; i < targets.length; i++) {
                    HashMap map = targets[i].toHashMap();
                    switch (mode) {
                        case 'A':
                            if (who.getType() == Constants.EPERSON) {
                                log.debug("ADD " + who + " " + who.getID());
                                AuthorizeManager.addPolicy(c, targets[i].getObject(), action_id, (EPerson) who);
                            } else {
                                log.debug("ADD " + who + " " + who.getID());
                                AuthorizeManager.addPolicy(c, targets[i].getObject(), action_id, (Group) who);
                            }
                            map.put(property, who);
                            break;
                        case 'D':
                            map.put(property, who);
                            break;
                        default:
                            map.put(property + "???", who);
                    }
                    p.println(targets[i]);
                }
                c.complete();
            } catch (AuthorizeException e) {
                e.printStackTrace();
            }
        }
    }

    public static void listPolicyInfo(Printer printer, Context c, ActionTarget[] targets, int action_id) throws SQLException {
        if (targets != null) {
            for (int i = 0; i < targets.length; i++) {
                List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(c, targets[i].getObject(), action_id);
                String[] whoCan = new String[policies.size()];
                for (int p = 0; p < policies.size(); p++) {
                    ResourcePolicy pol = policies.get(p);
                    Group g = pol.getGroup();
                    if (g != null) {
                        whoCan[p] = g.toString();
                    } else {
                        EPerson e = pol.getEPerson();
                        if (e != null) {
                            whoCan[p] = e.toString();
                        } else {
                            whoCan[p] = "SICK-POLICY." + pol.getID();
                        }
                    }
                }
                HashMap<String, Object> map = targets[i].toHashMap();
                map.put("policy." + Constants.actionText[action_id], whoCan);
                printer.println(targets[i]);
            }
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

    private String[] todo = {"ADD", "DEL", "LIST"};

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
        available = StringUtils.join(todo, ", ");
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
            String property = "policy." + Constants.actionText[action_id];
            if (doit != 'L') {
                property += "." + doitStr;
            }
            addKey(property);
            return true;
        }
        return false;
    }

}