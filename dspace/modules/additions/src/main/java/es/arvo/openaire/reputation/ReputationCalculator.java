package es.arvo.openaire.reputation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.AuthorProfile;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Metadatum;
import org.dspace.content.RevisionToken;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DB;

import es.arvo.dspace.submit.step.CompleteStep;

public class ReputationCalculator {
	// Reputacion 50 sobre 100
    public static int  DEFAULT_REPUTATION=50;
    public static int  REVISIONES_K=2;
    public static int  JUICIOS_K=2;
    // Factor de decrecimiento de la disminucion de impacto de la reputacion habiendo varios autores.
    public static double ALPHA=Double.parseDouble(ConfigurationManager.getProperty("openaire.reputation.calculation.alpha"));
    // Valor minimo en el que se tienen que mover la reputacion de todos los revisores del repositorio para parar el bucle. 
    public static int  DIFERENCIAL_REVISORES=1;

    // Valor minimo en el que se tienen que mover la reputacion de todos los autores del repositorio para parar el bucle. 
    public static int  DIFERENCIAL_AUTORES=1;
    
    // Reputacion de autores (como autor). Solo se tiene en cuenta el de las autoridades <idDeAutoridad, reputacion>
    HashMap<String, Integer> reputacionAutoridades=new HashMap<String, Integer>();
    
    // Reputacion de publicaciones
    HashMap<Integer, Integer> reputacionPublicacionesInicial=new HashMap<Integer, Integer>();
    
    // Reputacion de publicaciones
    HashMap<Integer, Integer> reputacionPublicaciones=new HashMap<Integer, Integer>();
    
    // Reputacion de revisores <email, reputacion>
    HashMap<String, Integer> reputacionRevisores=new HashMap<String, Integer>();
    
    // Reputacion de revisiones <revisionTokenId, reputacion>
    HashMap<Integer, Integer> reputacionRevisiones=new HashMap<Integer, Integer>();
    
    // Revisiones
    RevisionToken[] revisionesToken;
    
    Context context=null;
    
    private void run() {
	
	try {
	    context = new Context();
	    context.turnOffAuthorisationSystem();
	    limpiarRevisionTokenConItemsEliminados();
	    context.restoreAuthSystemState();
	    loadRevisores();
	    int reputationChange;
	    do{
		calcularReputacionRevisiones();
		reputationChange=calcularReputacionRevisores();
	    }while(reputationChange>DIFERENCIAL_REVISORES);
	    //
	    do{
		calcularReputacionPublicaciones();
		reputationChange=calcularReputacionAutores();
	    }while(reputationChange>DIFERENCIAL_AUTORES);

	    context.turnOffAuthorisationSystem();
	  //  printReputaciones();
	    
	    guardarReputacionPublicaciones(context);
	    guardarReputacionRevisiones(context);
	    guardarReputacionAutoridades(context);
	    
	    context.restoreAuthSystemState();
	    context.complete();

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (AuthorizeException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}finally{
	    if(context!=null){
		//O complete, hay que esperar a ver
		context.abort();
	    }
	}
    }

    /**
     * Elimina de la tabla REvision_token las referencias a items completos, pero eliminados antes de reclacular reputaciones
     * @throws SQLException 
     * @throws AuthorizeException 
     * @throws IOException 
     */
    private void limpiarRevisionTokenConItemsEliminados() throws SQLException, IOException, AuthorizeException {
	RevisionToken[] revisiones=RevisionToken.findAllRevisiones(context);
	for(int i=0;i<revisiones.length;i++){
	    Item itemRevision=Item.find(context, Integer.parseInt(revisiones[i].getRevisionId()));
	    if (itemRevision==null){
		RevisionToken.remove(context,revisiones[i].getRevisionTokenId());
	    }else{
		ArrayList<RevisionToken> juicios=RevisionToken.findJuiciosOfHandle(context, itemRevision.getHandle());
		for(int j=0;j<juicios.size();j++){
		    Item itemJuicio=Item.find(context, Integer.parseInt(juicios.get(j).getRevisionId()));
		    if(itemJuicio==null){
			RevisionToken.remove(context,juicios.get(j).getRevisionTokenId());
		    }
		}
	    }
	}
    }

private void printReputaciones() {
    	Iterator it=reputacionAutoridades.keySet().iterator();
    	System.out.println("Reputacion de autoridades");
    	while(it.hasNext()){
    		String key=(String) it.next();
    		Integer reputacion=reputacionAutoridades.get(key);
    		
    			System.out.println(key+":"+reputacion);
    		
    	}
    	it=reputacionRevisores.keySet().iterator();
    	System.out.println("Reputacion de revisores");
    	while(it.hasNext()){
    		String key=(String) it.next();
    		Integer reputacion=reputacionRevisores.get(key.toString());
    		
    			System.out.println(key+":"+reputacion);
    		
    	}
    	it=reputacionRevisiones.keySet().iterator();
    	System.out.println("Reputacion de revisiones");
    	while(it.hasNext()){
    		Integer key=(Integer) it.next();
    		Integer reputacion=reputacionRevisiones.get(key);
    		if(reputacion!=DEFAULT_REPUTATION){
    			System.out.println(key+":"+reputacion);
    		}
    	}
    	it=reputacionPublicaciones.keySet().iterator();
    	System.out.println("Reputacion de publicaciones");
    	while(it.hasNext()){
    		Integer key=(Integer) it.next();
    		Integer reputacion=reputacionPublicaciones.get(key);
    		if(reputacion!=DEFAULT_REPUTATION){
    			System.out.println(key+":"+reputacion);
    		}
    	}
	}

    private void guardarReputacionAutoridades(Context context) throws SQLException, AuthorizeException {
        AuthorProfile[] authorProfiles=AuthorProfile.findAll(context);
        for(int i=0;i<authorProfiles.length;i++){
            guardarReputacionAutoridad(context,authorProfiles[i]); 
        }
    }

    private void guardarReputacionAutoridad(Context context, AuthorProfile authorProfile) throws SQLException, AuthorizeException {
	String email=authorProfile.getMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA+".email");
	String id=authorProfile.getMetadata(AuthorProfile.AUTHOR_PROFILE_SCHEMA+".authority.id");
	
	// Si el profile no tiene id, intentamos encontrar uno en la bbdd de autoridades
	if(StringUtils.isBlank(id) && StringUtils.isNotBlank(email)){
	    String[] authority=DB.getInstance().executeQueryUnique(ConfigurationManager.getProperty("openaire.sql.author.getByEmail"), "'"+email+"'");
	    if(authority!=null && authority.length>0){
		id=authority[0];
	    }
	}
	
	boolean hasChanges=false;
	if(StringUtils.isBlank(email) && StringUtils.isNotBlank(id)){
	    String[] results=DB.getInstance().executeQueryUnique(ConfigurationManager.getProperty("openaire.emailFromId"),id);
	    if(results!=null && results.length>0){
		email=results[0];
	    }
	}
	
	if(StringUtils.isNotBlank(email)){
	  //  String reputacionAnterior=authorProfile.getMetadata("authorProfile.reputacion.revisor");
	    Integer reputacionNueva=reputacionRevisores.get(email);
	    //if(reputacionRevisores.containsKey(email) && !reputacionNueva.toString().equals(reputacionAnterior)){
	    //if(reputacionNueva!=null && reputacionAnterior!=null){
	    if(authorProfile.getMetadata("authorProfile", "reputacion", "revisor", Item.ANY)!=null && authorProfile.getMetadata("authorProfile", "reputacion", "revisor", Item.ANY).length>0){
		authorProfile.clearMetadata("authorProfile", "reputacion", "revisor", Item.ANY);
		hasChanges=true;
	    }
	    if(reputacionNueva!=null && reputacionNueva!=DEFAULT_REPUTATION){
		authorProfile.addMetadata("authorProfile", "reputacion", "revisor", Item.ANY,""+reputacionRevisores.get(email));
		hasChanges=true;
	    }
	    
	}

	String reputacionAnterior=authorProfile.getMetadata("authorProfile.reputacion.autor");
	String reputacionNueva=reputacionAutoridades.get(id)==null?null:reputacionAutoridades.get(id).toString();
	if(!(reputacionAnterior==null && reputacionNueva==null) && ((reputacionAnterior!=null && !reputacionAnterior.equalsIgnoreCase(reputacionNueva))  || (reputacionNueva!=null && !reputacionNueva.equalsIgnoreCase(reputacionAnterior)))){
	    authorProfile.clearMetadata("authorProfile", "reputacion", "autor", Item.ANY);
	    hasChanges=true;
	}
	if(reputacionNueva!=null && !reputacionNueva.equals(reputacionAnterior)){
	    authorProfile.addMetadata("authorProfile", "reputacion",  "autor", Item.ANY,""+reputacionAutoridades.get(id));
	    hasChanges=true;
	}
	if(hasChanges){
	    authorProfile.update();
	    context.commit();
	}
    }

    private void guardarReputacionRevisiones(Context context) throws SQLException, NumberFormatException, AuthorizeException {
	for(int i=0;i<revisionesToken.length;i++){
	    guardarReputacionRevision(context,revisionesToken[i]);
	}
	
    }

    private void guardarReputacionRevision(Context context, RevisionToken revisionToken) throws NumberFormatException, SQLException, AuthorizeException {
	Item item=Item.find(context, Integer.parseInt(revisionToken.getRevisionId()));
	Integer reputacion=reputacionRevisiones.get(revisionToken.getRevisionTokenId());
	String reputacionAnterior=item.getMetadata("oprm.reputacion");
	if(reputacion!=null && !reputacion.toString().equals(reputacionAnterior)){
	    item.clearMetadata("oprm", "reputacion", null, Item.ANY);
	    item.addMetadata("oprm", "reputacion", null, Item.ANY,""+reputacionRevisiones.get(revisionToken.getRevisionTokenId()));
	    item.update();
	    context.commit();
	}
    }

    private void guardarReputacionPublicaciones(Context context) throws SQLException, AuthorizeException, IOException {
	Integer[] ids=reputacionPublicaciones.keySet().toArray(new Integer[0]);
	for(int i=0;i<ids.length;i++){
	    Item item=Item.find(context, ids[i]);
	    if(item!=null){
		guardarReputacionPublicacion(item);
	    }
	}
    }

    private void guardarReputacionPublicacion(Item item) throws SQLException, AuthorizeException, IOException {
    	ArrayList<RevisionToken> revisiones=RevisionToken.findRevisionsOfHandle(context,item.getHandle());
    	String tieneRevisiones=item.getMetadata("oprm.item.hasRevision");
    	boolean haschanges=false;
    	if(tieneRevisiones==null || (!tieneRevisiones.equals(revisiones.size()>0?CompleteStep.REVISED:CompleteStep.NOT_REVISED))){
    		item.clearMetadata("oprm", "item", "hasRevision", Item.ANY);
    		item.addMetadata("oprm", "item", "hasRevision", Item.ANY, revisiones.size()>0?CompleteStep.REVISED:CompleteStep.NOT_REVISED);
    		haschanges=true;
    	}

    	// Si hay variacion en la reputacion o no hay reputacion
    	if(reputacionPublicacionesInicial.get(item.getID())==null || !reputacionPublicacionesInicial.get(item.getID()).equals(reputacionPublicaciones.get(item.getID()))){
    		item.clearMetadata("oprm", "reputacion", null, Item.ANY);
    		item.addMetadata("oprm", "reputacion", null, Item.ANY,""+reputacionPublicaciones.get(item.getID()));
    		haschanges=true;
    	}
    	if(haschanges){
    		item.update();
    		context.commit();
    		// Pequeña pausa para dar un respiro a solr
    		try {
    			Thread.sleep(50);
    		} catch (InterruptedException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		context.clearCache();
    	}
    }

    /**
     * Cargo la lista de revisores.
     * @throws SQLException
     */
    private void loadRevisores() throws SQLException {
	revisionesToken=RevisionToken.findAllRevisiones(context);
	for(int i=0;i<revisionesToken.length;i++){
	    reputacionRevisores.put(revisionesToken[i].getEmail(), DEFAULT_REPUTATION);
	}
    }

    private int calcularReputacionRevisores() throws SQLException {
	int changes=0;
	//RevisionToken[] revisiones=RevisionToken.findAllRevisiones(context);
	Iterator<String> it=reputacionRevisores.keySet().iterator();
	while(it.hasNext()){
	    changes+=calcularReputacionRevisor(it.next());
	}
	return changes;
    }
    
    private int calcularReputacionRevisor(String email) throws SQLException {
	int valorPrevio=reputacionRevisores.get(email);
	int valorFinal=0;
	RevisionToken[] revisiones=RevisionToken.findRevisionesOfRevisor(context,email);
	if(revisiones.length==0){
	    reputacionRevisores.put(email,DEFAULT_REPUTATION);
	    valorFinal=DEFAULT_REPUTATION;
	}else{
	    int denominador=0;
	    int numerador=0;
	    for(int i=0;i<revisiones.length;i++){
		if(reputacionRevisiones.get(revisiones[i].getRevisionTokenId())!=null){
		    // Intento con simplemente, la media de la reputacion de cada revision
		    denominador+=1;
		    numerador+=reputacionRevisiones.get(revisiones[i].getRevisionTokenId());
		    //		denominador+=reputacionRevisores.get(email);
		    //		numerador+=reputacionRevisores.get(email)*reputacionRevisiones.get(revisiones[i].getRevisionTokenId());
		}
	    }
	    if(denominador!=0){
		reputacionRevisores.put(email,numerador/denominador);
		valorFinal=numerador/denominador;
	    }
	}
	return Math.abs(valorPrevio-valorFinal);
    }

    private void calcularReputacionRevisiones() throws IOException, SQLException, AuthorizeException {
	for(int i=0;i<revisionesToken.length;i++){
	    calculaReputacionRevision(revisionesToken[i]);
	}
    }
    
    /**
     * Reputacion de una revision
     * @param revisionToken
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void calculaReputacionRevision(RevisionToken revisionToken) throws IOException, SQLException, AuthorizeException {
	int revisionId=Integer.parseInt(revisionToken.getRevisionId());
	Item item=Item.find(context, revisionId);
	if(item!=null){
	    ArrayList<RevisionToken> juicios=RevisionToken.findJuiciosOfHandle(context, item.getHandle());
	    if(juicios.size()<JUICIOS_K){
		reputacionRevisiones.put(revisionToken.getRevisionTokenId(), reputacionRevisores.get(revisionToken.getEmail()));
	    }else{
		int denominador=0;
		int numerador=0;
		for(int i=0;i<juicios.size();i++){
		    int reputacionEnjuiciador;
		    if(reputacionRevisores.containsKey(juicios.get(i).getEmail())){
			reputacionEnjuiciador=reputacionRevisores.get(juicios.get(i).getEmail());
		    }else{
			reputacionEnjuiciador=DEFAULT_REPUTATION;
		    }
		    denominador+=reputacionEnjuiciador;
		    numerador+=reputacionEnjuiciador*calculaPuntuacionRevisionToken(context,juicios.get(i));
		}
		reputacionRevisiones.put(revisionToken.getRevisionTokenId(),numerador/denominador);
	    }
	}
    }
    
    /**
     *****NO******Obtiene la puntuacion promedio de los 4 ejes. El primero es formato, si es 0 penaliza en un 30 % el valor total. El resto se promedia
     * Se cambio. Ahora se ignoran esos ejes y se coge de un metadato sin mas
     * @param revisionToken2
     * @return
     * @throws SQLException 
     * @throws NumberFormatException 
     */
    private static int calculaPuntuacionRevisionToken(Context context,RevisionToken revisionToken) throws NumberFormatException, SQLException {
	Item itemRevision=Item.find(context, Integer.parseInt(revisionToken.getRevisionId()));
//	int eje1=(Integer.parseInt(itemRevision.getMetadata("oprm.eje1.value")));
//	float media=(Float.parseFloat(itemRevision.getMetadata("oprm.eje2.value"))+Float.parseFloat(itemRevision.getMetadata("oprm.eje3.value"))+Float.parseFloat(itemRevision.getMetadata("oprm.eje4.value")))/3f;
//	if(eje1==1){
//	    return Math.round(media);
//	}else{
//	    return Math.round(media*0.7f);
//	}
	if(itemRevision.getMetadata("oprm.clasificacion")==null || itemRevision.getMetadata("oprm.clasificacion").equalsIgnoreCase("null")){
	    return DEFAULT_REPUTATION;
	}else{
	    return Integer.parseInt(itemRevision.getMetadata("oprm.clasificacion"));
	}
    }

    private void calcularReputacionPublicaciones() throws SQLException, NumberFormatException, IOException, AuthorizeException {
	//ItemIterator itemIterator=Item.findAllNoRevisionesNiJuicios(context);
	ArrayList<String> itemsACalcular=new ArrayList<String>();
	for(int i=0;i<revisionesToken.length;i++){
	    itemsACalcular.add(revisionesToken[i].getHandleRevisado());
	}
	for(int i=0;i<itemsACalcular.size();i++){
	    Item item= (Item) HandleManager.resolveToObject(context, itemsACalcular.get(i));
	    // guardo la reputacion inicial
	    if(item!=null){
		if(item.getMetadata("oprm.reputacion")!=null && !item.getMetadata("oprm.reputacion").equalsIgnoreCase("null")){
		    reputacionPublicacionesInicial.put(item.getID(),Integer.parseInt(item.getMetadata("oprm.reputacion")));
		}
		calcularReputacionPublicacion(item);
	    }
	}
    }

    /**
     * Calcula la reputacion de una publicacion. Tiene que ir antes que el de los autores porque rellena el array de autoridades.
     * @param item
     * @throws NumberFormatException
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private void calcularReputacionPublicacion(Item item) throws NumberFormatException, SQLException, IOException, AuthorizeException {
	ArrayList<RevisionToken> revisiones=RevisionToken.findRevisionsOfHandle(context,item.getHandle());
	
	if(revisiones.size()<REVISIONES_K){
	    reputacionPublicaciones.put(item.getID(), getBestAuthorReputation(item));   
	}else{
	    //No hace nada pero necesitamos cargar los autores en la lista
	    getBestAuthorReputation(item);
	    int denominador=0;
	    int numerador=0;
	    for(int i=0;i<revisiones.size();i++){
		denominador+=reputacionRevisores.get(revisiones.get(i).getEmail());
		numerador+=reputacionRevisores.get(revisiones.get(i).getEmail())*calculaPuntuacionRevisionToken(context,revisiones.get(i));
	    }
	    reputacionPublicaciones.put(item.getID(),numerador/denominador);
	}
    }

    private int calcularReputacionAutores() throws SQLException, AuthorizeException, IOException {
	int changes=0;
	Set<String> autoridades=reputacionAutoridades.keySet();
	Iterator<String> iterator=autoridades.iterator();
	while (iterator.hasNext()){
	    changes+=calcularReputacionAutoridad(iterator.next());
	}
	return changes;
    }

    private int calcularReputacionAutoridad(String autoridad) throws SQLException, AuthorizeException, IOException {
	int valorPrevio=reputacionAutoridades.get(autoridad);
	int valorFinal=0;
	ItemIterator itemIterator=Item.findNoRevisionesNiJuiciosByAuthorityValue(context, "dc", "contributor", "author", autoridad);
	//System.out.println("autoridad:"+autoridad);
	if(itemIterator.hasNext()){
	    int numPublicacionesAutor=0;
	    int sumaReputacionesPublicaciones=0;
	    while(itemIterator.hasNext()){
		Item itemDeAutor=itemIterator.next();
		//System.out.println("itemId:"+itemDeAutor.getID());
		// Si no tiene reputacion calculada no se tiene en cuenta
		if(reputacionPublicaciones.get(itemDeAutor.getID())!=null){
        		numPublicacionesAutor++;
        		int numAutores=itemDeAutor.getMetadata("dc", "contributor", "author", Item.ANY).length;
        		int publicationReputation=reputacionPublicaciones.get(itemDeAutor.getID())==null?DEFAULT_REPUTATION:reputacionPublicaciones.get(itemDeAutor.getID());
        		double gamma=Math.pow(1/(double)numAutores, ALPHA);
        		sumaReputacionesPublicaciones+=(((publicationReputation)*(gamma))+(((1-(gamma))*50)));
		}
	    }
	    reputacionAutoridades.put(autoridad,sumaReputacionesPublicaciones/numPublicacionesAutor);
	    valorFinal=sumaReputacionesPublicaciones/numPublicacionesAutor;
	}else{
	    reputacionAutoridades.put(autoridad,DEFAULT_REPUTATION);
	    valorFinal=DEFAULT_REPUTATION;
	}
	return Math.abs(valorPrevio-valorFinal);
    }

    public static void main(String[] argv) throws Exception{
    		String usage = "[-r] to reset metadata and undexing of \"has revision\".";
    		for(int i=0;i<argv.length;i++){
    		    if(argv[i].contains("-r")){
    			new ReputationCalculator().reindexHasRevision();
    		    }
    		}
	new ReputationCalculator().run();
    }
    private void reindexHasRevision() {
    	System.out.println("Reindexing hasRevision index");
    	Context context=null;

    	try {
    		context= new Context();
    		context.turnOffAuthorisationSystem();;
    		ItemIterator itemIterator = Item.findAll(context);
    		int count=0;
    		while(itemIterator.hasNext()){
    			actualizarHasRevision(context,itemIterator.next());
    			count++;
    			if(count%100==0){
    				context.commit();
    				context.clearCache();
    				System.out.println("Updated:"+count);
    			}
    		}
    		context.commit();
    	} catch (SQLException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} catch (AuthorizeException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}finally{
    		if (context!=null && context.isValid()){
    			context.restoreAuthSystemState();
    			context.abort();
    		}
    	}
    }

    private void actualizarHasRevision(Context context,Item item) throws SQLException, AuthorizeException, IOException {
    	if(item.getHandle()!=null){
    		ArrayList<RevisionToken> revisiones=RevisionToken.findRevisionsOfHandle(context,item.getHandle());
    		String tieneRevisiones=item.getMetadata("oprm.item.hasRevision");
    		boolean haschanges=false;
    		if(tieneRevisiones==null || (!tieneRevisiones.equals(revisiones.size()>0?CompleteStep.REVISED:CompleteStep.NOT_REVISED))){
    			item.clearMetadata("oprm", "item", "hasRevision", Item.ANY);
    			item.addMetadata("oprm", "item", "hasRevision", Item.ANY, revisiones.size()>0?CompleteStep.REVISED:CompleteStep.NOT_REVISED);
    			haschanges=true;
    		}

    		if(haschanges){
    			item.update();
    		}
    	}
    }
//
//    private static int calculateRevisionPublication(Context context,Item item,HashMap<String, Reputation> autoridades, ArrayList<RevisionToken> revisiones) throws SQLException {
//    	int sumaReputacionesRevisor=0;
//    	int sumaReputacionesRevisorPorPuntuacion=0;
//	for(int i=0;i<revisiones.size();i++){
//	    RevisionToken revToken=revisiones.get(i);
//	    Item revision=Item.find(context, Integer.parseInt(revToken.getRevisionId()));
//	    String valoracionEje1=revision.getMetadata("oprm.eje1.value");
//	    String valoracionEje2=revision.getMetadata("oprm.eje2.value");
//	    // En un futuro puede variar la ponderacion entre los ejes
//	    int valoracionD=(Integer.parseInt(valoracionEje1)+Integer.parseInt(valoracionEje2))/2;
//	    String emailRev=revToken.getEmail();
//	    String idRev=null;
//	    String idbbdd = DB.getInstance().select("SELECT id FROM persona where email= '"+emailRev+"'");
//	   
//	    if (StringUtils.isNotBlank(idbbdd)){
//		idRev=idbbdd;
//	    }
//	 
//	    int reputacionRevisor=autoridades.get(idRev).getLastReputation();
//	    
//	    sumaReputacionesRevisor+=reputacionRevisor;
//	    sumaReputacionesRevisorPorPuntuacion+=(reputacionRevisor*valoracionD);
//	}
//	return sumaReputacionesRevisorPorPuntuacion/sumaReputacionesRevisor;
//    }
/**
 * Obtiene la reputacion del mejor autor. Mete las autoridades en el array. Tiene que ir antes que el calculo de la reputacion de los autores
 * @param item
 * @return
 */
    private  int getBestAuthorReputation(Item item) {
	Metadatum[] autores=item.getMetadata("dc", "contributor", "author", Item.ANY);
	int mejorReputacion=0;
	if(autores.length==0){
	    return DEFAULT_REPUTATION;
	}
	
	for(int i=0;i<autores.length;i++){
	    if(StringUtils.isNotEmpty(autores[i].authority)){
		if(reputacionAutoridades.containsKey(autores[i].authority)){
		    int actual=reputacionAutoridades.get(autores[i].authority);
		    if(actual>mejorReputacion){
			mejorReputacion=actual;
		    }
		}else{
		    reputacionAutoridades.put(autores[i].authority, DEFAULT_REPUTATION);
		    mejorReputacion=DEFAULT_REPUTATION;
		}
	    }else{
		// Autor no autoridad, por si aca le metemos reputacion por defecto
		if(DEFAULT_REPUTATION>mejorReputacion){
		    mejorReputacion=DEFAULT_REPUTATION;
		}
	    }
	}
	return mejorReputacion;
    }  

    // Actualiza la reputacion de la publicacion si se añade una nueva revision (aproximado)
    // Actualiza la reputacion de la revision y de la publicacion si se añade una nueva revision (aproximado)
   public static void calculoRapido(Context context,Item itemRevisado, RevisionToken revisionToken) throws IOException, SQLException, AuthorizeException {
       if(revisionToken.getTipo().equals("R")){
	   String reputacionPublicacion=itemRevisado.getMetadata("oprm.reputacion");
       
	   ArrayList<RevisionToken> revisiones=RevisionToken.findRevisionsOfHandle(context, itemRevisado.getHandle());
	
	   // Si es la que genera puntuacion
       	   if(revisiones.size()>=REVISIONES_K && (reputacionPublicacion== null || reputacionPublicacion.equalsIgnoreCase("null"))){
       	       int numerador=0;
       	       int denominador=0;
       	       for(int i=0;i<revisiones.size();i++){
       		   AuthorProfile profile=AuthorProfile.findByEmail(context, revisiones.get(i).getEmail());
       		   int reputacion=DEFAULT_REPUTATION;
       		   if(profile!=null && profile.getMetadata("authorProfile.reputacion.revisor")!=null){
       		       reputacion=Integer.parseInt(profile.getMetadata("authorProfile.reputacion.revisor"));
       		   }
       		   numerador +=reputacion*calculaPuntuacionRevisionToken(context,revisiones.get(i));
       		   denominador+=reputacion;
       	       }
       	       itemRevisado.addMetadata("oprm", "reputacion", null, Item.ANY,""+numerador/denominador);  	       
       	       // Si ya hay opinion
       	   }else if(revisiones.size()<REVISIONES_K){
       	       // Por si acaso quitamos la reputacion
       	       itemRevisado.clearMetadata("oprm", "reputacion", null, Item.ANY);
       	   }else{
       	       // Lo cogemos de la bbdd porque revisionToken esta sin actualizar
       	       RevisionToken ultimaRevision=RevisionToken.find(context, revisionToken.getToken());
       	       AuthorProfile profile=AuthorProfile.findByEmail(context, ultimaRevision.getEmail());
       	       int reputacionRevisor=DEFAULT_REPUTATION;
       	       if(profile!=null && profile.getMetadata("authorProfile.reputacion.revisor")!=null){
       		   try {
		    reputacionRevisor=Integer.parseInt(profile.getMetadata("authorProfile.reputacion.revisor"));
       		   } catch (NumberFormatException e) {/*no pasa nada*/}
       	       }

       	       int numRevisiones=revisiones.size();
       	       int valoracionRevision=calculaPuntuacionRevisionToken(context,ultimaRevision);
       	       itemRevisado.clearMetadata("oprm", "reputacion", null, Item.ANY);
       	       itemRevisado.addMetadata("oprm", "reputacion", null, Item.ANY,""+(((numRevisiones-1)*Integer.parseInt(reputacionPublicacion))+(valoracionRevision*(1-(reputacionRevisor/100))))/numRevisiones);  	       
       	   }
       }else if(revisionToken.getTipo().equals("J")){
	   // Por ahora nada
       }
   }
}
// class ItemReputation{
//     	RevisionToken revisionToken;
//     	Item item;
//	int reputation=ReputationCalculator.DEFAULT_REPUTATION;
//	
//	public RevisionToken getRevisionToken() {
//	    return revisionToken;
//	}
//	public void setRevisionToken(RevisionToken revisionToken) {
//	    this.revisionToken = revisionToken;
//	}
//	public Item getItem() {
//	    return item;
//	}
//	public void setItem(Item item) {
//	    this.item = item;
//	}
//	public int getReputation() {
//	    return reputation;
//	}
//	public void setReputation(int reputation) {
//	    this.reputation = reputation;
//	}
//   }