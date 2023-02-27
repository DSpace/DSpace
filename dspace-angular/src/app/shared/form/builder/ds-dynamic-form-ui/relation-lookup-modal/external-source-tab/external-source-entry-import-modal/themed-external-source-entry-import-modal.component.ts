import { ExternalSourceEntryImportModalComponent } from './external-source-entry-import-modal.component';
import { ThemedComponent } from '../../../../../../theme-support/themed.component';
import { Component } from '@angular/core';

@Component({
  selector: 'ds-themed-external-source-entry-import-modal',
  styleUrls: [],
  templateUrl: '../../../../../../../shared/theme-support/themed.component.html',
})
export class ThemedExternalSourceEntryImportModalComponent extends ThemedComponent<ExternalSourceEntryImportModalComponent> {
  protected getComponentName(): string {
    return 'ExternalSourceEntryImportModalComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../../../../../../themes/${themeName}/app/shared/form/builder/ds-dynamic-form-ui/relation-lookup-modal/external-source-tab/external-source-entry-import-modal/external-source-entry-import-modal.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./external-source-entry-import-modal.component`);
  }
}
