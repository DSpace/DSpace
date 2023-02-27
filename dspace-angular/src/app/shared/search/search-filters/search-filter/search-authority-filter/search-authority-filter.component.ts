import { Component, OnInit } from '@angular/core';
import { FilterType } from '../../../models/filter-type.model';
import { facetLoad, SearchFacetFilterComponent } from '../search-facet-filter/search-facet-filter.component';
import { renderFacetFor } from '../search-filter-type-decorator';

@Component({
  selector: 'ds-search-authority-filter',
  styleUrls: ['./search-authority-filter.component.scss'],
  templateUrl: './search-authority-filter.component.html',
  animations: [facetLoad]
})

/**
 * Component that represents an authority facet for a specific filter configuration
 */
@renderFacetFor(FilterType.authority)
export class SearchAuthorityFilterComponent extends SearchFacetFilterComponent implements OnInit {
}
