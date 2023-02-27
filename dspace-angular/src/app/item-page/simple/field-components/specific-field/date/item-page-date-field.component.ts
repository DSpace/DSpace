import { Component, Input } from '@angular/core';

import { Item } from '../../../../../core/shared/item.model';
import { ItemPageFieldComponent } from '../item-page-field.component';

@Component({
    selector: 'ds-item-page-date-field',
    templateUrl: '../item-page-field.component.html'
})
/**
 * This component is used for displaying the issue date (dc.date.issued) metadata of an item
 */
export class ItemPageDateFieldComponent extends ItemPageFieldComponent {

    /**
     * The item to display metadata for
     */
    @Input() item: Item;

    /**
     * Separator string between multiple values of the metadata fields defined
     * @type {string}
     */
    separator = ', ';

    /**
     * Fields (schema.element.qualifier) used to render their values.
     * In this component, we want to display values for metadata 'dc.date.issued'
     */
    fields: string[] = [
        'dc.date.issued'
    ];

    /**
     * Label i18n key for the rendered metadata
     */
    label = 'item.page.date';

}
