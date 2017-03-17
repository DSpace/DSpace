/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
var labelType, useGradients, nativeTextSupport, animate, rgraph, arrayjson, ajaxurlprofile, authority, winW, winH
var othernetwork = new Array();
var bezierRatioSettings = new Array(); 

(function() {
	var ua = navigator.userAgent, iStuff = ua.match(/iPhone/i)
			|| ua.match(/iPad/i), typeOfCanvas = typeof HTMLCanvasElement, nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'), textSupport = nativeCanvasSupport
			&& (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
	// I'm setting this based on the fact that ExCanvas provides text support
	// for IE
	// and that as of today iPhone/iPad current text support is lame
	labelType = (!nativeCanvasSupport || (textSupport && !iStuff)) ? 'Native'
			: 'HTML';
	nativeTextSupport = labelType == 'Native';
	useGradients = nativeCanvasSupport;
	animate = !(iStuff || !nativeCanvasSupport);
})();

var Log = {
	elem : false,
	write : function(text) {
		if (!this.elem)
			this.elem = document.getElementById('logcontent');
		this.elem.innerHTML = text;		
	}
};

function init(graph, rp, network, typo) {	
	
	authority = rp;

	$jit.RGraph.Plot.NodeTypes
			.implement({
				'customnode' : {
					'render' : function(node, canvas) {

						var pos = node.pos.getc(true), dim = node
								.getData('dim');
//						var aaa = jQuery("#amount").val();
//						if (node._depth <= aaa) {

							for ( var index = 0; index < eventTypeDepth.length; index++) {
								if (node.data.type == eventTypeDepth[index]) {
									var aaaa = jQuery(
											"#amount" + eventTypeDepth[index])
											.val();
									if (aaaa == undefined
											|| node._depth <= aaaa) {
										
//											var ctx = canvas.getCtx();
//											ctx.strokeStyle = node.data.color;
//											var color = colorsNodes[node.data.type];
//											ctx.strokeStyle = color;
											node.data.$color = "#000000";
											this.nodeHelper.circle.render(node.data.modeStyle, pos, dim, canvas);
									}
								}
							}

//						}

					}
				}
			});

	$jit.RGraph.Plot.EdgeTypes
			.implement({
				'customline' : {
					'render' : function(adj, canvas) {
						var from = adj.nodeFrom.pos.getc(true), to = adj.nodeTo.pos
								.getc(true);
//						var aaa = jQuery("#amount").val();						
//						if (adj.nodeTo._depth <= aaa) {

							for ( var index = 0; index < eventTypeDepth.length; index++) {
								if (adj.nodeTo.data.type == eventTypeDepth[index]) {
									var aaaa = jQuery(
											"#amount" + eventTypeDepth[index])
											.val();
									if (aaaa == undefined
											|| adj.nodeTo._depth <= aaaa) {

										if (adj.nodeFrom._depth < adj.nodeTo._depth
												&& adj.nodeFrom._depth == 0 || adj.nodeTo._depth == 0) {
											
											this.edgeHelper.line.render(
													from, to, canvas);												 

											if (othernetwork != undefined
													&& othernetwork != null) {
												for (subIndex in othernetwork) {
													
													if(othernetwork[subIndex].id == adj.nodeTo.id && othernetwork[subIndex].fromid == authority) {
											
														
														if(j.inArray(othernetwork[subIndex].type, bezierRatioSettings)==-1) {

															bezierRatioSettings.push(othernetwork[subIndex].type);
																													
														}
														
														var color = othernetwork[subIndex].color;
														
														
														if (color != null) {
															
															var radialX = 100;
															var radialY = -100;
															if (from.x < to.x) {
																radialX = -100;
																if (from.y < to.y) {
																	radialY = 100;
																}
															}

															var ctx = canvas
																	.getCtx();
															ctx.beginPath();

															var cpx = from.x
																	+ radialX;
															var cpy = from.y
																	+ radialY;
															var currentX = from.x;
															var currentY = from.y;
															var x = to.x;
															var y = to.y;
//															var ratio = Math
//																	.random();
															var ratio = 0.1 * (j.inArray(othernetwork[subIndex].type, bezierRatioSettings)+1);
															// if the Bezier is
															// approximating an
															// elliptic arc
															// with best fitting
															var cp1x = currentX
																	+ (cpx - currentX)
																	* ratio;
															var cp1y = currentY
																	+ (cpy - currentY)
																	* ratio;
															var cp2x = cp1x
																	+ (x - currentX)
																	* (1 - ratio);
															var cp2y = cp1y
																	+ (y - currentY)
																	* (1 - ratio);

															// and now call cubic
															// Bezier curve
															// to function
															ctx.bezierCurveTo(
																	currentX,
																	currentY, cp2x,
																	cp2y, x, y);
															ctx.strokeStyle = color;
															ctx.stroke();
													

													}
													}
												}
											}
																						
											

											
										} else {

//											 var radialX = 100;
//											 var radialY = -100;
//											 if(from.x < to.x) {
//											 radialX = -100;
//											 if(from.y<to.y) {
//											 radialY = 100;
//											 }
//											 }
//																				
//																							
//											 var ctx = canvas.getCtx();
//											 ctx.beginPath();
//																						
//											 var cpx = from.x + radialX;
//											 var cpy = from.y + radialY;
//											 var currentX = from.x;
//											 var currentY = from.y;
//											 var x = to.x;
//											 var y = to.y;
//											 var ratio = 2.0 / 3.0; //
//											 0.5522847498307933984022516322796
//											 if the Bezier is
//											 approximating an elliptic arc
//											 with best fitting
//											 var cp1x = currentX + (cpx -
//											 currentX) * ratio;
//											 var cp1y = currentY + (cpy -
//											 currentY) * ratio;
//											 var cp2x = cp1x + (x -
//											 currentX) * (1 - ratio);
//											 var cp2y = cp1y + (y -
//											 currentY) * (1 - ratio);
//																						  
//											 // and now call cubic Bezier
//											 curve to function
//											 ctx.bezierCurveTo( cp1x,
//											 cp1y, cp2x, cp2y, x, y );
//											 ctx.stroke();
											if (adj.nodeTo._depth <= aaaa && adj.nodeFrom._depth <= aaaa) {

												var ctx = canvas.getCtx();
												ctx.strokeStyle = 'gray';
												ctx.globalAlpha = 0.2;
												this.edgeHelper.line.render(
														from, to, canvas);
											}

										}

									}

//								}

							}

						}

					},
					'contains' : function(adj, pos) {
						var from = adj.nodeFrom.pos.getc(true), to = adj.nodeTo.pos
								.getc(true);
						return this.edgeHelper.line.contains(from, to, pos,
								this.edge.epsilon);
					}

				}
			});

	
	
	winW = 630, winH = 460;
	if (document.body && document.body.offsetWidth) {
	 winW = document.body.offsetWidth;
	 winH = document.body.offsetHeight;
	}
	if (document.compatMode=='CSS1Compat' &&
	    document.documentElement &&
	    document.documentElement.offsetWidth ) {
	 winW = document.documentElement.offsetWidth;
	 winH = document.documentElement.offsetHeight;
	}
	if (window.innerWidth && window.innerHeight) {
	 winW = window.innerWidth;
	 winH = window.innerHeight;
	}
	
	// init RGraph
	rgraph = new $jit.RGraph(
			{
				// Where to append the visualization
				injectInto : 'infovis',
				
				width: winW,  
				height:winH,  
				// Optional: create a background canvas that plots
				// concentric circles.
//				background : {
//					CanvasStyles : {
//						strokeStyle : '#0479AC'
//					}
//				},

				levelDistance : 200,
				
				// Add navigation capabilities:
				// zooming by scrolling and panning.
				Navigation : {
					enable : true,
					panning : true,
					zooming : 10
				},

				// Set Node and Edge styles.
				Node : {
					color : colorsNodes[network], // default color - must be the same with
					// that injected on json first relation
					// (coauthors)
					overridable : true,
					type : "customnode",
					dim : 3					

				},

				Edge : {
					overridable : true,
					color : colorsEdges[network], // default color - must be the same with
					// that injected on json first relation
					// (coauthors)
					lineWidth : 1.5,
					epsilon : 7,
					type : "customline"
				},

				// Add tooltips
				Tips : {
					enable : true,
					onShow : function(tip, node) {
						rgraph.refresh();
						foundParent(node, rgraph);
						var name = node.id;
						if(name.match("^rp[0-9]*")) {
							var html = tmpl("tmpl-networkimage", {id: node.id, name: node.name});
							tip.innerHTML = html;
						}
						else {
							var html = "<div class=\"tip-title\"><span>" + node.name	+ "</span></div>";
							tip.innerHTML = html;
						}
	
					}
				},
	
				
				 //Add events for Dragging and dropping nodes  
//				   Events: {  
//				     enable: true,  
//				     type: 'auto',  
//				     onMouseEnter: function(node, eventInfo, e){  
//				       rgraph.canvas.getElement().style.cursor = 'move';  
//				     },  
//				     onMouseLeave: function(node, eventInfo, e){  
//				       rgraph.canvas.getElement().style.cursor = '';  
//				     },  
//				     onDragMove: function(node, eventInfo, e){  
//				       var pos = eventInfo.getPos();  
//				       node.pos.setc(pos.x, pos.y);  
//				       rgraph.plot();  
//				     }, 
//				     onDragEnd: function(node, eventInfo, e){  
//				       rgraph.compute('end');  
//				       rgraph.fx.animate( {  
//				         modes: [  
//				           'linear'  
//				         ],  
//				         duration: 700,
//				         transition: $jit.Trans.Elastic.easeOut  
//				       });  
//				     },  
//				     //touch events  
//				     onTouchStart: function(node, eventInfo, e) {  
//				       //stop the default event  
//				       $jit.util.event.stop(e);  
//				     },  
//				     onTouchMove: function(node, eventInfo, e){  
//				       //stop the default event  
//				       $jit.util.event.stop(e);  
//				       var pos = eventInfo.getPos();  
//				       node.pos.setc(pos.x, pos.y);  
//				       rgraph.plot();  
//				     }
//				     onTouchEnd: function(node, eventInfo, e){  
//				       //stop the default event  
//				       $jit.util.event.stop(e);  
//				       rgraph.compute('end');  
//				       rgraph.fx.animate( {  
//				         modes: [  
//				           'linear'  
//				         ],  
//				         duration: 700,  
//				         transition: $jit.Trans.Elastic.easeOut  
//				       });  
//				     }  
//				   },  

				onBeforeCompute : function(node) {					
					Log.write("centering " + node.name + "...");
					// Add the relation list in the right column.
					// This list is taken from the data property of each JSON
					// node.
					// $jit.id('inner-details').innerHTML = node.data.relation;
				},

		// Add the name of the node in the correponding label
		// and a click handler to move the graph.
		// This method is called once, on label creation.
				onCreateLabel : function(domElement, node) {
					domElement.innerHTML = node.name;
					
//					domElement.ondblclick = function() {						
//						j("#log").dialog("open");						
//						if (node.id != rgraph.root) {
//							if (authority == node.id) {
//								j("#profiletargetminimized").remove();
//								j("#profiletargetinternal").remove();
//								j("#relationdiv").remove();
//								j("#profiletargettwiceminimized").remove();
//								j("#relationdivtwice").remove();
//								j("#profileminimized").hide();
//								j("#profilefocus").show();
//
//							} else {
//								j("#profiletargetminimized").remove();
//								j("#profiletargetinternal").remove();
//								j("#relationdiv").remove();
//								j("#profiletargettwiceminimized").remove();
//								j("#relationdivtwice").remove();
//							}
//
//							internalOnClk(authority, node.id, node._depth,
//									authority, typo);
//							rgraph.onClick(node.id, {
//								onComplete : function() {
//									Log.write("done");
//									j("#log").dialog("close");
//								}
//							});
//						} else {
//							j("#log").dialog("close");
//						}
//					};

					domElement.onclick = function() {					
						j("#loadingprofile").show();
						j("#profiletarget").hide();
						//j('#network-pane').layout().close("south");						
						internalOnClk(authority, node.id, node._depth,
								rgraph.root, typo);						
						j("#loadingprofile").hide();
						j("#profiletarget").show();
					};

				},

				// Change some label dom properties.
				// This method is called each time a label is plotted.
				onPlaceLabel : function(domElement, node) {
					var style = domElement.style;
					style.display = '';
					style.cursor = 'pointer';
//					var aaa = jQuery("#amount").val();
//
//					if (node._depth <= aaa) {

						for ( var index = 0; index < eventTypeDepth.length; index++) {
							if (node.data.type == eventTypeDepth[index]) {
								var elem = "#amount" + eventTypeDepth[index];
								var aaaa = jQuery(elem).val();
								if (aaaa == undefined || node._depth <= aaaa) {
									// nothing
								} else {
									style.display = "none";
								}
							}
						}

//					} else {
//						style.display = "none";
//					}
					var left = parseInt(style.left);
					var w = domElement.offsetWidth;
					style.left = (left - w / 2) + 'px';
				}

			});
	// load JSON data
	rgraph.loadJSON(graph);

	// trigger small animation
	rgraph.graph.eachNode(function(n) {
		var pos = n.getPos();
		pos.setc(-200, -200);
	});
	rgraph.compute('end');
	rgraph.fx.animate({
		modes : [ 'linear' ],
		duration : 4000,
		hideLabels : true,
		fps: 10
	});
    


//	rgraph.canvas.translate(-(rgraph.canvas.getSize().width)/3, -(rgraph.canvas.getSize().height)/3);
	rgraph.canvas.translate(-(rgraph.canvas.getSize().width)/6, 0);
	// end
	// append information about the root relations in the right column
	// $jit.id('inner-details').innerHTML =
	// rgraph.graph.getNode(rgraph.root).data.relation;
	return graph;
}

function addLocalJson(jj) {
	var type = "";
	for (index in jj) {
		type = index;
		break;
	}
	if (arrayjson == undefined) {
		reloadLocalJson(jj);
	} else {
		var push = true;
		for (index in arrayjson) {
			for (prop in arrayjson[index]) {
				if (prop == type) {
					push = false;
					break;
				}
			}
		}

		if (push) {
			arrayjson.push(jj);
		}

	}

}

function removeLocalJson(jj) {
	var firstLoad = true;
	var newarrayjson = new Array();
	for (i=0; i<arrayjson.length; i++) {		
			var jjjj = arrayjson[i];
			for(prop in jjjj) {
			if (jj != prop) {
				if (firstLoad) {
					rgraph.loadJSON(jjjj[prop]);
					firstLoad = false;
				} else {
					rgraph.op.sum(jjjj[prop], {
						type: 'fade:con',  
						duration: 1500  
					});
				}
				newarrayjson.push(jjjj);
			}
			}
	}
	
	arrayjson = newarrayjson;
	rgraph.refresh();
}

function reloadLocalJson(jj) {
	arrayjson = new Array();
	arrayjson.push(jj);
}

function foundParent(node, rgraph) {
	var canvas = rgraph.canvas;
	var plot = rgraph.fx;
	var parents = node.getParents();
	if(parents!=null && parents[0]!=null) {
				
		for(var i = 0; i<parents.length; i++) {
			var from = node.pos.getc(true);
			var to = parents[i].pos.getc(true);
			var ctx = canvas.getCtx();
			ctx.strokeStyle = 'black';
			ctx.beginPath();
			ctx.moveTo(to.x, to.y);
			ctx.lineTo(from.x, from.y);
			ctx.stroke();					
			foundParent(parents[i],rgraph);		
		}
	}
}

function internalOnClk(authority, nodeId, nodeDepth, rootId, typo) {
	

	if(authority!=nodeId) {
	var profilediv = "profiletarget";
	var profilefocus = "profilefocus";
	var profilemin = "profileminimized";
	if(authority!=rootId) {
		profilediv = "profiletargettwice";
		profilefocus = "profiletargetinternal";
		profilemin = "profiletargetminimized";
	}		
	
	var parameterId = nodeId;
	var parameterDepth = nodeDepth;
	j.ajax({
		async: false,
		url : ajaxurlprofile,
		data : {
			"target" : parameterId,
			"depth" : parameterDepth,
			"root": rootId,
			"typo": typo
		},
		success : function(data) {			
			j("#"+profilediv).html(data);	
				
		},
		complete : function(data) {
			//j("#"+profilemin).show();
			//j("#"+profilefocus).hide();
			var visible = j('#ui-no-layout-south').is(":visible");
			
			if(visible) {
				var a = j('#ui-no-layout-center').innerHeight();
				var b = j('#network-pane').height();
				var c = j('#ui-no-layout-south').innerHeight();			    
			    var d = j("#"+profilediv).innerHeight();	
			    if((b-a-c-d) > 0) {
			    	j('#separator-ui').css("height",b-a-c-d);		    	
			    }
			    else {
			    	j('#separator-ui').css("height","auto");
			    }
			}
			else {
				
				var b = j('#network-pane').height();
				var d = j("#"+profilediv).innerHeight();
				var a = j('#ui-no-layout-center').innerHeight();
				if((b-d-a)<0) {
					j('#open-no-layout-south-internal').css("top",b+d);	
				}			    
			    
			}
			
			
		},
		error : function(data) {			
			Log.write(data.statusText);			
		}
	});
	
	
	}
	
}
