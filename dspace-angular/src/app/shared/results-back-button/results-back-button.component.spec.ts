import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ResultsBackButtonComponent } from './results-back-button.component';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

describe('ResultsBackButtonComponent', () => {

  let component: ResultsBackButtonComponent;
  let fixture: ComponentFixture<ResultsBackButtonComponent>;

  const translate = {
    get: jasmine.createSpy('get').and.returnValue(of('item button')),
  };

  describe('back to results', () => {


    beforeEach(waitForAsync(() => {
      TestBed.configureTestingModule({
        declarations: [ResultsBackButtonComponent],
        imports: [TranslateModule.forRoot(),
          RouterTestingModule.withRoutes([])
        ],
        providers: [
          { provide: TranslateService, useValue: translate }
        ],
        schemas: [NO_ERRORS_SCHEMA]
      }).compileComponents();
    }));

    describe('from a metadata browse list', () => {

      beforeEach(waitForAsync(() => {
        translate.get.calls.reset();
        fixture = TestBed.createComponent(ResultsBackButtonComponent);
        component = fixture.componentInstance;
        component.buttonLabel = of('browse button');
        component.ngOnInit();
        fixture.detectChanges();
      }));

      it('should have back from browse label', () => {
        expect(translate.get).not.toHaveBeenCalled();
        const el = fixture.debugElement.nativeElement;
        expect(el.innerHTML).toContain('browse button');
      });

    });

    describe('from an item', () => {

      beforeEach(waitForAsync(() => {
        translate.get.calls.reset();
        fixture = TestBed.createComponent(ResultsBackButtonComponent);
        component = fixture.componentInstance;
        component.ngOnInit();
        fixture.detectChanges();
      }));

      it('should set item label by default', () => {
        expect(translate.get).toHaveBeenCalledWith('search.browse.item-back');
        const el = fixture.debugElement.nativeElement;
        expect(el.innerHTML).toContain('item button');
      });

    });

  });

});

