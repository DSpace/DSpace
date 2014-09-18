/** Requerido pelo google+ **/
window.___gcfg = {lang: 'pt-BR'};

/**
*	Método responsável por compartilhar um item
*	na rede social escolhida.
*
*	@param socialNetwork.
*		A rede social na qual será compartilhado o item.
**/

function shareItem(socialNetwork, title, titlePopUp) {
	
	var	sharedURL 	=	encodeURIComponent(window.location.href);
	var paramsPopUp = 	'width=640, height=340, toolbar=no, scrollbars=yes, status=yes';
	var mainURL 	=	"";
	var titlePopUp  = 	"";
	
	if(title == '')
	{
		title = getMetaValue("DC.title");
	}
	
	switch( socialNetwork ) {
		case 'twitter':
			mainURL 	= "http://twitter.com/home?status=" + title + " - " + sharedURL;
			break;
		
		case 'facebook':
			mainURL 	= "https://www.facebook.com/sharer/sharer.php?u" +  "u=" + sharedURL;
			break;
		
		case 'google':
			mainURL 	= "https://plus.google.com/?" +  "u=" + sharedURL;
			break;
	}
	
	if (window.navigator.appName == "Microsoft Internet Explorer"){
		titlePopUp='';
	}
	
	window.open(mainURL, titlePopUp , paramsPopUp);
}


/**
 * Recupera informações contidas na tag "META" do XHTML renderizado
 * @param name Nome do atributo presente na tag
 */
function getMetaValue (name){
	var metaArrays = document.getElementsByTagName("META");
	var i = 0;
	for (i = 0; i < metaArrays.length; i++){
		if (metaArrays[i].name == name){
			return metaArrays[i].content;
		}
	}
	return null;
}


/**
 * Função do google+
 */
(function() {
  var po = document.createElement('script'); po.type = 'text/javascript'; po.async = true;
  po.src = 'https://apis.google.com/js/platform.js';
  var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);
})();

/**
 * Função de comentário do facebook
 * @param d
 * @param s
 * @param id
 */
(function(d, s, id) {
	  var js, fjs = d.getElementsByTagName(s)[0];
	  if (d.getElementById(id)) return;
	  js = d.createElement(s); js.id = id;
	  js.src = "//connect.facebook.net/pt_BR/all.js#xfbml=1";
	  fjs.parentNode.insertBefore(js, fjs);
	}(document, 'script', 'facebook-jssdk'));

