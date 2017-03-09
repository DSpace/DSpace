/** Metadata Tree Browser Aspect, display the overview listing of sets wich an expandable list **/
jQuery(document).ready(function() {

   // Add toggle controls to all elements which have children.
   jQuery("#edu_tamu_metadatatreebrowser_BrowseOverview_list_overview-list li").each(function() {
      if (jQuery(this).next().has('ul').length > 0) {
         jQuery(this).prepend("<a class=\"toggle btn btn-default btn-sm toggler collapsed\" href=\"javascript: void(0);\">"+
                                 "<i aria-hidden=\"true\" class=\"glyphicon glyphicon-plus closed-icon\"></i>"+
                              "</a> ");
      }
   });
   
   // Toggle the state of a control, showing the children and switching the display character used.
   jQuery("#edu_tamu_metadatatreebrowser_BrowseOverview_list_overview-list .toggle").click(function() {
      $toggler = jQuery(this).children("i.glyphicon");
      if ($toggler.hasClass("glyphicon-minus")) {
         // Collapse Children
         $toggler.removeClass("glyphicon-minus open-icon").addClass("glyphicon-plus closed-icon");
         
         // Variable speed based upon how many children.
         count = jQuery(this).parent().next().has('li ul li').length;
         jQuery(this).parent().next().has('li').slideUp(count * 200 + 200);
         

      } else {
         // Expand Children
         $toggler.removeClass("glyphicon-plus closed-icon").addClass("glyphicon-minus open-icon");
       
         count = jQuery(this).parent().next().has('li ul li').length;
         jQuery(this).parent().next().has('li').slideDown(count * 200 + 200);
      }
   });
   
   // preset all toggles to be closed. This will only show the first order list.
   jQuery("#edu_tamu_metadatatreebrowser_BrowseOverview_list_overview-list a.toggle").each(function () {
      jQuery(this).parent().next().has('li').hide();     
   });
   
   // If there is only one top level element, auto expand it.
   if (jQuery("ul#edu_tamu_metadatatreebrowser_BrowseOverview_list_overview-list.root-list").children().length <= 2) {
      jQuery("ul#edu_tamu_metadatatreebrowser_BrowseOverview_list_overview-list.root-list").children().last().show();
      jQuery("ul#edu_tamu_metadatatreebrowser_BrowseOverview_list_overview-list.root-list a.toggle").first().children("i.glyphicon").removeClass("glyphicon-plus closed-icon").addClass("glyphicon-minus open-icon");
   }
   
   // Add the expand all button
   jQuery("#edu_tamu_metadatatreebrowser_BrowseOverview_div_metadata-tree-browser-overview").prepend("<a class=\"expand-all ds-button-field btn btn-default\" href=\"javascript: void(0);\">Expand All</a>");
   jQuery("#edu_tamu_metadatatreebrowser_BrowseOverview_div_metadata-tree-browser-overview .expand-all").click(function() {
      if (jQuery(this).text().indexOf('Expand') > -1) { 
         // Expand all Children
         jQuery("#edu_tamu_metadatatreebrowser_BrowseOverview_list_overview-list a.toggle").each(function () {
            count = jQuery(this).parent().next().has('li ul li').length;
            jQuery(this).parent().next().has('li').slideDown(count * 200 + 200);
            jQuery(this).children("i.glyphicon").removeClass("glyphicon-plus closed-icon").addClass("glyphicon-minus open-icon");
         });
         jQuery(this).text('Collapse All');
         
      } else {
         // Collapse all Children
         jQuery("#edu_tamu_metadatatreebrowser_BrowseOverview_list_overview-list a.toggle").each(function () {
            count = jQuery(this).parent().next().has('li ul li').length;
            jQuery(this).parent().next().has('li').slideUp(count * 200 + 200);
            jQuery(this).children("i.glyphicon").removeClass("glyphicon-minus open-icon").addClass("glyphicon-plus closed-icon");
         });
         jQuery(this).text('Expand All');
      }
      
   });
});