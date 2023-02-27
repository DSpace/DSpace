import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { PageErrorComponent } from './page-error.component';
import { ActivatedRoute } from '@angular/router';
import { ActivatedRouteStub } from '../shared/testing/active-router.stub';
import { of as observableOf } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { TranslateLoaderMock } from '../shared/testing/translate-loader.mock';

describe('PageErrorComponent', () => {
  let component: PageErrorComponent;
  let fixture: ComponentFixture<PageErrorComponent>;
  const activatedRouteStub = Object.assign(new ActivatedRouteStub(), {
    queryParams: observableOf({
      status: 401,
      code: 'orcid.generic-error'
    })
  });
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ PageErrorComponent ],
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PageErrorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show error for 401 unauthorized', () => {
    const statusElement = fixture.debugElement.query(By.css('[data-test="status"]')).nativeElement;
    expect(statusElement.innerHTML).toEqual('401');
  });
});
