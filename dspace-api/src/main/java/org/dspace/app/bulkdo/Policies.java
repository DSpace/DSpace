/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
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

    PolicyArguments args;

    String property;
    String propertyExists;
    String propertyBefore;

    char action;
    int dspaceActionId;

    ResourcePolicyService policyService;

    Policies(PolicyArguments arguments) throws SQLException {
        args = arguments;
        action = args.getAction();
        dspaceActionId = args.dspaceActionid;

        propertyExists = ActionTarget.POLICY + "."  + Constants.actionText[dspaceActionId];
        property = propertyExists + "." + args.getActionString();
        propertyBefore = propertyExists + "." + "before";
        policyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
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

        List<ResourcePolicy> policies = policyService.find(args.getContext(), target.getObject(), dspaceActionId);
        target.put(prop, policies.toArray());
    }

    private void changePolicy(Printer p, ArrayList<ActionTarget> targets) throws SQLException {
        try {
            p.addKey(propertyExists);
            if (args.getVerbose()) {
                p.addKey(propertyBefore);
                p.addKey(property);
            }
            DSpaceObject who = args.whoObj;
            Context c = args.getContext();
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
                        ResourcePolicy rp = policyService.create(c);
                        rp.setdSpaceObject(targets.get(i).getObject());
                        rp.setAction(dspaceActionId);
                        if (who.getType() == Constants.EPERSON) {
                            rp.setEPerson((EPerson) who);
                        } else {
                            rp.setGroup((Group) who);
                        }
                        target.put(property, who);
                    }
                } else if (action == Arguments.DO_DEL) {
                    for (int pi = 0; pi < pols.length; pi++) {
                        ResourcePolicy pol = (ResourcePolicy) pols[pi];
                        if (pol.getGroup() == who || pol.getEPerson() == who) {
                            // found a matching policy ==> delete it
                            if (who.getType() == Constants.EPERSON) {
                                policyService.removeDsoEPersonPolicies(args.getContext(), pol.getdSpaceObject(), (EPerson) who);
                            } else {
                                policyService.removeDsoGroupPolicies(args.getContext(), pol.getdSpaceObject(), (Group) who);
                            }
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
            if (!args.getDryRun())
               args.getContext().complete();
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

    DSpaceObject whoObj;
    int dspaceActionid;

    PolicyArguments() {
        super(Policies.class.getCanonicalName(), new char[]{Arguments.DO_ADD, Arguments.DO_DEL});
        options.addOption(DSPACE_ACTION, DSPACE_ACTION_LONG, true, "one of " + deepToString(Constants.actionText) + " default is " + Constants.actionText[Constants.READ]);
        options.addOption(WHO, WHO_LONG, true, "group/eperson used in policies, give as GROUP.<name>, EPERSON.<netid>, or EPERSON.<email>");
    }

    @Override
    public Boolean parseArgs(String[] argv) throws ParseException {
        try {
            if (super.parseArgs(argv)) {
                if (!line.hasOption(ACTION)) {
                    throw new ParseException("Missing " + ACTION_LONG + " option");
                }
                String who = "UNKNOWN";
                if (!line.hasOption(WHO)) {
                    throw new ParseException("Missing " + WHO_LONG + " option");
                } else {
                    who = line.getOptionValue(WHO);
                }
                whoObj = DSpaceObject.fromString(getContext(), who);
                if (whoObj == null || (whoObj.getType() != Constants.GROUP && whoObj.getType() != Constants.EPERSON)) {
                    throw new ParseException(who + " is not a known Group or EPerson");
                }
                if (whoObj == null) {
                    throw new ParseException("Missing " + WHO_LONG + " option");
                }

                String dspaceAction = Constants.actionText[Constants.READ];
                if (line.hasOption(DSPACE_ACTION)) {
                    dspaceAction = line.getOptionValue(DSPACE_ACTION);
                }
                dspaceActionid = Constants.getActionID(dspaceAction);
                if (dspaceActionid < 0) {
                    throw new ParseException(dspaceAction + " is not a valid action");
                }

                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new ParseException("Configuration Error: " + e.getMessage());
        }
    }

    @Override
    public void printArgs(PrintStream out, String prefix) {
        super.printArgs(out, prefix);
        out.println(prefix + " " + DSPACE_ACTION_LONG + "=" + Constants.actionText[dspaceActionid]);
        out.println(prefix + " " + WHO_LONG + "=" + whoObj);
        out.println(prefix + " ");
    }

    @Override
    public void shortDescription() {
        System.out.println("ADD policy to or DELete policy from all dspaceObjects of given type contained in root");
    }

}
