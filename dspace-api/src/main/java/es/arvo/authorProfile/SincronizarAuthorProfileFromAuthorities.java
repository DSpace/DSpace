/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package es.arvo.authorProfile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.AuthorProfile;
import org.dspace.content.DCDate;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Suspendable;
import org.dspace.storage.rdbms.DB;
import org.dspace.storage.rdbms.DB.DatosPersona;

/**
 * @author Adán Roman Ruiz (Arvo)
 */
@Suspendable
public class SincronizarAuthorProfileFromAuthorities {
	private final static Logger log = Logger.getLogger(SincronizarAuthorProfileFromAuthorities.class);

    private static  void createAuthorProfile(Context context,DatosPersona datosPersona) throws SQLException, AuthorizeException {
	AuthorProfile profile= AuthorProfile.create(context);
	
	profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "authority","id", null, ""+datosPersona.getId());
	if(StringUtils.isNotBlank(datosPersona.getNombre())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name","first", null, datosPersona.getNombre());
	}
	if(StringUtils.isNotBlank(datosPersona.getApellidos())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name","last", null, datosPersona.getApellidos());
	}
	// Mails por defecto privados
	if(StringUtils.isNotBlank(datosPersona.getEmail())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "email",null, null, datosPersona.getEmail());
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "email","private", null, "true");
	}
	if(StringUtils.isNotBlank(datosPersona.getAutor())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "author",null,null, datosPersona.getAutor());
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name","variant",null, datosPersona.getAutor());
	}
	if(StringUtils.isNotBlank(datosPersona.getCentro())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "department","name", null, datosPersona.getCentro());
	}
	if(StringUtils.isNotBlank(datosPersona.getGoogleScholar())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","google", null, datosPersona.getGoogleScholar());
	}
	if(StringUtils.isNotBlank(datosPersona.getResearcherID())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","researcher", null, datosPersona.getResearcherID());
	}
	if(StringUtils.isNotBlank(datosPersona.getScopusID())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","scopus", null, datosPersona.getScopusID());
	}
	if(StringUtils.isNotBlank(datosPersona.getDialnet())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","dialnet", null, datosPersona.getDialnet());
	}
	if(StringUtils.isNotBlank(datosPersona.getOrcid())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","orcid", null, datosPersona.getOrcid());
	}
	addUpdateDate(profile);
	profile.update();
    }

    // ARVO: TODO: Revisar como quieren lo de los alias, que es multiple y lo piso.
    // Incompleto, no se va a usar en prod
    private  static void updateAuthorProfile(AuthorProfile profile, DatosPersona datosPersona) throws SQLException, AuthorizeException {
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name","first", null);
	if(StringUtils.isNotBlank(datosPersona.getNombre())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name","first", null, datosPersona.getNombre());
	}
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name","last", null);
	if(StringUtils.isNotBlank(datosPersona.getApellidos())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name","last", null, datosPersona.getApellidos());
	}
	// Mails por defecto privados
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "email",null, null);
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "email","private", null);
	if(StringUtils.isNotBlank(datosPersona.getEmail())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "email",null, null, datosPersona.getEmail());
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "email","private", null, "true");
	}
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "author",null,null);
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name","variant",null);
	if(StringUtils.isNotBlank(datosPersona.getAutor())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "author",null,null, datosPersona.getAutor());
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "name","variant",null, datosPersona.getAutor());
	}
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "department","name", null);
	if(StringUtils.isNotBlank(datosPersona.getCentro())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "department","name", null, datosPersona.getCentro());
	}
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","google", null);
	if(StringUtils.isNotBlank(datosPersona.getGoogleScholar())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","google", null, datosPersona.getGoogleScholar());
	}
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","researcher", null);
	if(StringUtils.isNotBlank(datosPersona.getResearcherID())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","researcher", null, datosPersona.getResearcherID());
	}
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","scopus", null);
	if(StringUtils.isNotBlank(datosPersona.getScopusID())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","scopus", null, datosPersona.getScopusID());
	}
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","dialnet", null);
	if(StringUtils.isNotBlank(datosPersona.getDialnet())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","dialnet", null, datosPersona.getDialnet());
	}
	profile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","orcid", null);
	if(StringUtils.isNotBlank(datosPersona.getOrcid())){
	    profile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "id","orcid", null, datosPersona.getOrcid());
	}
	addUpdateDate(profile);
	profile.update();
	
    }

    private  static boolean igualesAuthorYAutoridad(AuthorProfile authorProfile,DatosPersona datosPersona) {
	if(authorProfile!=null && datosPersona!=null){
	    Metadatum[] ids=authorProfile.getMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "authority","id", Item.ANY);
	    if(ids!=null){
		for(int i=0;i<ids.length;i++){
		    if(Integer.parseInt(ids[i].value)==datosPersona.id){
			return true;
		    }
		}
	    }
	}
	return false;
    }

    private  static AuthorProfile findAuthorProfileFromAuthorityId(AuthorProfile[] authorProfiles, DatosPersona datosPersona) {
	for(int i=0;i<authorProfiles.length;i++) {
	    if(igualesAuthorYAutoridad(authorProfiles[i],datosPersona)){
		return authorProfiles[i];
	    }
	}
	return null;
    }
    private  static void addUpdateDate(AuthorProfile authorProfile)
    {
        authorProfile.clearMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "updateDate", null, Item.ANY);
        Calendar cal = Calendar.getInstance();
        DCDate date = new DCDate(cal.getTime());
        authorProfile.addMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA, "updateDate", null, null, date.toString());
    }

    public static synchronized void sincronize(Context context) {
	try {
	    AuthorProfile[] authorProfiles=AuthorProfile.findAll(context);
	    List<DatosPersona> autoridades=DB.getInstance().getAutoridades();
	    log.debug("Numero de authorProfiles:"+authorProfiles.length);
	    log.debug("Numero de autoridades:"+autoridades.size());
	    for(int i=0;i<autoridades.size();i++){
		//AuthorProfile authorProfile=findAuthorProfileFromAuthorityId(authorProfiles,autoridades.get(i));
		// ARVO TODO: Aqui se podria hacer una sincronizacion bidireccional usando fechas o algo asi. 
		// Otra alternativa. solo en direccion a la bbdd con la curation y, en la importacion de metadatos desde el csv,en la direccion de los metadatos.
//		if(igualesAuthorYAutoridad(authorProfile,autoridades.get(i))){
//		    updateAuthorProfile(authorProfile,autoridades.get(i));
//		}else{
		    createAuthorProfile(context,autoridades.get(i));
//		}
	    }
	    context.commit();
	} catch (SQLException e1) {
	    log.debug("Error de bbdd:"+e1.getMessage());
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	    context.abort();
	} catch (AuthorizeException e) {
	    log.debug("Error de autorizacion:"+e.getMessage());
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    context.abort();
	}
    }
}
