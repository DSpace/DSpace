import { ThemedComponent } from '../../../../shared/theme-support/themed.component';
import { FileSectionComponent } from './file-section.component';
import {Component, Input} from '@angular/core';
import {Item} from '../../../../core/shared/item.model';

@Component({
    selector: 'ds-themed-item-page-file-section',
    templateUrl: '../../../../shared/theme-support/themed.component.html',
})
export class ThemedFileSectionComponent extends ThemedComponent<FileSectionComponent> {

    @Input() item: Item;

    protected inAndOutputNames: (keyof FileSectionComponent & keyof this)[] = ['item'];

    protected getComponentName(): string {
        return 'FileSectionComponent';
    }

    protected importThemedComponent(themeName: string): Promise<any> {
        return import(`../../../../../themes/${themeName}/app/item-page/simple/field-components/file-section/file-section.component`);
    }

    protected importUnthemedComponent(): Promise<any> {
        return import(`./file-section.component`);
    }

}
