const version = '2.0.0';
const applicationName = 'yearpicker-v2';


const event_click = 'click.';
const event_focus = 'focus.';
const event_keyup = 'keyup.';
const event_selected = 'selected.';
const event_show = 'show.';
const event_hide = 'hide.';


if(!jQuery){
    alert(`${appName} ${version} requires jQuery`);
}

var defaults = {
    // Auto Hide
    autoHide: true,
    // The Initial Date
    year: null,
    // Start Date
    startYear: null,
    // End Date
    endYear: null,
    // A element tag items
    itemTag: 'li',
    //css class selected date item 
    selectedClass: 'selected',
    // css class disabled
    disabledClass: 'disabled',
    hideClass: 'hide',
    highlightedClass: 'highlighted',
    template: `<div class="yearpicker-container">
                    <div class="yearpicker-header">
                        <div class="yearpicker-prev" data-view="yearpicker-prev">&lsaquo;</div>
                        <div class="yearpicker-current" data-view="yearpicker-current">SelectedYear</div>
                        <div class="yearpicker-next" data-view="yearpicker-next">&rsaquo;</div>
                    </div>
                    <div class="yearpicker-body">
                        <ul class="yearpicker-year" data-view="years">
                        </ul>
                    </div>
                </div>
`,

    // Event shortcuts
    show: null,
    hide: null,
    pick: null
};

