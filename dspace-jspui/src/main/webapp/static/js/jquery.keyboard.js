(function() {

    jQuery.keyboardLayout = {};

    jQuery.keyboardLayout.indicator = jQuery('<div class="keyboardLayout" style="position: absolute; top: 7px; right: 295px; color: #4169E0;"/>');

    jQuery.keyboardLayout.target;

    jQuery.keyboardLayout.layout;

    jQuery.keyboardLayout.show = function(layout){
        this.layout = layout;
        this.indicator.text(layout);
        this.target.after(this.indicator);
    };

    jQuery.keyboardLayout.hide = function(){
        this.target = null;
        this.layout = null;
        this.indicator.remove();
    };

    jQuery.fn.keyboardLayout = function()
    {
        this.each(function(){

            jQuery(this).focus(function(){
                jQuery.keyboardLayout.target = jQuery(this);
            });

            jQuery(this).blur(function(){
                jQuery.keyboardLayout.hide();
            });

            jQuery(this).keypress(function(e){
                var c = (e.charCode == undefined ? e.keyCode : e.charCode);
                var layout = jQuery.keyboardLayout.layout;
                if (c >= 97/*a*/  && c <= 122/*z*/ && !e.shiftKey ||
                    c >= 65/*A*/  && c <= 90/*Z*/  &&  e.shiftKey ||
                    (c == 91/*[*/  && !e.shiftKey ||
                        c == 93/*]*/  && !e.shiftKey ||
                        c == 123/*{*/ &&  e.shiftKey ||
                        c == 125/*}*/ &&  e.shiftKey ||
                        c == 96/*`*/  && !e.shiftKey ||
                        c == 126/*~*/ &&  e.shiftKey ||
                        c == 64/*@*/  &&  e.shiftKey ||
                        c == 35/*#*/  &&  e.shiftKey ||
                        c == 36/*$*/  &&  e.shiftKey ||
                        c == 94/*^*/  &&  e.shiftKey ||
                        c == 38/*&*/  &&  e.shiftKey ||
                        c == 59/*;*/  && !e.shiftKey ||
                        c == 39/*'*/  && !e.shiftKey ||
                        c == 44/*,*/  && !e.shiftKey ||
                        c == 60/*<*/  &&  e.shiftKey ||
                        c == 62/*>*/  &&  e.shiftKey) && layout != 'EN') {

                    layout = 'EN';

                } else if (c >= 65/*A*/ && c <= 90/*Z*/  && !e.shiftKey ||
                    c >= 97/*a*/ && c <= 122/*z*/ &&  e.shiftKey) {

                    layout = 'EN';

                } else if (c >= 1072/*Р°*/ && c <= 1103/*СЏ*/ && !e.shiftKey ||
                    c >= 1040/*Рђ*/ && c <= 1071/*РЇ*/ &&  e.shiftKey ||
                    (c == 1105/*С‘*/ && !e.shiftKey ||
                        c == 1025/*РЃ*/ &&  e.shiftKey ||
                        c == 8470/*в„–*/ &&  e.shiftKey ||
                        c == 59/*;*/   &&  e.shiftKey ||
                        c == 44/*,*/   &&  e.shiftKey) && layout != 'RU') {

                    layout = 'RU';

                } else if (c >= 1040/*Рђ*/ && c <= 1071/*РЇ*/ && !e.shiftKey ||
                    c >= 1072/*Р°*/ && c <= 1103/*СЏ*/ &&  e.shiftKey) {

                    layout = 'RU';

                }else if((c == 1108 /*є*/ || c == 1110 /*і*/|| c == 1111/*ї*/ || c == 1169/*ґ*/) ||
                    (c == 1028 /*Є*/ || c == 1030 /*І*/|| c == 1031 /*Ї*/|| c == 1068/*Ґ*/)){

                    layout = 'UK';
                }
                if (layout) {
                    jQuery.keyboardLayout.show(layout);
                }
            });
        });
    };

})();
