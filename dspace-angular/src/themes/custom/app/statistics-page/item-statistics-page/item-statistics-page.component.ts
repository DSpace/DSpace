import { Component } from '@angular/core';
import { ItemStatisticsPageComponent as BaseComponent } from '../../../../../app/statistics-page/item-statistics-page/item-statistics-page.component';

@Component({
  selector: 'ds-item-statistics-page',
  // styleUrls: ['./item-statistics-page.component.scss'],
  styleUrls: ['../../../../../app/statistics-page/item-statistics-page/item-statistics-page.component.scss'],
  // templateUrl: './item-statistics-page.component.html',
  templateUrl: '../../../../../app/statistics-page/statistics-page/statistics-page.component.html'
})

/**
 * Component representing the statistics page for an item.
 */
export class ItemStatisticsPageComponent extends BaseComponent {}

