import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { TranslateLoaderMock } from '../mocks/translate-loader.mock';
import { SearchService } from '../../core/shared/search/search.service';
import { ViewModeSwitchComponent } from './view-mode-switch.component';
import { SearchServiceStub } from '../testing/search-service.stub';
import { ViewMode } from '../../core/shared/view-mode.model';
import { BrowserOnlyMockPipe } from '../testing/browser-only-mock.pipe';

@Component({ template: '' })
class DummyComponent {
}

describe('ViewModeSwitchComponent', () => {
  let comp: ViewModeSwitchComponent;
  let fixture: ComponentFixture<ViewModeSwitchComponent>;
  const searchService = new SearchServiceStub();
  let listButton: HTMLElement;
  let gridButton: HTMLElement;
  let detailButton: HTMLElement;
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
        RouterTestingModule.withRoutes([
          { path: 'search', component: DummyComponent, pathMatch: 'full' },
        ])
      ],
      declarations: [
        ViewModeSwitchComponent,
        DummyComponent,
        BrowserOnlyMockPipe,
      ],
      providers: [
        { provide: SearchService, useValue: searchService },
      ],
    }).overrideComponent(ViewModeSwitchComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ViewModeSwitchComponent);
    comp = fixture.componentInstance; // ViewModeSwitchComponent test instance
    spyOn(comp.changeViewMode, 'emit');
  });

  describe('', () => {
    beforeEach(fakeAsync(() => {
      comp.viewModeList = [ViewMode.ListElement, ViewMode.GridElement];
      comp.currentMode = ViewMode.ListElement;
      searchService.setViewMode(ViewMode.ListElement);
      tick();
      fixture.detectChanges();
      const debugElements = fixture.debugElement.queryAll(By.css('a'));
      listButton = debugElements[0].nativeElement;
      gridButton = debugElements[1].nativeElement;
    }));

    it('should set list button as active when on list mode', fakeAsync(() => {
      comp.switchViewTo(ViewMode.ListElement);
      expect(comp.changeViewMode.emit).not.toHaveBeenCalled();
      tick();
      fixture.detectChanges();
      expect(comp.currentMode).toBe(ViewMode.ListElement);
      expect(listButton.classList).toContain('active');
      expect(gridButton.classList).not.toContain('active');

    }));

    it('should set grid button as active when on grid mode', fakeAsync(() => {
      comp.switchViewTo(ViewMode.GridElement);
      expect(comp.changeViewMode.emit).toHaveBeenCalledWith(ViewMode.GridElement);
      tick();
      fixture.detectChanges();
      expect(comp.currentMode).toBe(ViewMode.GridElement);
      expect(listButton.classList).not.toContain('active');
      expect(gridButton.classList).toContain('active');
    }));
  });


  describe('', () => {
    beforeEach(fakeAsync(() => {
      comp.viewModeList = [ViewMode.ListElement, ViewMode.DetailedListElement];
      comp.currentMode = ViewMode.ListElement;
      searchService.setViewMode(ViewMode.ListElement);
      tick();
      fixture.detectChanges();
      const debugElements = fixture.debugElement.queryAll(By.css('a'));
      listButton = debugElements[0].nativeElement;
      detailButton = debugElements[1].nativeElement;
    }));

    it('should set list button as active when on list mode', fakeAsync(() => {
      comp.switchViewTo(ViewMode.ListElement);
      expect(comp.changeViewMode.emit).not.toHaveBeenCalled();
      tick();
      fixture.detectChanges();
      expect(comp.currentMode).toBe(ViewMode.ListElement);
      expect(listButton.classList).toContain('active');
      expect(detailButton.classList).not.toContain('active');
    }));

    it('should set detail button as active when on detailed mode', fakeAsync(() => {
      comp.switchViewTo(ViewMode.DetailedListElement);
      expect(comp.changeViewMode.emit).toHaveBeenCalledWith(ViewMode.DetailedListElement);
      tick();
      fixture.detectChanges();
      expect(comp.currentMode).toBe(ViewMode.DetailedListElement);
      expect(listButton.classList).not.toContain('active');
      expect(detailButton.classList).toContain('active');
    }));
  });


});
