/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing an author profile in DSpace
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorProfile extends DSpaceObject
{

    public static final String AUTHOR_PROFILE_SCHEMA = "authorProfile";

    private static final Logger log = Logger.getLogger(AuthorProfile.class);

    /** The row in the table representing this eperson */
    private TableRow myRow;

    private Bitstream authorProfile;

    /**
     * Construct an EPerson
     *
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    AuthorProfile(Context context, TableRow row) throws SQLException {
        super(context);
        myRow = row;

        TableRow bitstreamLinkRow = DatabaseManager.querySingleTable(context, "authorprofile2bitstream", "SELECT bitstream_id FROM authorprofile2bitstream WHERE authorprofile_id = ?", getID());
        if(bitstreamLinkRow == null)
        {
            authorProfile = null;
        }else{
            authorProfile = Bitstream.find(context, bitstreamLinkRow.getIntColumn("bitstream_id"));
        }


        // Cache ourselves
        context.cache(this, row.getIntColumn("authorprofile_id"));
        clearDetails();
    }

    /**
     * Create a new eperson
     *
     * @param context
     *            DSpace context object
     */
    public static AuthorProfile create(Context context) throws SQLException,
            AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an Author Profile");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "authorprofile");

        AuthorProfile authorProfile = new AuthorProfile(context, row);

        log.info(LogManager.getHeader(context, "create_author_profile", "create_author_profile_id="
                + authorProfile.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.AUTHOR_PROFILE, authorProfile.getID(), null));

        return authorProfile;
    }


    @Override
    public String toString() {
        return "AuthorProfile{" + "type=" + getType() + ", ID=" + getID() + ", handle='" + getHandle() + '\'' + ", name='" + getName() + '\'' + '}';
    }

    /**
     * Update the Author profile
     */
    public void update() throws SQLException, AuthorizeException
    {
        if(!AuthorizeManager.isAdmin(getContext())) {
            throw new AuthorizeException("Only admins can update an author profile.");
        }


        log.info(LogManager.getHeader(ourContext, "update_authorprofile",
                "authorprofile_id=" + getID()));
        if (modifiedMetadata) {
            updateMetadata();
            clearDetails();
        }
        if(authorProfile!=null)
        {
            authorProfile.update();
        }
        ourContext.removeCached(this,getID());
    }

    @Override
    public void updateLastModified() {
        //No-Op
    }

    public void delete() throws SQLException, AuthorizeException {
        if (!AuthorizeManager.isAdmin(ourContext))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete an Author profile");
        }

        ourContext.addEvent(new Event(Event.DELETE, Constants.AUTHOR_PROFILE, getID(), ""));
        // Remove from cache
        ourContext.removeCached(this, getID());


        // Delete the Dublin Core
        removeMetadataFromDatabase();

                // Remove ourself
        DatabaseManager.deleteByValue(ourContext, "authorprofile2bitstream", "authorprofile_id", getID());
        if(authorProfile!=null) authorProfile.delete();
        DatabaseManager.delete(ourContext, myRow);


        log.info(LogManager.getHeader(ourContext, "delete_authorprofile",
                "authorprofile_id=" + getID()));
    }


    @Override
    public int getType() {
        return Constants.AUTHOR_PROFILE;
    }

    @Override
    public int getID() {
        return myRow.getIntColumn("authorprofile_id");
    }

    @Override
    public String getHandle() {
        //Author profiles don't have a handle
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    /**
    * Return a hash code for this object.
    *
    * @return int hash of object
    */
   @Override
   public int hashCode()
   {
       int hash = Constants.AUTHOR_PROFILE;
       hash = 89 * hash + this.getID();
       return hash;
   }

    public Bitstream setAuthorProfilePicture(InputStream is,String filename,String mime) throws SQLException, AuthorizeException, IOException {
        if (!AuthorizeManager.isAdmin(ourContext))
        {
            throw new AuthorizeException(
                    "You must be an admin to set an author profile");
        }

        TableRow linkRow = DatabaseManager.querySingleTable(ourContext, "authorprofile2bitstream", "SELECT * FROM authorprofile2bitstream WHERE authorprofile_id = ?", getID());
        //When our inputstream is null we clear the bitstream identifier !
        if(is == null)
        {
            if(linkRow != null)
            {
                linkRow.setColumnNull("bitstream_id");
                DatabaseManager.update(ourContext, linkRow);
                authorProfile = null;
                return null;
            }
        }

        if(linkRow == null)
        {
            linkRow = DatabaseManager.create(ourContext, "authorprofile2bitstream");
            linkRow.setColumn("authorprofile_id", getID());
        }

        Bitstream newProfilePicture = Bitstream.create(ourContext, is);
        newProfilePicture.setName(filename);
        newProfilePicture.setFormat(BitstreamFormat.findByMIMEType(ourContext, mime));
        newProfilePicture.setSequenceID(1);

        linkRow.setColumn("bitstream_id", newProfilePicture.getID());
        authorProfile = newProfilePicture;

        // now create policy for logo bitstream
        // to match our READ policy
        AuthorizeManager.addPolicy(ourContext, authorProfile, Constants.READ, Group.find(ourContext, Group.ANONYMOUS_ID));

        log.info(LogManager.getHeader(ourContext, "set_author_profile",
                "author_profile_id=" + getID() + "bitstream_id="
                        + newProfilePicture.getID()));


        linkRow.setColumn("bitstream_id", newProfilePicture.getID());


        DatabaseManager.update(ourContext, linkRow);

        return newProfilePicture;
    }

    public Bitstream getAuthorProfilePicture()
    {
        return authorProfile;
    }

    /**
     * Test main method to test the adding of an author profile !
     * @param args shouldn't be used
     */
    public static void main(String[] args) throws SQLException, AuthorizeException, IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        Context context = new Context();
        context.turnOffAuthorisationSystem();
        AuthorProfile authorProfile = AuthorProfile.create(context);

        authorProfile.update();
        //Commit context & ensure it ends up in the discovery consumer
        context.commit();

        System.out.print("Check discovery, press any key to continue !");
        System.out.flush();

        input.readLine();

        authorProfile.delete();
        context.commit();
    }


    protected Logger getLogger() {
        return log;
    }

    protected Context getContext() {
        return ourContext;
    }

    public static AuthorProfile find(Context context, int id) throws SQLException {
        // First check the cache
        AuthorProfile fromCache = (AuthorProfile) context.fromCache(AuthorProfile.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "authorprofile", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new AuthorProfile(context, row);
        }
    }



    public static AuthorProfile[] findAll(Context context) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(context, "authorprofile",
                "SELECT * FROM authorprofile");

        List<AuthorProfile> authorProfiles = new ArrayList<AuthorProfile>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                AuthorProfile fromCache = (AuthorProfile) context.fromCache(
                        AuthorProfile.class, row.getIntColumn("authorprofile_id"));

                if (fromCache != null)
                {
                    authorProfiles.add(fromCache);
                }
                else
                {
                    authorProfiles.add(new AuthorProfile(context, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        AuthorProfile[] authorProfileArray = new AuthorProfile[authorProfiles.size()];
        authorProfileArray = authorProfiles.toArray(authorProfileArray);

        return authorProfileArray;


    }

    public static AuthorProfile findByVariant(Context context,String variant) throws AuthorizeException, SQLException {
        TableRow row;

        int field=MetadataField.findByElement(context, Constants.AUTHOR_PROFILE,"author",null).getFieldID();
        row = DatabaseManager.querySingle(context, "select authorprofile_id from authorprofile as a join metadatavalue as r on (a.authorprofile_id=r.resource_id and r.metadata_field_id=? and r.text_value=?)", field, variant);
        if(row!=null){
            return new AuthorProfile(context,row);
        }
        return null;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthorProfile that = (AuthorProfile) o;

        if (this.getID()!=that.getID()) return false;

        return true;
    }


}
