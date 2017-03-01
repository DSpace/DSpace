/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
$(document).ready(function() {
    //create a cookie with name: @name and value: @value, to expire in @years years
    function setCookie(name,value,years) {
        exp = new Date();
        exp.setTime(exp.getTime()+(years*365*24*60*60*1000));
        document.cookie = name+"="+value+";path="+window.location.pathname+";expires="+exp.toGMTString();
    }

    //get cookie by name: @name
    function getCookie(name) {
        var cookieCutter = name+"=";
        var temp = document.cookie.split(";");
        for (var i=0;i<temp.length;i++) {
            var cookie = temp[i].trim();
            if (cookie.indexOf(cookieCutter)==0) {
                return cookie.substring(cookieCutter.length,cookie.length);
            }
        }
        return null;
    }

    $(".toggler").click(function(e) {
        e.preventDefault();
        parentid = $(this).attr("data-target");
        $togglerNext = $(this).children(".hidden");
        $(this).children("i:not(.hidden)").addClass("hidden");
        $togglerNext.removeClass("hidden");
        if ($(parentid).hasClass("hidden")) {
            $(parentid).removeClass("hidden");
        } else {
            $(parentid).addClass("hidden");
        }
    });
    $(".expand-all").click(function(e) {
        e.preventDefault();
        $(".toggler").children().each(function() {
            $("i.closed-icon").addClass("hidden");
            $("i.open-icon").removeClass("hidden");
        });
        $(".sub-tree-wrapper").removeClass("hidden");
    });
    $(".close-all").click(function(e) {
        e.preventDefault();
        $(".toggler").children().each(function() {
            $("i.closed-icon").removeClass("hidden");
            $("i.open-icon").addClass("hidden");
        });
        $(".sub-tree-wrapper").addClass("hidden");
    });

    //*** Remember the state of the community browser   
    if (jQuery("#aspect_artifactbrowser_CommunityBrowser_div_comunity-browser").length) {
        var rawCookie = null;
        //try to load cookie
        if (rawCookie = getCookie("expander")) {
            //parse cookie
            if (state = JSON.parse(rawCookie)) {
                //expand accordion for previously opened nodes
                jQuery.each(state,function(i,v) {
                    $("#aspect_artifactbrowser_CommunityBrowser_referenceSet_community-browser a.toggler[data-target='#"+v+"']").click();
                });
            }
        }
        //on page exit: store state to cookie
        window.onbeforeunload = function () {
            var openNodes = [];
            jQuery("#aspect_artifactbrowser_CommunityBrowser_referenceSet_community-browser").find(".sub-tree-wrapper").each(function() {
                if ($(this).is(":visible")) {
                    openNodes.push($(this).attr("id"));
                }
            });
            setCookie("expander",JSON.stringify(openNodes),10);
        };
    }
});
