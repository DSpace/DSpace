package ar.edu.unlp.sedici.dspace.xmlui.app.xmlui.aspect.contextHelper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;


import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.xml.sax.SAXException;
import org.dspace.core.Constants;

/**
 * 
 * @author sedici.unlp.edu.ar
 *
 */
public class LocalConfigTransformer extends AbstractDSpaceTransformer {

	@Override
	public void addUserMeta(UserMeta userMeta) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		
		EPerson eperson = this.context.getCurrentUser();
		if (eperson ==null)
			return;

		/**
		 * Carga los grupos a los que pertenece el usuario actualmente logueado, sólo si el grupo posee permisos de ADD, 
		 * ADMIN o es Administrator.
		 * Se coloca un grupo en cada /document/meta/userMeta/metadata[@element='identifier' and @qualifier='group']
		 */
		ArrayList<Integer> actions = new ArrayList<Integer>();
        actions.add(Constants.ADD); actions.add(Constants.ADMIN);
        
		for (Group g:Group.allMemberGroups(this.context, eperson)) {
			if((hasAtLeastOneAction(-1,actions,g.getID(),true) || g.getID() == Group.ADMIN_ID)
					&& g.getID() != Group.ANONYMOUS_ID){
				userMeta.addMetadata("identifier", "group").addContent(g.getName());
			}
		}
	}
	
	/**
	 * Verifica si, dado un grupo dado y un TIPO de DSpaceObject en particular, existe al menos uno de los 
	 * permisos solicitados en "actions".
	 * @parameter dsoType --> Si se pasa el valor "-1", entonces busca en todos los tipos de DSO
	 * @parameter actions --> arreglo de enteros, donde cada entero se corresponde a una constante "action"
	 * 						sobre un DSO, definidas en la clase org.dspace.core.Constants
	 * @parameter eperson_id --> ID de un eperson o epersongroup correspondiente
	 * @parameter isGroup --> TRUE si el ID corresponde a un grupo particular
	 * 
	 * @return TRUE cuando se encuentra al menos un ResourcePolicy que corresponda a los parámetros indicados. 
	 */
	private boolean hasAtLeastOneAction(int dsoType, ArrayList<Integer> actions, int eperson_id, boolean isGroup) 
					throws SQLException{
		
		Iterator iterator = actions.iterator();
		String actionWhere = "";
		while(iterator.hasNext()){
			actionWhere += "action_id = " + iterator.next().toString();
			if(iterator.hasNext()){
				 actionWhere += " OR ";
			}
		}
		String queryText="SELECT * FROM resourcepolicy " +
				((isGroup)?"WHERE epersongroup_id= ? ":"WHERE eperson_id= ? ") +
				"AND (" + actionWhere + ") " +
				((dsoType != -1)?"AND resource_type_id = ? ":" ");
		TableRowIterator tri;
		if(dsoType != -1){
			tri = DatabaseManager.queryTable(this.context, "resourcepolicy",queryText,eperson_id, dsoType);
		}else{
			tri = DatabaseManager.queryTable(this.context, "resourcepolicy",queryText,eperson_id);
		}
        return tri.hasNext();
	}
	
	
}

