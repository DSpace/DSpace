package edu.tamu.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.eperson.Group;

public class AuthorizeAnonymous extends AbstractCurationTask {

    private int result = Curator.CURATE_SUCCESS;
    private StringBuilder sb = new StringBuilder();

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        sb = new StringBuilder();
        super.init(curator, taskId);
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso.getType() == Constants.SITE) {
            sb.append("Cannot perform this task at site level.");
            this.setResult(sb.toString());
            return Curator.CURATE_FAIL;
        } else {
            distribute(dso);
            this.setResult(sb.toString());
            return result;
        }
    }

    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        Context context = Curator.curationContext();
        Item workingItem = Item.find(context, item.getID());

        try {
            removeReadPolicies(context, workingItem);

            // Generally, we want items themselves to be available for everyone to access, only restricting their actual content.
            AuthorizeManager.addPolicy(context, workingItem, Constants.READ, Group.findByName(context, "Anonymous"));

            for (Bundle bundle : workingItem.getBundles()) {
                for (Bitstream bs : bundle.getBitstreams()) {
                    AuthorizeManager.addPolicy(context, bs, Constants.READ, Group.findByName(context, "Anonymous"));
                }
                AuthorizeManager.addPolicy(context, bundle, Constants.READ, Group.findByName(context, "Anonymous"));
            }

            workingItem.update();
            context.commit();

            sb.append(workingItem.getHandle() + " authorized for group \"Anonymous\"\n");
        } catch (AuthorizeException e) {
            result = Curator.CURATE_ERROR;
            sb.append("Authorization failure on item: " + item.getHandle() + "\nAborting...");
        }
    }

    /**
     * Scan the item and delete the READ policies associated with either of our 3 special groups
     * 
     * @param c
     * @param item
     * @throws SQLException
     */
    private void removeReadPolicies(Context c, Item item) throws SQLException {
        // The special groups whose READ permissions we'll be deleting. Leave others alone.
        List<Group> affectedGroups = new ArrayList<Group>();
        affectedGroups.add(Group.findByName(c, "Anonymous"));
        affectedGroups.add(Group.findByName(c, "Administrator"));
        affectedGroups.add(Group.findByName(c, "member"));

        // Start with a blank slate.
        List<ResourcePolicy> allReadPolicies = new ArrayList<ResourcePolicy>();

        // Add item's READ policies to the bucket list
        allReadPolicies.addAll(AuthorizeManager.getPoliciesActionFilter(c, item, Constants.READ));

        // Add in all the bundle and bitstream READ policies
        for (Bundle bundle : item.getBundles()) {
            allReadPolicies.addAll(AuthorizeManager.getPoliciesActionFilter(c, bundle, Constants.READ));
            for (Bitstream bs : bundle.getBitstreams()) {
                allReadPolicies.addAll(AuthorizeManager.getPoliciesActionFilter(c, bs, Constants.READ));
            }
        }

        // Delete only those policies that fall into our special groups
        for (ResourcePolicy readPolicy : allReadPolicies) {
            if (affectedGroups.contains(readPolicy.getGroup())) {
                readPolicy.delete();
            }
        }
    }

}
