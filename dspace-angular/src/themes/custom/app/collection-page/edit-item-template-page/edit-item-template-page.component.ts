import { Component } from '@angular/core';
import {
  EditItemTemplatePageComponent as BaseComponent
} from '../../../../../app/collection-page/edit-item-template-page/edit-item-template-page.component';

@Component({
  selector: 'ds-edit-item-template-page',
  styleUrls: ['./edit-item-template-page.component.scss'],
  // templateUrl: './edit-item-template-page.component.html',
  templateUrl: '../../../../../app/collection-page/edit-item-template-page/edit-item-template-page.component.html',
})
/**
 * Component for editing the item template of a collection
 */
export class EditItemTemplatePageComponent extends BaseComponent {
}
