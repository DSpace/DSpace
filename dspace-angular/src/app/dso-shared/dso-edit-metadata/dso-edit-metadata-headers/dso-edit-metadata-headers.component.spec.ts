import { DsoEditMetadataHeadersComponent } from './dso-edit-metadata-headers.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../../../shared/utils/var.directive';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';

describe('DsoEditMetadataHeadersComponent', () => {
  let component: DsoEditMetadataHeadersComponent;
  let fixture: ComponentFixture<DsoEditMetadataHeadersComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [DsoEditMetadataHeadersComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DsoEditMetadataHeadersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should display three headers', () => {
    expect(fixture.debugElement.queryAll(By.css('.ds-flex-cell')).length).toEqual(3);
  });
});
