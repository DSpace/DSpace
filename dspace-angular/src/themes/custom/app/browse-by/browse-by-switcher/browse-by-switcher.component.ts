import { Component } from '@angular/core';
import { BrowseBySwitcherComponent as BaseComponent } from '../../../../../app/browse-by/browse-by-switcher/browse-by-switcher.component';

@Component({
  selector: 'ds-browse-by-switcher',
  // styleUrls: ['./browse-by-switcher.component.scss'],
  // templateUrl: './browse-by-switcher.component.html'
  templateUrl: '../../../../../app/browse-by/browse-by-switcher/browse-by-switcher.component.html'
})

/**
 * Component for determining what Browse-By component to use depending on the metadata (browse ID) provided
 */
export class BrowseBySwitcherComponent extends BaseComponent {}

