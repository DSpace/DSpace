/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {

    //Catch "Holder: invisible placeholder"
    Holder.invisible_error_fn = function(fn){
        return function(el){
            try
            {
                fn.call(this, el)
            }
            catch(err)
            {
                //Catch "Holder: invisible placeholder"
            }
        }
    }

})(jQuery);