/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package ar.edu.unlp.sedici.dspace.administer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;

public final class CreateGroup {

	/** DSpace Context object */

	private Context context;
	static final Logger logger = Logger.getLogger(CreateGroup.class);

	/**
	 * For invoking via the command line. If called with no command line
	 * arguments, it will negotiate with the user for the user details
	 * 
	 * @param argv
	 *            command-line arguments
	 */

	public static void main(String[] argv) throws Exception {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();

		CreateGroup cu = new CreateGroup();

		options.addOption("n", "name", true, "Group Name");
		options.addOption("p", "parent", true, "Parent Group Name");
		Option oComm = new Option( "c", "communities", true, "Community list, separated by spaces");
		oComm.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(oComm);
		

		CommandLine line = parser.parse(options, argv);

		if (line.hasOption("n")) {
			String[] communitiesIds = line.getOptionValues("c");
			if (communitiesIds == null)
				communitiesIds = new String[0];
			
			cu.createGroup(line.getOptionValue("n"), line.getOptionValue("p"), Arrays.asList(communitiesIds));
		} else {
			cu.negotiateGroupDetails();
		}
	}

	/**
	 * constructor, which just creates and object with a ready context
	 * 
	 * @throws Exception
	 */
	public CreateGroup() throws Exception {
		context = new Context();

	}

	/**
	 * Method which will negotiate with the user via the command line to obtain
	 * the user's details
	 * 
	 * @throws Exception
	 */
	private void negotiateGroupDetails() throws Exception {
		// For easier reading of typing
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Creating a group");

		boolean dataOK = false;

		String groupName = null;
		String parentGroupName = null;
		List<String> communitiesIds = new LinkedList<String>();
		while (!dataOK) {

			System.out.print("Group name (cannot be anonymous nor administrator): ");
			System.out.flush();
			groupName = input.readLine();

			if (groupName != null) {
				groupName = groupName.trim();
			}

			System.out.print("Parent Group (empty for anonymous): ");
			System.out.flush();
			parentGroupName = input.readLine();
			if (parentGroupName != null) {
				parentGroupName = parentGroupName.trim();
			}
			
			String communityId;
			do{
				System.out.print("Community id (enter blank to skip): ");
				System.out.flush();
				communityId= input.readLine();
				if (communityId == null || "".equals(communityId.trim()))
					break;
				
				communitiesIds.add(communityId.trim());
				
			}while(true);
			
			if (!StringUtils.isEmpty(groupName)) {
				// password OK
				System.out.print("Is the above data correct? (y or n): ");
				System.out.flush();

				String s = input.readLine();

				if (s != null) {
					s = s.trim();
					if (s.toLowerCase().startsWith("y")) {
						dataOK = true;
					}
				}
			} else {
				System.out.println("groupName es required");
			}
		}

		// if we make it to here, we are ready to create a group
		createGroup(groupName, parentGroupName, communitiesIds);
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void createGroup(String name, String parentGroupName, List<String> communitiesIds) throws Exception {
		// Of course we aren't an user yet so we need to
		// circumvent authorisation

		context.turnOffAuthorisationSystem();
		

		try {
			List<Community> communities = new ArrayList<Community>(communitiesIds.size());
			for (String communityId : communitiesIds) {
			
				Community community = resolveCommunity(context, communityId);
		        if (community == null)
		        {
		            throw new IllegalStateException("Community cannot be found: " + communityId);
		        }
		        communities.add(community);
    		}
        
			// Find user group
			Group parentGroup;
			if (parentGroupName == null || "".equals(parentGroupName)) {
				// parentGroupName = "0 {anonymous}";
				parentGroup = null;
			} else {
				if ("1".equals(parentGroupName) || "administrator".equals(parentGroupName)) {
					parentGroupName = "1 {administrator}";
					parentGroup = Group.find(context, 1);

				} else {
					parentGroup = Group.findByName(context, parentGroupName);
				}
				if (parentGroup == null) {
					throw new IllegalStateException("Error, no parentGroup (" + parentGroupName + ") found");
				}
			}

			if (name == null || "".equals(name)) {
				throw new IllegalStateException("Debe especificar el nombre del grupo");
			}

			Group group = Group.findByName(context, name);

			if (group != null) {
				throw new IllegalStateException("Error, El grupo " + name + " ya se encuentra registrado");
			}
			group = Group.create(context);
			group.setName(name);

			group.update();
			if (parentGroup != null) {
				parentGroup.addMember(group);
				parentGroup.update();
			}
			
			for (Community community : communities) {
			    AuthorizeManager.addPolicy(context,community,Constants.ADMIN,group);
			}
			
			context.complete();
			logger.info("Se creo el grupo " + group.getName() + " con id " + group.getID());
			System.out.println("Se creo el grupo " + group.getName() + " con id " + group.getID());
		} catch (Exception e) {
			context.abort();
			throw e;
		}finally {
			context.restoreAuthSystemState();
		}
	}

	protected Community resolveCommunity(Context c, String communityID) throws SQLException {
		Community community = null;

		if (communityID.indexOf('/') != -1) {
			// has a / must be a handle
			community = (Community) HandleManager.resolveToObject(c, communityID);

			// ensure it's a community
			if ((community == null) || (community.getType() != Constants.COMMUNITY)) {
				community = null;
			}
		} else {
			try{
				community = Community.find(c, Integer.parseInt(communityID));
			}catch (NumberFormatException e) {
				community = null;
			}
		}

		return community;
	}
}
