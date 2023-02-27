import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { configureSearchComponentTestingModule } from '../shared/search/search.component.spec';
import { ConfigurationSearchPageComponent } from './configuration-search-page.component';
import { SearchConfigurationService } from '../core/shared/search/search-configuration.service';
import { Component, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { RouteService } from '../core/services/route.service';
import createSpy = jasmine.createSpy;

const CONFIGURATION = 'test-configuration';
const QUERY = 'test query';

@Component({
  template: `
    <ds-configuration-search-page [configuration]="'${CONFIGURATION}'"
                                  [fixedFilterQuery]="'${QUERY}'"
                                  #configurationSearchPage>
    </ds-configuration-search-page>
  `,
})
class HostComponent {
  @ViewChild('configurationSearchPage') configurationSearchPage: ConfigurationSearchPageComponent;
}

describe('ConfigurationSearchPageComponent', () => {
  let comp: ConfigurationSearchPageComponent;
  let fixture: ComponentFixture<HostComponent>;
  let searchConfigService: SearchConfigurationService;
  let routeService: RouteService;

  beforeEach(waitForAsync(() => {
    configureSearchComponentTestingModule(ConfigurationSearchPageComponent, [HostComponent]);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HostComponent);

    // Set router url to a dummy value for SearchComponent#ngOnInit
    spyOnProperty(TestBed.inject(Router), 'url', 'get').and.returnValue('some/url/here');

    routeService = TestBed.inject(RouteService);
    routeService.setParameter = createSpy('setParameter');

    fixture.detectChanges();

    comp = fixture.componentInstance.configurationSearchPage;
    searchConfigService = (comp as any).searchConfigService;
  });

  it('should set route parameters on init', () => {
    expect(comp.configuration).toBe(CONFIGURATION);
    expect(comp.fixedFilterQuery).toBe(QUERY);

    expect(routeService.setParameter).toHaveBeenCalledWith('configuration', CONFIGURATION);
    expect(routeService.setParameter).toHaveBeenCalledWith('fixedFilterQuery', QUERY);
  });

});
