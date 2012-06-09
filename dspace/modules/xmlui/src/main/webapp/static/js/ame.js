
function AMESuggestSetup(itemID, treeContainerElem, treeElem, field, buttonID)
{
    	var form = document.getElementById('org_datadryad_app_xmlui_aspect_ame_AMEForm_div_ame-item');

	var container = document.getElementById(treeContainerElem);
	var tree = document.getElementById(treeElem);
	container.style.display = 'block';
	jQuery(tree).dynatree("destroy");
	jQuery(tree).dynatree({
	      checkbox: true,
	      selectMode: 2,
	      minExpandLevel: 10,
	      initAjax: {
	          url: "../JSON/ame/suggest/" + itemID + "/" + field
	      },
              onCustomRender: function(node) {
                var html = "<span class='dynatree-title'><a class='dynatree-title' title='" + node.data.tooltip + "' target='_hive' href='" + node.data.url + "'>" + node.data.title + "</a></span>";

                if (node.data.origin) {
                    html +=  "<a title=\"Source vocabulary\">[" + node.data.origin + "]</a>";
                }

                html += "<br><span class='dynatree-path'>";

                if (node.data.broaders) {
                   html += "> ";
                   for (i=0; i < node.data.broaders.length; i++) {
                      if (i > 0) 
                          html += "; ";
                      var parent = node.data.broaders[i];
                      html +=  "<a title='Broader term' target='_hive' href='" + parent.url + "'>" + parent.title + "</a>";
                   }
                }

		html += "</span>";

                if (node.data.path)
                   return html;
              },
	      onSelect: function(select, node) {
	        // Display list of selected nodes
	        var selNodes = node.tree.getSelectedNodes();
	        // convert to title/key array
	        var selKeys = jQuery.map(selNodes, function(node){
	             return "[" + node.data.key + "]: '" + node.data.title + "'";
	        });
	      },
	      onClick: function(node, event) {
                  if (event.target.className == "dynatree-title") {
	            window.open(node.data.url);
                  }
                  else if (event.target.className != "dynatree-checkbox") {
	            window.open(event.target);
                  }
	      },
	      onKeydown: function(node, event) {
	        if( event.which == 32 ) {
	          node.toggleSelect();
	          return false;
	        }
	      },
	      // The following options are only required, if we have more than one tree on one page:
	      cookieId: "dynatree-Cb2",
	      idPrefix: "dynatree-Cb2-",
              classNames: {
                  nodeIcon: "custom-node-icon"
              }

	    });

        var addButton = document.getElementById(buttonID);
        jQuery(addButton).button();
        jQuery(addButton).click(
            function() { 
	        var selected = jQuery(tree).dynatree("getSelectedNodes");
                for (i=0; i<selected.length; i++) {
                   var el = document.createElement("input");
                   el.type = "hidden";
                   el.name = "add_" + field + "_" + i;
                   el.value = selected[i].data.title;
                   form.appendChild(el);

                   var confidence = document.createElement("input");
                   confidence.type = "hidden";
                   confidence.name = "add_" + i + "_confidence";
                   confidence.value = "ACCEPTED";
                   form.appendChild(confidence);

                   var authority = document.createElement("input");
                   authority.type = "hidden";
                   authority.name = "add_" + i + "_authority";
                   authority.value = selected[i].data.url;
                   form.appendChild(authority);
                }
                var submit = document.createElement("input");
                submit.type = "hidden";
                submit.name = "submit_" + field + "_add";
                submit.value = "Add";
                form.appendChild(submit);
                form.submit();
            } 
        );
        jQuery('.ame-item-remove').attr("title", "Removes the selected entry.");
  }
