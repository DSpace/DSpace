import {Component} from '@angular/core';
import {CreateCommunityParentSelectorComponent} from './create-community-parent-selector.component';
import {ThemedComponent} from 'src/app/shared/theme-support/themed.component';

/**
 * Themed wrapper for CreateCommunityParentSelectorComponent
 */
@Component({
    selector: 'ds-themed-create-community-parent-selector',
    styleUrls: [],
    templateUrl: '../../../theme-support/themed.component.html'
})
export class ThemedCreateCommunityParentSelectorComponent
    extends ThemedComponent<CreateCommunityParentSelectorComponent> {
    protected getComponentName(): string {
        return 'CreateCommunityParentSelectorComponent';
    }

    protected importThemedComponent(themeName: string): Promise<any> {
        return import(`../../../../../themes/${themeName}/app/shared/dso-selector/modal-wrappers/create-community-parent-selector/create-community-parent-selector.component`);
    }

    protected importUnthemedComponent(): Promise<any> {
        return import('./create-community-parent-selector.component');
    }

}
