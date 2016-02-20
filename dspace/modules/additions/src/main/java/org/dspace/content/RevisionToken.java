/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Database access class representing a Dublin Core metadata value.
 * It represents a value of a given <code>MetadataField</code> on an Item.
 * (The Item can have many values of the same field.)  It contains                                           element, qualifier, value and language.
 * the field (which names the schema, element, and qualifier), language,
 * and a value.
 *
 * @author Martin Hald
 * @see org.dspace.content.MetadataSchema
 * @see org.dspace.content.MetadataField
 */
public class RevisionToken
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(RevisionToken.class);
    int revisionTokenId;
    String tipo;
    String email;
    String token;
    String handleRevisado;
    String workspaceId;
    String revisionId ;
    
    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getRevisionTokenId() {
        return revisionTokenId;
    }

    public void setRevisionTokenId(int revisionTokenId) {
        this.revisionTokenId = revisionTokenId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHandleRevisado() {
        return handleRevisado;
    }

    public void setHandleRevisado(String handleRevisado) {
        this.handleRevisado = handleRevisado;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }

    /** The row in the table representing this type */
    private TableRow row;

    /**
     * Construct the metadata object from the matching database row.
     *
     * @param row database row to use for contents
     */
    public RevisionToken(TableRow row)
    {
        if (row != null)
        {
            revisionTokenId = row.getIntColumn("revision_token_id");
            tipo  = row.getStringColumn("tipo");
            email  = row.getStringColumn("email");
            token  = row.getStringColumn("token");
            handleRevisado  = row.getStringColumn("handle_revisado");
            workspaceId  = row.getStringColumn("workspace_id");
            revisionId  = row.getStringColumn("revision_id");
            this.row = row;
        }
    }

    /**
     * Default constructor.
     * @param handleRevisado 
     * @param token 
     * @param email 
     */
    public RevisionToken(String email,String tipo, String token, String handleRevisado)
    {
	this.tipo=tipo;
	this.email=email;
	this.token=token;
	this.handleRevisado=handleRevisado;
    }

 
    public void create(Context context) throws SQLException, AuthorizeException
    {
        // Create a table row and update it with the values
        row = DatabaseManager.row("revision_token");
        row.setColumn("revision_token_id", revisionTokenId);
        row.setColumn("email", email);
        row.setColumn("tipo", tipo);
        row.setColumn("token", token);
        row.setColumn("handle_revisado", handleRevisado);
        row.setColumn("workspace_id", workspaceId);
        row.setColumn("revision_id", revisionId);
        DatabaseManager.insert(context, row);
    }

  
    public static RevisionToken find(Context context, String token)
            throws IOException, SQLException, AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context, "revision_token",
                "SELECT * FROM revision_token where token= ? ",
                token);

        TableRow row = null;
        try
        {
            if (tri.hasNext())
            {
                row = tri.next();
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

        if (row == null)
        {
            return null;
        }
        else
        {
            return new RevisionToken(row);
        }
    }
    
    public static ArrayList<RevisionToken> findRevisionsOfHandle(Context context, String handle)
            throws IOException, SQLException, AuthorizeException
    {
        // Grab rows from DB
	log.debug("Buscando revisiones del handle:"+handle);
        TableRowIterator tri = DatabaseManager.queryTable(context, "revision_token","SELECT * FROM revision_token where handle_revisado= ? and tipo='R' and revision_id is not null",handle);

        TableRow row = null;
        ArrayList<RevisionToken> resultado=new ArrayList<RevisionToken>();
        try
        {
            while (tri.hasNext())
            {
                row = tri.next();
                resultado.add(new RevisionToken(row));              
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

           return resultado;
    }
    
    public static RevisionToken findItemOfRevision(Context context, int id)
            throws IOException, SQLException, AuthorizeException
    {
        // Grab rows from DB
	log.debug("Buscando item de la revision del id:"+id);
        TableRowIterator tri = DatabaseManager.queryTable(context, "revision_token","SELECT * FROM revision_token where revision_id= ? and tipo='R' and revision_id is not null",""+id);

        TableRow row = null;
        try
        {
            while (tri.hasNext())
            {
                row = tri.next();
                return new RevisionToken(row);
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

            return null;
    }
    
    public static ArrayList<RevisionToken> findJuiciosOfHandle(Context context, String handle)
            throws IOException, SQLException, AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context, "revision_token","SELECT * FROM revision_token where handle_revisado= ? and tipo='J' and revision_id is not null",handle);

        TableRow row = null;
        ArrayList<RevisionToken> resultado=new ArrayList<RevisionToken>();
        try
        {
            while (tri.hasNext())
            {
                row = tri.next();
                resultado.add(new RevisionToken(row));              
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

            return resultado;
    }

     /**
     * Update the metadata value in the database.
     *
     * @param context dspace context
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void update(Context context) throws SQLException, AuthorizeException
    {
	row.setColumn("revision_token_id", revisionTokenId);
	row.setColumn("email", email);
	row.setColumn("tipo", tipo);
        row.setColumn("token", token);
        row.setColumn("handle_revisado", handleRevisado);
        if(workspaceId==null){
            row.setColumnNull("workspace_id");
        }else{
            row.setColumn("workspace_id", workspaceId);
        }
        if(revisionId==null){
            row.setColumnNull("revision_id");
        }else{
            row.setColumn("revision_id", revisionId);
        }
        DatabaseManager.update(context, row);
    }

    /**
     * Delete the metadata field.
     *
     * @param context dspace context
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void delete(Context context) throws SQLException, AuthorizeException
    {
        DatabaseManager.delete(context, row);
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same MetadataValue
     * as this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final RevisionToken other = (RevisionToken) obj;
        if (this.email != other.email)
        {
            return false;
        }
        if (this.token != other.token)
        {
            return false;
        }
        if (this.handleRevisado != other.handleRevisado)
        {
            return false;
        }
        if (this.workspaceId != other.workspaceId)
        {
            return false;
        }
        if (this.revisionId != other.revisionId)
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 71 * hash + (""+email+token+handleRevisado+workspaceId+revisionId).hashCode();
        return hash;
    }

    public static RevisionToken findUnique(Context context,String token) throws SQLException {
	TableRow qResult = DatabaseManager.querySingle(context,	"SELECT * FROM revision_token WHERE token = ?",token);
	if(qResult!=null){
	    return new RevisionToken(qResult);
	}else{
	    return null;
	}

    }
    /**
     * Obtiene todas las revisiones finalizadas.
     *
     * @param context
     *            DSpace context object
     * @return an iterator over the items in the archive.
     * @throws SQLException
     */
    public static RevisionToken[] findAllRevisiones(Context context) throws SQLException
    {
        String myQuery = "SELECT * FROM revision_token WHERE tipo='R' and revision_id is not null";

        TableRowIterator rows = DatabaseManager.queryTable(context, "revision_token", myQuery);

        try
        {
            List<TableRow> revisionRows = rows.toList();

            RevisionToken[] revisionToken = new RevisionToken[revisionRows.size()];

            for (int i = 0; i < revisionRows.size(); i++)
            {
                TableRow row = (TableRow) revisionRows.get(i);

                // First check the cache
                RevisionToken fromCache = (RevisionToken) context.fromCache(RevisionToken.class, row
                        .getIntColumn("revision_token_id"));

                if (fromCache != null)
                {
                    revisionToken[i] = fromCache;
                }
                else
                {
                    revisionToken[i] = new RevisionToken(row);
                }
            }

            return revisionToken;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }
    /**
     * Devuelve las revisiones completas de un revisor
     * @param context
     * @param email
     * @return
     * @throws SQLException
     */
    public static RevisionToken[] findRevisionesOfRevisor(Context context,String email) throws SQLException {
	String myQuery = "SELECT * FROM revision_token WHERE tipo='R' and revision_id is not null and email=?";

	TableRowIterator rows = DatabaseManager.queryTable(context, "revision_token", myQuery,email);

	try
	{
	    List<TableRow> revisionRows = rows.toList();

	    RevisionToken[] revisionToken = new RevisionToken[revisionRows.size()];

	    for (int i = 0; i < revisionRows.size(); i++)
	    {
		TableRow row = (TableRow) revisionRows.get(i);

		// First check the cache
		RevisionToken fromCache = (RevisionToken) context.fromCache(RevisionToken.class, row
			.getIntColumn("revision_token_id"));

		if (fromCache != null)
		{
		    revisionToken[i] = fromCache;
		}
		else
		{
		    revisionToken[i] = new RevisionToken(row);
		}
	    }

	    return revisionToken;
	}
	finally
	{
	    if (rows != null)
	    {
		rows.close();
	    }
	}
    }
    
    public static RevisionToken verificaTokenWorkspaceID(Context context,String token,String workspaceID) throws SQLException {
	if(token==null || workspaceID==null){
	    return null;
	}
	TableRow qResult = DatabaseManager.querySingle(context,	"SELECT * FROM revision_token WHERE token = ? AND workspace_id = ?",token,workspaceID);
	if(qResult!=null){
	    return new RevisionToken(qResult);
	}else{
	    return null;
	}
    }
    
    public static void updateWorkspaceID(Context context,String token,String workspaceID) throws SQLException, AuthorizeException {
	TableRow qResult = DatabaseManager.querySingle(context,	"SELECT * FROM revision_token WHERE token = ?",token);
	qResult.setTable("revision_token");
	if(qResult!=null){
	    RevisionToken rt=new RevisionToken(qResult);
	    rt.setWorkspaceId(workspaceID);
	    rt.update(context);
	}
    }
    ///e-ieo/submit/workspaceID=87&tokenEvaluacion=f2806a3669c480a7be20b2d6fb6f1549
    public static String getTokenParameter(String url){
	if(StringUtils.isNotBlank(url)&& url.indexOf("tokenEvaluacion=")!=-1){
	    String params=url.substring(url.indexOf("tokenEvaluacion=")+"tokenEvaluacion=".length());
	    if(params.indexOf("&")==-1){
		return params;
	    }else{
		return params.substring(0,params.indexOf("="));
	    }
	}
	return null;
    }
    public static boolean estaFinalizado(Context context,String token) throws SQLException{
	TableRow qResult = DatabaseManager.querySingle(context,	"SELECT revision_id FROM revision_token WHERE token = ?",token);
	if(qResult!=null && qResult.getStringColumn("revision_id")!=null){
	    return true;
	}
	return false;
    }

    public static void remove(Context context,int id) throws SQLException {
	DatabaseManager.delete(context, "revision_token", id);
    }


}
