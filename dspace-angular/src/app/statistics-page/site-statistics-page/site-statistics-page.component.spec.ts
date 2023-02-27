import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { SiteStatisticsPageComponent } from './site-statistics-page.component';
import { StatisticsTableComponent } from '../statistics-table/statistics-table.component';
import { TranslateModule } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UsageReportDataService } from '../../core/statistics/usage-report-data.service';
import { of as observableOf } from 'rxjs';
import { Site } from '../../core/shared/site.model';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { UsageReport } from '../../core/statistics/models/usage-report.model';
import { SharedModule } from '../../shared/shared.module';
import { CommonModule } from '@angular/common';
import { DSONameService } from '../../core/breadcrumbs/dso-name.service';
import { DSpaceObjectDataService } from '../../core/data/dspace-object-data.service';
import { SiteDataService } from '../../core/data/site-data.service';
import { AuthService } from '../../core/auth/auth.service';

describe('SiteStatisticsPageComponent', () => {

  let component: SiteStatisticsPageComponent;
  let de: DebugElement;
  let fixture: ComponentFixture<SiteStatisticsPageComponent>;

  beforeEach(waitForAsync(() => {

    const activatedRoute = {
    };

    const router = {
    };

    const usageReportService = {
      searchStatistics: () => observableOf([
        Object.assign(
          new UsageReport(), {
            id: `site_id-TotalVisits-report`,
            points: [],
          }
        ),
      ]),
    };

    const nameService = {
      getName: () => observableOf('test dso name'),
    };

    const siteService = {
      find: () => observableOf(Object.assign(new Site(), {
        id: 'site_id',
        _links: {
          self: {
            href: 'test_site_link',
          },
        },
      }))
    };

    const authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true),
      setRedirectUrl: {}
    });

    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
        CommonModule,
        SharedModule,
      ],
      declarations: [
        SiteStatisticsPageComponent,
        StatisticsTableComponent,
      ],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: Router, useValue: router },
        { provide: UsageReportDataService, useValue: usageReportService },
        { provide: DSpaceObjectDataService, useValue: {} },
        { provide: DSONameService, useValue: nameService },
        { provide: SiteDataService, useValue: siteService },
        { provide: AuthService, useValue: authService },
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SiteStatisticsPageComponent);
    component = fixture.componentInstance;
    de = fixture.debugElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should resolve to the correct site', () => {
    expect(de.query(By.css('.header')).nativeElement.id)
      .toEqual('site_id');
  });

  it('should show a statistics table for each usage report', () => {
    expect(de.query(By.css('ds-statistics-table.site_id-TotalVisits-report')).nativeElement)
      .toBeTruthy();
  });
});
