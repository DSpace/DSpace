import { Component } from '@angular/core';
import { SiteStatisticsPageComponent as BaseComponent } from '../../../../../app/statistics-page/site-statistics-page/site-statistics-page.component';

@Component({
  selector: 'ds-site-statistics-page',
  // styleUrls: ['./site-statistics-page.component.scss'],
  styleUrls: ['../../../../../app/statistics-page/site-statistics-page/site-statistics-page.component.scss'],
  // templateUrl: './site-statistics-page.component.html',
  templateUrl: '../../../../../app/statistics-page/statistics-page/statistics-page.component.html'
})

/**
 * Component representing the site-wide statistics page.
 */
export class SiteStatisticsPageComponent extends BaseComponent {}

