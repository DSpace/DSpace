import {Component, Input} from '@angular/core';
import {CreateItemParentSelectorComponent} from './create-item-parent-selector.component';
import {ThemedComponent} from 'src/app/shared/theme-support/themed.component';

/**
 * Themed wrapper for CreateItemParentSelectorComponent
 */
@Component({
    selector: 'ds-themed-create-item-parent-selector',
    styleUrls: [],
    templateUrl: '../../../theme-support/themed.component.html'
})
export class ThemedCreateItemParentSelectorComponent
    extends ThemedComponent<CreateItemParentSelectorComponent> {
    @Input() entityType: string;

    protected inAndOutputNames: (keyof CreateItemParentSelectorComponent & keyof this)[] = ['entityType'];

    protected getComponentName(): string {
        return 'CreateItemParentSelectorComponent';
    }

    protected importThemedComponent(themeName: string): Promise<any> {
        return import(`../../../../../themes/${themeName}/app/shared/dso-selector/modal-wrappers/create-item-parent-selector/create-item-parent-selector.component`);
    }

    protected importUnthemedComponent(): Promise<any> {
        return import('./create-item-parent-selector.component');
    }

}
