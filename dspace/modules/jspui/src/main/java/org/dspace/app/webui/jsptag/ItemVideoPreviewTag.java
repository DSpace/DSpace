/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.webui.jsptag;

import org.dspace.app.webui.util.UIUtil;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * <p>
 * JSP tag for displaying a video pseudo-streaming preview version of an item. 
 * For this tag to output anything, the preview feature must be activated in DSpace.
 * </p>
 * @author David Andrés Maznzano Herrera <damanzano>
 */
public class ItemVideoPreviewTag extends TagSupport{
    
    /** Item to display */
    private transient Item item;
    
    /** Title to use in video tag*/
    private transient String title;
    
    /** Type of player to use*/
    private transient String player;
    
    private static final long serialVersionUID = -5535762797556685631L;
    
    public ItemVideoPreviewTag()
    {
        super();
    }
    
    public int doStartTag() throws JspException
    {
    	if (!ConfigurationManager.getBooleanProperty("webui.preview.enabled"))
    	{
    		return SKIP_BODY;
    	}
        try
        {
            String youtubekey=existYoutubeIdentifier();
            if(youtubekey!= null){
                showYoutubePlayerPreview(youtubekey);
            }else{
                if("swfobject".equalsIgnoreCase(this.player)){
                    showSWFObjectPreview();
                }else{
                    showJPlayerPreview();
                }
            }
            
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle);
        }
        catch (IOException ioe)
        {
            throw new JspException(ioe);
        }

        return SKIP_BODY;
    }
    
    public void setItem(Item item)
    {
        this.item = item;
    }
    
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    public void setPlayer(String player)
    {
        this.player = player;
    }
    
    private void showSWFObjectPreview() throws SQLException, IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        
        // Only shows 1 preview video (the first encountered) regardless
        // of the number of bundles/bitstreams of this type
        Bundle[] bundles = item.getBundles("ORIGINAL");
        
        if (bundles.length > 0) {
            Bitstream[] bitstreams = bundles[0].getBitstreams();
            boolean found = false;
            for (Bitstream bitstream : bitstreams) {
                if (!found) {
                    if ("video/x-flv".equals(bitstream.getFormat().getMIMEType())) {
                        // We found one, don't search for any more
                        found = true;

                        // Display the player
                        
                        String url = request.getContextPath()
                                + "/bitstream/" + item.getHandle() + "/"
                                + bitstream.getSequenceID() + "/"
                                + UIUtil.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);
                        
                        out.println("<script type=\"text/javascript\" src=\"" + request.getContextPath()
                                + "/swfobject.js\"></script>"
                                + "<div class=\"item-video-player text-center visible-md visible-lg\" id=\"player\">Video</div>"
                                + "<script type=\"text/javascript\">"
                                + "var so = new SWFObject('" + request.getContextPath() + "/player.swf','mpl','320','240','9');"
                                + "so.addParam('allowscriptaccess','always');"
                                + "so.addParam('allowfullscreen','true');"
                                + "so.addParam('flashvars','&file=" + url + "&autostart=true');"
                                + "so.write('player');"
                                + "</script>");
                    }
                }
            }
        }
    }
    
    private void showJPlayerPreview() throws SQLException, IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        
        // Only shows 1 preview video (the first encountered) regardless
        // of the number of bundles/bitstreams of this type
        Bundle[] bundles = item.getBundles("ORIGINAL");
        
        if (bundles.length > 0) {
            Bitstream[] bitstreams = bundles[0].getBitstreams();

            // First flv file found
            String flvUrl= null;
            // First m4v or mp4 file found
            String m4vUrl = null;

            for (Bitstream bitstream : bitstreams) {
                if (flvUrl == null || m4vUrl == null) {
                    if (flvUrl == null) {
                        if ("video/x-flv".equals(bitstream.getFormat().getMIMEType())) {
                            flvUrl = request.getContextPath()
                                    + "/bitstream/" + item.getHandle() + "/"
                                    + bitstream.getSequenceID() + "/"
                                    + UIUtil.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);
                        }
                    }
                    if (m4vUrl == null) {
                        if ("video/mp4".equals(bitstream.getFormat().getMIMEType())
                                || "video/x-m4v".equals(bitstream.getFormat().getMIMEType())) {
                            m4vUrl = request.getContextPath()
                                    + "/bitstream/" + item.getHandle() + "/"
                                    + bitstream.getSequenceID() + "/"
                                    + UIUtil.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);
                        }
                    }

                }
            }
            
            // Display the player
            if (flvUrl != null || m4vUrl != null) {
                
                /*  FIXME:
                    If there is not m4v files do not show the player in small 
                    devices
                */
                
                // Build html markup required for player
                out.println("<div class=\"item-video-jplayer\">"
                        + "<div id=\"jp_container_1\" class=\"jp-video jp-video-360p center-block\">"
                        + "			<div class=\"jp-type-single\">"
                        + "				<div id=\"jquery_jplayer_1\" class=\"jp-jplayer\"></div>"
                        + "				<div class=\"jp-gui\">"
                        + "					<div class=\"jp-video-play\">"
                        + "						<a href=\"javascript:;\" class=\"jp-video-play-icon\" tabindex=\"1\">play</a>"
                        + "					</div>"
                        + "					<div class=\"jp-interface\">"
                        + "						<div class=\"jp-progress\">"
                        + "							<div class=\"jp-seek-bar\">"
                        + "								<div class=\"jp-play-bar\"></div>"
                        + "							</div>"
                        + "						</div>"
                        + "						<div class=\"jp-current-time\"></div>"
                        + "						<div class=\"jp-duration\"></div>"
                        + "						<div class=\"jp-controls-holder\">"
                        + "							<ul class=\"jp-controls\">"
                        + "								<li><a href=\"javascript:;\" class=\"jp-play\" tabindex=\"1\">play</a></li>"
                        + "								<li><a href=\"javascript:;\" class=\"jp-pause\" tabindex=\"1\">pause</a></li>"
                        + "								<li><a href=\"javascript:;\" class=\"jp-stop\" tabindex=\"1\">stop</a></li>"
                        + "								<li><a href=\"javascript:;\" class=\"jp-mute\" tabindex=\"1\" title=\"mute\">mute</a></li>"
                        + "								<li><a href=\"javascript:;\" class=\"jp-unmute\" tabindex=\"1\" title=\"unmute\">unmute</a></li>"
                        + "								<li><a href=\"javascript:;\" class=\"jp-volume-max\" tabindex=\"1\" title=\"max volume\">max volume</a></li>"
                        + "							</ul>"
                        + "							<div class=\"jp-volume-bar\">"
                        + "								<div class=\"jp-volume-bar-value\"></div>"
                        + "							</div>"
                        + "							<ul class=\"jp-toggles\">"
                        + "								<li><a href=\"javascript:;\" class=\"jp-full-screen\" tabindex=\"1\" title=\"full screen\">full screen</a></li>"
                        + "								<li><a href=\"javascript:;\" class=\"jp-restore-screen\" tabindex=\"1\" title=\"restore screen\">restore screen</a></li>"
                        + "								<li><a href=\"javascript:;\" class=\"jp-repeat\" tabindex=\"1\" title=\"repeat\">repeat</a></li>"
                        + "								<li><a href=\"javascript:;\" class=\"jp-repeat-off\" tabindex=\"1\" title=\"repeat off\">repeat off</a></li>"
                        + "							</ul>"
                        + "						</div>"
                        + "						<div class=\"jp-details\">"
                        + "							<ul>"
                        + "								<li><span class=\"jp-title\"></span></li>"
                        + "							</ul>"
                        + "						</div>"
                        + "					</div>"
                        + "				</div>"
                        + "				<div class=\"jp-no-solution\">"
                        + "					<span>Update Required</span>"
                        + "					To play the media you will need to either update your browser to a recent version or update your <a href=\"http://get.adobe.com/flashplayer/\" target=\"_blank\">Flash plugin</a>."
                        + "				</div>"
                        + "			</div>"
                        + "		</div>"
                        + "</div>");
                
                // Build the jPlayer Object
                StringBuffer sb = new StringBuffer();
                
                String scriptStart = "<script type=\"text/javascript\">\n"
                        + "jQuery(document).ready(function(){\n"
                        + "	jQuery(\"#jquery_jplayer_1\").jPlayer({\n";
                String scriptEnd = "	});\n"
                        + "});\n"
                        + "</script>";
                
                String playerMediaParams = "		ready: function () {\n"
                        + "			jQuery(this).jPlayer(\"setMedia\", {\n"
                        + "				title: \""+this.title+"\"\n";
                String suppliedFormats ="";
                if(m4vUrl!=null){
                    playerMediaParams += ", m4v:\""+m4vUrl+"\"\n";
                    if(!suppliedFormats.equals("")){
                        suppliedFormats +=", ";
                    }
                    suppliedFormats +="m4v";
                }
                if(flvUrl!=null){
                    playerMediaParams += ", flv:\""+flvUrl+"\"\n";
                    if(!suppliedFormats.equals("")){
                        suppliedFormats +=", ";
                    }
                    suppliedFormats +="flv";
                }
                
                //close setMedia function
                playerMediaParams +="			});\n"
                        + "		},\n";
                
                String otherParams = "		swfPath: \""+ request.getContextPath() +"/static/js/jplayer\",\n"
                        + "		solution: \"html, flash\",\n"
                        + "		supplied: \""+suppliedFormats+"\",\n"
                        + "		size: {"
                        + "			width: \"640px\",\n"
                        + "			height: \"360px\",\n"
                        + "			cssClass: \"jp-video-360p\"\n"
                        + "		},"
                        + "		smoothPlayBar: true,\n"
                        + "		keyEnabled: true,\n"
                        + "		remainingDuration: true,\n"
                        + "		toggleDuration: true\n";
                sb.append(scriptStart);
                sb.append(playerMediaParams);
                sb.append(otherParams);
                sb.append(scriptEnd);
                
                /*  damanzano:
                    The script object is saved in the request instead of printed
                    in order to be managed by the layout tag at footer-defualt.jsp
                    This way we can put all javascriptn code at the htmt body's 
                    end and avoid javascript dependncy errors.
                 */
                request.setAttribute("item.video.preview.script",sb.toString());
                //out.println(sb.toString());
            }
        }
    }
    
    private void showYoutubePlayerPreview(String youtubekey) throws SQLException, IOException
    {
        JspWriter out = pageContext.getOut();
        StringBuffer sb = new StringBuffer();
        sb.append("<div class=\"item-video-youtube-player\">");
        sb.append("<iframe class=\"center-block\" width=\"100%\" height=\"360px\" src=\"//www.youtube.com/embed/");
        sb.append(youtubekey);
        sb.append("\" frameborder=\"0\" allowfullscreen></iframe>");
        sb.append("</div>");

        out.println(sb.toString());
    }
    
    private String existYoutubeIdentifier() 
    {
        DCValue[] youtubeIdentifiers;
        youtubeIdentifiers=item.getMetadata("dc", "identifier", "youtubeedu", Item.ANY);
        String youtubekey= null;
        if(youtubeIdentifiers != null && youtubeIdentifiers.length > 0)
        {
            youtubekey= youtubeIdentifiers[0].value;
        }
        return youtubekey; 
    }
    
    public void release()
    {
        item = null;
    }
}