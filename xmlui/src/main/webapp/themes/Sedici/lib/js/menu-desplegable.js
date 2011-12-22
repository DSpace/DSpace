function llamar_alerta(div, boton, value_ocultar, value_ver){
	if ($('#'+boton).attr('value')==value_ocultar){
		$('#'+boton).attr('value', value_ver);
		$('#'+div).siblings().hide('slow');
	} else {
		$('#'+boton).attr('value', value_ocultar);
		$('#'+div).siblings().show('slow');

	 };
}

$(document).ready(function (){
	$(".artifact-description-community").siblings().css('display', 'none');
}); 
