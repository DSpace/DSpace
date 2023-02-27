import { TranslateLoaderMock } from '../../../../shared/testing/translate-loader.mock';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MetadataInformationComponent } from './metadata-information.component';

import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { SherpaDataResponse } from '../../../../shared/mocks/section-sherpa-policies.service.mock';

describe('MetadataInformationComponent', () => {
  let component: MetadataInformationComponent;
  let fixture: ComponentFixture<MetadataInformationComponent>;
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
      ],
      declarations: [MetadataInformationComponent]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MetadataInformationComponent);
    component = fixture.componentInstance;
    de = fixture.debugElement;
    component.metadata = SherpaDataResponse.sherpaResponse.metadata;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show 4 rows', () => {
    expect(de.queryAll(By.css('.row')).length).toEqual(4);
  });

});
