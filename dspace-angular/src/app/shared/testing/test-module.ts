import { CommonModule } from '@angular/common';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { MySimpleItemActionComponent } from '../../item-page/edit-item-page/simple-item-action/abstract-simple-item-action.component.spec';
import { SharedModule } from '../shared.module';
import { NgComponentOutletDirectiveStub } from './ng-component-outlet-directive.stub';
import { QueryParamsDirectiveStub } from './query-params-directive.stub';
import { RouterLinkDirectiveStub } from './router-link-directive.stub';
import { BrowserOnlyMockPipe } from './browser-only-mock.pipe';

/**
 * This module isn't used. It serves to prevent the AoT compiler
 * complaining about components/pipes/directives that were
 * created only for use in tests.
 * See https://github.com/angular/angular/issues/13590
 */
@NgModule({
  imports: [
    CommonModule,
    SharedModule
  ],
  declarations: [
    QueryParamsDirectiveStub,
    MySimpleItemActionComponent,
    RouterLinkDirectiveStub,
    NgComponentOutletDirectiveStub,
    BrowserOnlyMockPipe,
  ],
    exports: [
        QueryParamsDirectiveStub,
        RouterLinkDirectiveStub
    ],
  schemas: [
    CUSTOM_ELEMENTS_SCHEMA
  ]
})
export class TestModule {
}
