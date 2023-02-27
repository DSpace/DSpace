import {Component} from '@angular/core';
import {EditItemSelectorComponent} from './edit-item-selector.component';
import {ThemedComponent} from 'src/app/shared/theme-support/themed.component';

/**
 * Themed wrapper for EditItemSelectorComponent
 */
@Component({
    selector: 'ds-themed-edit-item-selector',
    styleUrls: [],
    templateUrl: '../../../theme-support/themed.component.html'
})
export class ThemedEditItemSelectorComponent
    extends ThemedComponent<EditItemSelectorComponent> {
    protected getComponentName(): string {
        return 'EditItemSelectorComponent';
    }

    protected importThemedComponent(themeName: string): Promise<any> {
        return import(`../../../../../themes/${themeName}/app/shared/dso-selector/modal-wrappers/edit-item-selector/edit-item-selector.component`);
    }

    protected importUnthemedComponent(): Promise<any> {
        return import('./edit-item-selector.component');
    }

}
