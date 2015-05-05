
(function () {
    $(document).ready(function () {

        //HTML5 Video player fallback to Flash

        (function() {
            var video = document.createElement("video");
            // detect browsers with no _video_ support
            if ( typeof(video.canPlayType) == 'undefined' || video.canPlayType('video/x-m4v') == '') { // detect the ability to play H.264/MP4

                video = $('#html5video');
                var videoUrl = video.find('source').attr('src');

                var flashUrl = ($(location).attr("protocol") == "http:") ? "http://" : "https://";
                flashUrl = flashUrl + "library.osu.edu/resources/jwplayer/jwplayer_5.10.swf";

                var flash = '<object type="application/x-shockwave-flash" data="' + flashUrl + '" width="' + video.width() + '" height="' + video.height() + '">' +
                    '		<param name="allowfullscreen" value="true" /> ' +
                    '       <param name="flashvars" value="file=' + videoUrl + '&amp;type=video"/> ' +
                    '</object>';

                // insert flash and remove video
                video.before(flash);
                video.detach();
            }
        })();

        //LazyLoad of images (within modal, or click usually)
        $("img.lazy").lazyload();

        (function() {
            var audio = document.createElement("audio");
            if(typeof(audio.canPlayType) == 'undefined' || audio.canPlayType('audio/mpeg') == '') {
                audio = $('#html5audio');
                var audioUrl = audio.find('source').attr('src');

                var flashUrl = ($(location).attr("protocol") == "http:") ? "http://" : "https://";
                flashUrl = flashUrl + "library.osu.edu/resources/jwplayer/jwplayer_5.10.swf";

                var flashEmbed = '<embed type="application/x-shockwave-flash" src="'+flashUrl+'" width="470" height="24" flashvars="file='+audioUrl+'">';

                // insert flash and remove audio
                audio.before(flashEmbed);
                audio.detach();
            }
        })();

        //Show/Hide
        $('ul#file_list').addClass('js').removeClass('no-js');
        return $('ul#file_list .file-entry .slide-arrow').click(function () {
            var fentry, fv, fvc;
            fentry = $(this).parent(".file-entry");
            if ($(this).hasClass("show")) {
                //$(this).removeClass("show");
                //$(this).addClass("hide");
                //$(this).find("span.showhide").html("Hide file");
                fv = fentry.find(".file-view");
                fvc = fv.find(".file-view-container");
                return fv.slideDown();
            } else {
                //$(this).removeClass("hide");
                //$(this).addClass("show");
                //$(this).find("span.showhide").html("Show file");
                fv = fentry.find(".file-view");
                fvc = fv.find(".file-view-container");
                return fv.slideUp();
            }
        });
    });
}).call(this);
