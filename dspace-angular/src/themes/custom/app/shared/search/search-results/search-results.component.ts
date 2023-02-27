import { SearchResultsComponent as BaseComponent } from '../../../../../../app/shared/search/search-results/search-results.component';
import { Component } from '@angular/core';
import { fadeIn, fadeInOut } from '../../../../../../app/shared/animations/fade';

@Component({
  selector: 'ds-search-results',
  // templateUrl: './search-results.component.html',
  templateUrl: '../../../../../../app/shared/search/search-results/search-results.component.html',
  // styleUrls: ['./search-results.component.scss'],
  animations: [
    fadeIn,
    fadeInOut
  ]
})
export class SearchResultsComponent extends BaseComponent {

}
