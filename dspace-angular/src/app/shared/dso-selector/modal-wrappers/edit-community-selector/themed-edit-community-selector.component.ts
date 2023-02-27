import {Component} from '@angular/core';
import {EditCommunitySelectorComponent} from './edit-community-selector.component';
import {ThemedComponent} from 'src/app/shared/theme-support/themed.component';

/**
 * Themed wrapper for EditCommunitySelectorComponent
 */
@Component({
    selector: 'ds-themed-edit-community-selector',
    styleUrls: [],
    templateUrl: '../../../theme-support/themed.component.html'
})
export class ThemedEditCommunitySelectorComponent
    extends ThemedComponent<EditCommunitySelectorComponent> {
    protected getComponentName(): string {
        return 'EditCommunitySelectorComponent';
    }

    protected importThemedComponent(themeName: string): Promise<any> {
        return import(`../../../../../themes/${themeName}/app/shared/dso-selector/modal-wrappers/edit-community-selector/edit-community-selector.component`);
    }

    protected importUnthemedComponent(): Promise<any> {
        return import('./edit-community-selector.component');
    }

}
