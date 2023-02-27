import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { NgbAccordionModule, NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import { TranslateLoaderMock } from '../../shared/mocks/translate-loader.mock';
import { HealthPanelComponent } from './health-panel.component';
import { HealthResponseObj } from '../../shared/mocks/health-endpoint.mocks';
import { ObjNgFor } from '../../shared/utils/object-ngfor.pipe';

describe('HealthPanelComponent', () => {
  let component: HealthPanelComponent;
  let fixture: ComponentFixture<HealthPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NgbNavModule,
        NgbAccordionModule,
        CommonModule,
        BrowserAnimationsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
      ],
      declarations: [
        HealthPanelComponent,
        ObjNgFor
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HealthPanelComponent);
    component = fixture.componentInstance;
    component.healthResponse = HealthResponseObj;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render a panel for each component', () => {
    const components = fixture.debugElement.queryAll(By.css('[data-test="component"]'));
    expect(components.length).toBe(5);
  });

});
