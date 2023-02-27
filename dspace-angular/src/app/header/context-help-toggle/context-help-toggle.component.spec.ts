import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';

import { ContextHelpToggleComponent } from './context-help-toggle.component';
import { TranslateModule } from '@ngx-translate/core';
import { ContextHelpService } from '../../shared/context-help.service';
import { of as observableOf } from 'rxjs';
import { By } from '@angular/platform-browser';

describe('ContextHelpToggleComponent', () => {
  let component: ContextHelpToggleComponent;
  let fixture: ComponentFixture<ContextHelpToggleComponent>;
  let contextHelpService;

  beforeEach(async () => {
    contextHelpService = jasmine.createSpyObj('contextHelpService', [
      'tooltipCount$', 'toggleIcons'
    ]);
    contextHelpService.tooltipCount$.and.returnValue(observableOf(0));
    await TestBed.configureTestingModule({
      declarations: [ ContextHelpToggleComponent ],
      providers: [
        { provide: ContextHelpService, useValue: contextHelpService },
      ],
      imports: [ TranslateModule.forRoot() ]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ContextHelpToggleComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('if there are no elements on the page with a tooltip', () => {
    it('the toggle should not be visible', fakeAsync(() => {
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        expect(fixture.debugElement.query(By.css('div'))).toBeNull();
      });
    }));
  });

  describe('if there are elements on the page with a tooltip', () => {
    beforeEach(() => {
      contextHelpService.tooltipCount$.and.returnValue(observableOf(1));
      fixture.detectChanges();
    });

    it('clicking the button should toggle context help icon visibility', fakeAsync(() => {
      fixture.whenStable().then(() => {
        fixture.debugElement.query(By.css('a')).nativeElement.click();
        tick();
        expect(contextHelpService.toggleIcons).toHaveBeenCalled();
      });
    }));
  });

});
