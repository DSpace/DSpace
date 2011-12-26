function llamar_alerta(div, boton, value_ocultar, value_ver){
	if ($('#'+boton).attr('value')==value_ocultar){
		$('#'+boton).attr('value', value_ver);
		$('#'+div).siblings("ul").hide('slow');
		$('#'+boton).text('+');
	} else {
		$('#'+boton).attr('value', value_ocultar);
		$('#'+div).siblings("ul").show('slow');
		$('#'+boton).text('-');

	 };
}

/*$(document).ready(function (){
	$(".artifact-description-community").siblings("ul").css('display', 'none');
}); */
