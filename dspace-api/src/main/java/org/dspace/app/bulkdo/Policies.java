package org.dspace.app.bulkdo;

import oracle.jdbc.driver.Const;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.xpath.Arg;
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

                Printer p = args.getPrinter();
                if (args.doit == 'L') {
                    findPolicyInfo(args.getContext(), targets, args.action_id, args.whoObj);
                    System.out.println("# " + targets.length + " type=" + args.getType());
                    for (int i = 0; i < targets.length; i++)
                        p.println(targets[i]);
                } else if (args.doit == 'A') {
                    // add policy where it is not set
                } else if (args.doit == 'D') {
                    // del policy where it is  set
                } else {
                    assert (false);  // should never get here
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

    public static void findPolicyInfo(Context c, ActionTarget[] targets, int action_id, DSpaceObject who) throws SQLException {
        assert (who != null);
        assert (who.getType() == Constants.EPERSON || who.getType() == Constants.GROUP);
        log.info("findPolicyInfo: " + Constants.actionText[action_id]);
        if (targets == null || targets.length == 0) {
            log.info("findPolicyInfo:  empty entity array");
        } else {
            for (int i = 0; i < targets.length; i++) {
                assert (targets[i] != null);
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
            if (doit != 'L' && line.hasOption(WHO)) {
                who = line.getOptionValue(WHO);
                whoObj = DSpaceObject.fromString(getContext(), who);
                if (whoObj == null || (whoObj.getType() != Constants.GROUP && whoObj.getType() != Constants.EPERSON)) {
                    throw new ParseException(who + " is not a known Group or EPerson");
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

    @Override
    protected void optionExplainKeys() {
        super.optionExplainKeys();
        System.out.println("\tDepending on " + ACTION_LONG + " option all types have an additional key:  policy.<ACTION>");
    }

}