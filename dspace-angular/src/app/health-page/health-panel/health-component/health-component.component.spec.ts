import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';

import { HealthComponentComponent } from './health-component.component';
import { HealthComponentOne, HealthComponentTwo } from '../../../shared/mocks/health-endpoint.mocks';
import { ObjNgFor } from '../../../shared/utils/object-ngfor.pipe';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateLoaderMock } from '../../../shared/mocks/translate-loader.mock';

describe('HealthComponentComponent', () => {
  let component: HealthComponentComponent;
  let fixture: ComponentFixture<HealthComponentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        NgbCollapseModule,
        NoopAnimationsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      declarations: [
        HealthComponentComponent,
        ObjNgFor
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HealthComponentComponent);
    component = fixture.componentInstance;
  });

  describe('when has nested components', () => {
    beforeEach(() => {
      component.healthComponentName = 'db';
      component.healthComponent = HealthComponentOne;
      component.isCollapsed = false;
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should create collapsible divs properly', () => {
      const collapseDivs = fixture.debugElement.queryAll(By.css('[data-test="collapse"]'));
      expect(collapseDivs.length).toBe(2);
      const detailsDivs = fixture.debugElement.queryAll(By.css('[data-test="details"]'));
      expect(detailsDivs.length).toBe(6);
    });
  });

  describe('when has details', () => {
    beforeEach(() => {
      component.healthComponentName = 'geoIp';
      component.healthComponent = HealthComponentTwo;

      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should create detail divs properly', () => {
      const detailsDivs = fixture.debugElement.queryAll(By.css('[data-test="details"]'));
      expect(detailsDivs.length).toBe(1);
      const collapseDivs = fixture.debugElement.queryAll(By.css('[data-test="collapse"]'));
      expect(collapseDivs.length).toBe(0);
    });
  });
});
