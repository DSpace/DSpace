import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { of as observableOf } from 'rxjs';
import { TruncatablePartComponent } from './truncatable-part.component';
import { TruncatableService } from '../truncatable.service';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { getMockTranslateService } from '../../mocks/translate.service.mock';
import { TranslateLoaderMock } from '../../mocks/translate-loader.mock';
import { mockTruncatableService } from '../../mocks/mock-trucatable.service';
import { By } from '@angular/platform-browser';
import { NativeWindowRef, NativeWindowService } from '../../../core/services/window.service';

describe('TruncatablePartComponent', () => {
  let comp: TruncatablePartComponent;
  let fixture: ComponentFixture<TruncatablePartComponent>;
  let translateService: TranslateService;
  const id1 = '123';
  const id2 = '456';

  let truncatableService;
  const truncatableServiceStub: any = {
    isCollapsed: (id: string) => {
      if (id === id1) {
        return observableOf(true);
      } else {
        return observableOf(false);
      }
    }
  };
  beforeEach(waitForAsync(() => {
    translateService = getMockTranslateService();
    void TestBed.configureTestingModule({
      imports: [NoopAnimationsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
      ],
      declarations: [TruncatablePartComponent],
      providers: [
        { provide: NativeWindowService, useValue: new NativeWindowRef() },
        { provide: TruncatableService, useValue: truncatableServiceStub },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(TruncatablePartComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));
  beforeEach(() => {
    fixture = TestBed.createComponent(TruncatablePartComponent);
    comp = fixture.componentInstance; // TruncatablePartComponent test instance
    fixture.detectChanges();
    truncatableService = (comp as any).filterService;
  });

  describe('When the item is collapsed', () => {
    beforeEach(() => {
      comp.id = id1;
      comp.minLines = 5;
      (comp as any).setLines();
      fixture.detectChanges();
    })
    ;

    it('lines should equal minlines', () => {
      expect((comp as any).lines).toEqual(comp.minLines.toString());
    });

    it('collapseButton should be hidden', () => {
      const a = fixture.debugElement.query(By.css('.collapseButton'));
      expect(a).toBeNull();
    });
  });

  describe('When the item is expanded', () => {
    beforeEach(() => {
      comp.id = id2;
    })
    ;

    it('lines should equal maxlines when maxlines has a value', () => {
      comp.maxLines = 5;
      (comp as any).setLines();
      fixture.detectChanges();
      expect((comp as any).lines).toEqual(comp.maxLines.toString());
    });

    it('lines should equal \'none\' when maxlines has no value', () => {
      (comp as any).setLines();
      fixture.detectChanges();
      expect((comp as any).lines).toEqual('none');
    });

    it('collapseButton should be shown', () => {
      (comp as any).setLines();
      (comp as any).expandable = true;
      fixture.detectChanges();
      const a = fixture.debugElement.query(By.css('.collapseButton'));
      expect(a).not.toBeNull();
    });
  });
});

describe('TruncatablePartComponent', () => {
  let comp: TruncatablePartComponent;
  let fixture: ComponentFixture<TruncatablePartComponent>;
  let translateService: TranslateService;
  const identifier = '1234567890';
  let truncatableService;
  beforeEach(waitForAsync(() => {
    translateService = getMockTranslateService();
    void TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
      ],
      declarations: [TruncatablePartComponent],
      providers: [
        { provide: NativeWindowService, useValue: new NativeWindowRef() },
        { provide: TruncatableService, useValue: mockTruncatableService },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(TruncatablePartComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TruncatablePartComponent);
    comp = fixture.componentInstance; // TruncatablePartComponent test instance
    comp.id = identifier;
    fixture.detectChanges();
    truncatableService = (comp as any).service;
  });

  describe('When toggle is called', () => {
    beforeEach(() => {
      spyOn(truncatableService, 'toggle');
      comp.toggle();
    });

    it('should call toggle on the TruncatableService', () => {
      expect(truncatableService.toggle).toHaveBeenCalledWith(identifier);
    });
  });

});
