<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/
	
	Graphics license:

	    Copyright (c) 2013, Olly Smith
		All rights reserved.

		Redistribution and use in source and binary forms, with or without
		modification, are permitted provided that the following conditions are met:

		1. Redistributions of source code must retain the above copyright notice, this
		list of conditions and the following disclaimer.
		2. Redistributions in binary form must reproduce the above copyright notice,
		this list of conditions and the following disclaimer in the documentation
		and/or other materials provided with the distribution.

		THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
		ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
		WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
		DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
		ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
		(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
		LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
		ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
		(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
		SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

--%>
<%--

  - Renders a page containing a statistical summary of the repository usage
  --%>
 <link rel="stylesheet" href="//cdnjs.cloudflare.com/ajax/libs/morris.js/0.5.1/morris.css">
 <script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script>
 <script src="//cdnjs.cloudflare.com/ajax/libs/raphael/2.1.0/raphael-min.js"></script>
 <script src="//cdnjs.cloudflare.com/ajax/libs/morris.js/0.5.1/morris.min.js"></script>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

	<%
	    String navbar = (String) request.getAttribute("navbar");
	%>


<div class="container">
	<div class="row" style="background-color: #fff; margin-right: -6%; margin-left: -6%; margin-bottom: -2%; padding: 2%">
		<dspace:layout style="submission" navbar="<%=  navbar %>" titlekey="jsp.statistics.no-report.title">
		
		<div class="row"> 
		      <div class="col-6 col-md-6" >
		    	<h2 style="color: #401410">Estadísticas del Repositorio</h2>  
		      </div>
    		  <div class="col-6 col-md-6" style="text-align: right; padding-right: 1%;">
    		  	<p></p>
    			<h4><a href="#">25 de feb. 2019 - 03 mar 2019</a></h4>	  	
    		  </div>
		 </div> 
		 <div class="row"> 
		 	<div class="col-6 col-md-6" >
		    	<h3 style="color: #401410;"><b>Visión General</b></h3>  	
		    </div>
    		<div class="col-md-6" style="text-align: right; padding-right: 1%;">
    		</div>
		 </div> 

		 <div class="row"> 
		 	<div class="col-6 col-md-6" >
		    	<h4 style="color: #401410;">Usuarios</h4>  	
		    </div>
    		<div class="col-6 col-md-6" style="text-align: right; padding-right: 1%;">
    		</div>
		 </div> 

		<div class="row"> 
			<div id="usersAreaChart" style="height: 250px;"></div>
		</div>

		<hr>

		<div class="row"> 
		 	<div class="col-6 col-md-6" >
		    	<div class="row">
				    <div class="col-6 col-sm-4">
				    	<div style="padding-left: 17%"><p>Usuarios</p><h4>10,346</h4></div>
						<div id="AreaChart1" style="height: 100px; width: 100%; margin-top: -10%;"></div>	    	
				    </div>
				    <div class="col-6 col-sm-4">
				    	<div style="padding-left: 17%"><p>Documentos archivados</p><h4>325</h4></div>
						<div id="AreaChart2" style="height: 100px; width: 100%; margin-top: -10%;"></div>	
				    </div>
				    <div class="col-6 col-sm-4">
				    	<div style="padding-left: 17%"><p>Lista de documentos</p><h4>2,304</h4></div>
						<div id="AreaChart3" style="height: 100px; width: 100%; margin-top: -10%;"></div>	
				    </div>
				</div> 

				<div class="row">
				    <div class="col-6 col-sm-4">
				    	<div style="padding-left: 17%"><p>Vista de colecciones</p><h4>2,601</h4></div>
						<div id="AreaChart4" style="height: 100px; width: 100%; margin-top: -10%;"></div>	    	
				    </div>
				    <div class="col-6 col-sm-4">
				    	<div style="padding-left: 17%"><p>Vista de comunidades</p><h4>6,325</h4></div>
						<div id="AreaChart5" style="height: 100px; width: 100%; margin-top: -10%;"></div>	
				    </div>
				    <div class="col-6 col-sm-4">
				    	<div style="padding-left: 17%"><p>Log in de usuarios</p><h4>1,283</h4></div>
						<div id="AreaChart6" style="height: 100px; width: 100%; margin-top: -10%;"></div>	
				    </div>
				</div> 	
		    </div>
    		<div class="col-6 col-md-6" style="text-align: left; padding-right: 1%;">
    			<div class="row">
	    			<h3 style="color: #401410; text-align: center;">Visitantes por estado</h3> 
	    			<div id="Visitasdonut" style="height: 200px;"></div>	
    			</div>
    		</div>
		 </div> 
		
	
		

		</dspace:layout>
	</div>
</div>

<script type="text/javascript">
	new Morris.Area({
	  // ID of the element in which to draw the chart.
	  element: 'usersAreaChart',
	  // Chart data records -- each entry in this array corresponds to a point on
	  // the chart.
	  data: [
	    { year: '...', value: 10 },
	    { year: '26 Feb.', value: 15 },
	    { year: '27 Feb.', value: 12 },
	    { year: '28 Feb.', value: 17 },
	    { year: '01 Mar.', value: 15 },
	    { year: '02 Mar.', value: 17 },
	    { year: '03 Mar.', value: 14 }
	  ],
	  // The name of the data record attribute that contains x-values.
	  xkey: 'year',
	  // A list of names of data record attributes that contain y-values.
	  ykeys: ['value'],
	  // Labels for the ykeys -- will be displayed when you hover over the
	  // chart.
	  labels: ['Usuarios'],

	  resize: true, behaveLikeLine: false, hideHover: 'auto', parseTime: false, smooth: false, gridTextColor: "#000000", fillOpacity: 0.2, lineColors: ['#410401'], pointSize: 6, pointStrokeColors: '#410401'
	});

	new Morris.Area({
	  // ID of the element in which to draw the chart.
	  element: 'AreaChart1',
	  // Chart data records -- each entry in this array corresponds to a point on
	  // the chart.
	  data: [	   
	    { year: '26 Feb.', value: 10 },
	    { year: '27 Feb.', value: 5 },
	    { year: '28 Feb.', value: 7 },
	    { year: '01 Mar.', value: 6 },
	    { year: '02 Mar.', value: 5 }
	  ],
	  // The name of the data record attribute that contains x-values.
	  xkey: 'year',
	  // A list of names of data record attributes that contain y-values.
	  ykeys: ['value'],
	  // Labels for the ykeys -- will be displayed when you hover over the
	  // chart.
	  labels: ['Usuarios'],

	  resize: true, behaveLikeLine: false, hideHover: 'always', parseTime: false, gridTextColor: "#fff", fillOpacity: 0.2, lineColors: ['#410401'], pointSize: 0, pointStrokeColors: '#410401'
	});

	new Morris.Area({
	  // ID of the element in which to draw the chart.
	  element: 'AreaChart2',
	  // Chart data records -- each entry in this array corresponds to a point on
	  // the chart.
	  data: [	   
	    { year: '26 Feb.', value: 7 },
	    { year: '27 Feb.', value: 10 },
	    { year: '28 Feb.', value: 6 },
	    { year: '01 Mar.', value: 3 },
	    { year: '02 Mar.', value: 5 }
	  ],
	  // The name of the data record attribute that contains x-values.
	  xkey: 'year',
	  // A list of names of data record attributes that contain y-values.
	  ykeys: ['value'],
	  // Labels for the ykeys -- will be displayed when you hover over the
	  // chart.
	  labels: ['Usuarios'],

	  resize: true, behaveLikeLine: false, hideHover: 'always', parseTime: false, gridTextColor: "#fff", fillOpacity: 0.2, lineColors: ['#410401'], pointSize: 0, pointStrokeColors: '#410401'
	});

	new Morris.Area({
	  // ID of the element in which to draw the chart.
	  element: 'AreaChart3',
	  // Chart data records -- each entry in this array corresponds to a point on
	  // the chart.
	  data: [	   
	    { year: '26 Feb.', value: 4 },
	    { year: '27 Feb.', value: 8 },
	    { year: '28 Feb.', value: 3 },
	    { year: '01 Mar.', value: 7 },
	    { year: '02 Mar.', value: 10 }
	  ],
	  // The name of the data record attribute that contains x-values.
	  xkey: 'year',
	  // A list of names of data record attributes that contain y-values.
	  ykeys: ['value'],
	  // Labels for the ykeys -- will be displayed when you hover over the
	  // chart.
	  labels: ['Usuarios'],

	  resize: true, behaveLikeLine: false, hideHover: 'always', parseTime: false, gridTextColor: "#fff", fillOpacity: 0.2, lineColors: ['#410401'], pointSize: 0, pointStrokeColors: '#410401'
	});

	new Morris.Area({
	  // ID of the element in which to draw the chart.
	  element: 'AreaChart4',
	  // Chart data records -- each entry in this array corresponds to a point on
	  // the chart.
	  data: [	   
	    { year: '26 Feb.', value: 3 },
	    { year: '27 Feb.', value: 5 },
	    { year: '28 Feb.', value: 7 },
	    { year: '01 Mar.', value: 10 },
	    { year: '02 Mar.', value: 5 }
	  ],
	  // The name of the data record attribute that contains x-values.
	  xkey: 'year',
	  // A list of names of data record attributes that contain y-values.
	  ykeys: ['value'],
	  // Labels for the ykeys -- will be displayed when you hover over the
	  // chart.
	  labels: ['Usuarios'],

	  resize: true, behaveLikeLine: false, hideHover: 'always', parseTime: false, gridTextColor: "#fff", fillOpacity: 0.2, lineColors: ['#410401'], pointSize: 0, pointStrokeColors: '#410401'
	});

	new Morris.Area({
	  // ID of the element in which to draw the chart.
	  element: 'AreaChart5',
	  // Chart data records -- each entry in this array corresponds to a point on
	  // the chart.
	  data: [	   
	    { year: '26 Feb.', value: 6 },
	    { year: '27 Feb.', value: 3 },
	    { year: '28 Feb.', value: 7 },
	    { year: '01 Mar.', value: 8 },
	    { year: '02 Mar.', value: 5 }
	  ],
	  // The name of the data record attribute that contains x-values.
	  xkey: 'year',
	  // A list of names of data record attributes that contain y-values.
	  ykeys: ['value'],
	  // Labels for the ykeys -- will be displayed when you hover over the
	  // chart.
	  labels: ['Usuarios'],

	  resize: true, behaveLikeLine: false, hideHover: 'always', parseTime: false, gridTextColor: "#fff", fillOpacity: 0.2, lineColors: ['#410401'], pointSize: 0, pointStrokeColors: '#410401'
	});

	new Morris.Area({
	  // ID of the element in which to draw the chart.
	  element: 'AreaChart6',
	  // Chart data records -- each entry in this array corresponds to a point on
	  // the chart.
	  data: [	   
	    { year: '26 Feb.', value: 5 },
	    { year: '27 Feb.', value: 2 },
	    { year: '28 Feb.', value: 10 },
	    { year: '01 Mar.', value: 4 },
	    { year: '02 Mar.', value: 3 }
	  ],
	  // The name of the data record attribute that contains x-values.
	  xkey: 'year',
	  // A list of names of data record attributes that contain y-values.
	  ykeys: ['value'],
	  // Labels for the ykeys -- will be displayed when you hover over the
	  // chart.
	  labels: ['Usuarios'],

	  resize: true, behaveLikeLine: false, hideHover: 'always', parseTime: false, gridTextColor: "#fff", fillOpacity: 0.2, lineColors: ['#410401'], pointSize: 0, pointStrokeColors: '#410401'
	});

	Morris.Donut({
	  element: 'Visitasdonut',
	  data: [
	    {label: "Puebla", value: 10000},
	    {label: "Oaxaca", value: 35000},
	    {label: "Jalisco", value: 15000},
	    {label: "Querétaro", value: 20000},
	    {label: "Chiapas", value: 20000},
	  ],
	  resize: true,
	  colors: ['#bc514a', '#88615e', '#70302c', '#bc8683', '#3d1a18']
});

</script>

<!--
Para el jueves
descripcion de la tesis, una cuartilla arial 10 espacioado de 1
tabla (resumen del protocolo)
presentación y en la última diapositiva con los objetivos con el procentaje de avance
un calendario de entregas (30 de abril, entrega del producto)

escrito
Experimental 
Producto: aportación, estudios


historiador que me ayude a definir la importancia de los personajes y así poder justificar porqué los incluí. O con un libro. 
Averiguar culaes son los hitos más importantes y de ahí partir para elegir a los personajes. 

josé luis pintor del mural de los arcos del palacio. Basarme en el mural 
Manuel barragán- mureh-->