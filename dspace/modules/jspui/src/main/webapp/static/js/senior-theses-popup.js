/*
 * When the user clicks on a link whose target URL contains the string "bitstream", this script will generate a popup window displaying terms of use
 * for senior theses.
 *
 * Author: Mark Ratliff
 *
 */

window.onload = function()
{  
     var lnks = document.links;
     if (lnks)
     {
        for (var i = 0; i < lnks.length; ++i)
        {
          lnks[i].onclick = linkOnClick;
        }
      }
};


function linkOnClick()
{
  var h = this.href;
  var action = true;

  if (h.indexOf('bitstream') != -1) 
  {
    if (!confirm('PDF access currently not available. Contact Mudd@princeton.edu with questions. (Please click the Cancel button.)'))
    {      
      action = false;    
    }  
  }

  return action;
}
