import { Component, Inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BROWSE_BY_COMPONENT_FACTORY } from './browse-by-decorator';
import { GenericConstructor } from '../../core/shared/generic-constructor';
import { BrowseDefinition } from '../../core/shared/browse-definition.model';
import { ThemeService } from '../../shared/theme-support/theme.service';

@Component({
  selector: 'ds-browse-by-switcher',
  templateUrl: './browse-by-switcher.component.html'
})
/**
 * Component for determining what Browse-By component to use depending on the metadata (browse ID) provided
 */
export class BrowseBySwitcherComponent implements OnInit {

  /**
   * Resolved browse-by component
   */
  browseByComponent: Observable<any>;

  public constructor(protected route: ActivatedRoute,
                     protected themeService: ThemeService,
                     @Inject(BROWSE_BY_COMPONENT_FACTORY) private getComponentByBrowseByType: (browseByType, theme) => GenericConstructor<any>) {
  }

  /**
   * Fetch the correct browse-by component by using the relevant config from the route data
   */
  ngOnInit(): void {
    this.browseByComponent = this.route.data.pipe(
      map((data: { browseDefinition: BrowseDefinition }) => this.getComponentByBrowseByType(data.browseDefinition.dataType, this.themeService.getThemeName()))
    );
  }

}
