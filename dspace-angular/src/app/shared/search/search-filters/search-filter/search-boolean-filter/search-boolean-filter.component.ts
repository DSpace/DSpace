import { Component, OnInit } from '@angular/core';
import { FilterType } from '../../../models/filter-type.model';
import { renderFacetFor } from '../search-filter-type-decorator';
import { facetLoad, SearchFacetFilterComponent } from '../search-facet-filter/search-facet-filter.component';

@Component({
  selector: 'ds-search-boolean-filter',
  styleUrls: ['./search-boolean-filter.component.scss'],
  templateUrl: './search-boolean-filter.component.html',
  animations: [facetLoad]
})

/**
 * Component that represents a boolean facet for a specific filter configuration
 */
@renderFacetFor(FilterType.boolean)
export class SearchBooleanFilterComponent extends SearchFacetFilterComponent implements OnInit {
}
