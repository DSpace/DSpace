import { NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateLoaderMock } from '../../../../shared/mocks/translate-loader.mock';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentAccordionComponent } from './content-accordion.component';

import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { SherpaDataResponse } from '../../../../shared/mocks/section-sherpa-policies.service.mock';

describe('ContentAccordionComponent', () => {
  let component: ContentAccordionComponent;
  let fixture: ComponentFixture<ContentAccordionComponent>;
  let de: DebugElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
        NgbCollapseModule
      ],
      declarations: [ContentAccordionComponent]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ContentAccordionComponent);
    component = fixture.componentInstance;
    de = fixture.debugElement;
    component.isCollapsed = false;
    component.version = SherpaDataResponse.sherpaResponse.journals[0].policies[0].permittedVersions[0];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show 2 rows', () => {
    component.version = SherpaDataResponse.sherpaResponse.journals[0].policies[0].permittedVersions[0];
    fixture.detectChanges();
    expect(de.queryAll(By.css('.row')).length).toEqual(2);
  });

  it('should show 5 rows', () => {
    component.version = SherpaDataResponse.sherpaResponse.journals[0].policies[0].permittedVersions[2];
    fixture.detectChanges();
    expect(de.queryAll(By.css('.row')).length).toEqual(5);
  });
});
