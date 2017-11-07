function llamar_alerta(div, boton, value_ocultar, value_ver){
	if ($('#'+boton).attr('value')==value_ocultar){
		$('#'+boton).attr('value', value_ver);
		$('#'+div).siblings("ul").hide("blind", { direction: "vertical" }, 1500);
		$('#'+boton).text('+');
	} else {
		$('#'+boton).attr('value', value_ocultar);
		$('#'+div).siblings("ul").show("blind", { direction: "vertical" }, 1500);
		$('#'+boton).text('-');

	 };
}

/*$(document).ready(function (){
	$(".artifact-description-community").siblings("ul").css('display', 'none');
}); */
