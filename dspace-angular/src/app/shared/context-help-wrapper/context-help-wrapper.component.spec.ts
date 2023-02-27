import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { of as observableOf, BehaviorSubject } from 'rxjs';
import { ContextHelpWrapperComponent } from './context-help-wrapper.component';
import { NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';
import { ContextHelpService } from '../context-help.service';
import { ContextHelp } from '../context-help.model';
import { Component, Input, DebugElement } from '@angular/core';
import { PlacementArray } from '@ng-bootstrap/ng-bootstrap/util/positioning';
import { PlacementDir } from './placement-dir.model';
import { By } from '@angular/platform-browser';

@Component({
  template: `
    <ng-template #div>template</ng-template>
    <ds-context-help-wrapper
      #chwrapper
      [templateRef]="div"
      [content]="content"
      [id]="id"
      [tooltipPlacement]="tooltipPlacement"
      [iconPlacement]="iconPlacement"
      [dontParseLinks]="dontParseLinks"
    >
    </ds-context-help-wrapper>
  `
})
class TemplateComponent {
  @Input() content: string;
  @Input() id: string;
  @Input() tooltipPlacement?: PlacementArray;
  @Input() iconPlacement?: PlacementDir;
  @Input() dontParseLinks?: boolean;
}

const messages = {
  lorem: 'lorem ipsum dolor sit amet',
  linkTest: 'This is text, [this](https://dspace.lyrasis.org/) is a link, and [so is this](https://google.com/)'
};
const exampleContextHelp: ContextHelp = {
  id: 'test-tooltip',
  isTooltipVisible: false
};

describe('ContextHelpWrapperComponent', () => {
  let templateComponent: TemplateComponent;
  let wrapperComponent: ContextHelpWrapperComponent;
  let fixture: ComponentFixture<TemplateComponent>;
  let el: DebugElement;
  let translateService: any;
  let contextHelpService: any;
  let getContextHelp$: BehaviorSubject<ContextHelp>;
  let shouldShowIcons$: BehaviorSubject<boolean>;

  function makeWrappedElement(): HTMLElement {
    const wrapped: HTMLElement = document.createElement('div');
    wrapped.innerHTML = 'example element';
    return wrapped;
  }

  beforeEach(waitForAsync( () => {
    translateService = jasmine.createSpyObj('translateService', ['get']);
    contextHelpService = jasmine.createSpyObj('contextHelpService', [
      'shouldShowIcons$',
      'getContextHelp$',
      'add',
      'remove',
      'toggleIcons',
      'toggleTooltip',
      'showTooltip',
      'hideTooltip'
    ]);

    TestBed.configureTestingModule({
      imports: [ NgbTooltipModule ],
      providers: [
        { provide: TranslateService, useValue: translateService },
        { provide: ContextHelpService, useValue: contextHelpService },
      ],
      declarations: [ TemplateComponent, ContextHelpWrapperComponent ]
    }).compileComponents();
  }));

  beforeEach(() => {
    // Initializing services.
    getContextHelp$ = new BehaviorSubject<ContextHelp>(exampleContextHelp);
    shouldShowIcons$ = new BehaviorSubject<boolean>(false);
    contextHelpService.getContextHelp$.and.returnValue(getContextHelp$);
    contextHelpService.shouldShowIcons$.and.returnValue(shouldShowIcons$);
    translateService.get.and.callFake((content) => observableOf(messages[content]));

    getContextHelp$.next(exampleContextHelp);
    shouldShowIcons$.next(false);

    // Initializing components.
    fixture = TestBed.createComponent(TemplateComponent);
    el = fixture.debugElement;
    templateComponent = fixture.componentInstance;
    templateComponent.content = 'lorem';
    templateComponent.id = 'test-tooltip';
    templateComponent.tooltipPlacement = ['bottom'];
    templateComponent.iconPlacement = 'left';
    wrapperComponent = el.query(By.css('ds-context-help-wrapper')).componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(templateComponent).toBeDefined();
    expect(wrapperComponent).toBeDefined();
  });

  it('should not show the context help icon while icon visibility is not turned on', (done) => {
    fixture.whenStable().then(() => {
      const wrapper = el.query(By.css('ds-context-help-wrapper')).nativeElement;
      expect(wrapper.children.length).toBe(0);
      done();
    });
  });

  describe('when icon visibility is turned on', () => {
    beforeEach(() => {
      shouldShowIcons$.next(true);
      fixture.detectChanges();
      spyOn(wrapperComponent.tooltip, 'open').and.callThrough();
      spyOn(wrapperComponent.tooltip, 'close').and.callThrough();
    });

    it('should show the context help button', (done) => {
      fixture.whenStable().then(() => {
        const wrapper = el.query(By.css('ds-context-help-wrapper')).nativeElement;
        expect(wrapper.children.length).toBe(1);
        const [i] = wrapper.children;
        expect(i.tagName).toBe('I');
        done();
      });
    });

    describe('after the icon is clicked', () => {
      let i;
      beforeEach(() => {
        i = el.query(By.css('.ds-context-help-icon')).nativeElement;
        i.click();
        fixture.detectChanges();
      });

      it('should display the tooltip', () => {
        expect(contextHelpService.toggleTooltip).toHaveBeenCalledWith('test-tooltip');
        getContextHelp$.next({...exampleContextHelp, isTooltipVisible: true});
        fixture.detectChanges();
        expect(wrapperComponent.tooltip.open).toHaveBeenCalled();
        expect(wrapperComponent.tooltip.close).toHaveBeenCalledTimes(0);
        expect(fixture.debugElement.query(By.css('.ds-context-help-content')).nativeElement.textContent)
          .toMatch(/\s*lorem ipsum dolor sit amet\s*/);
      });

      it('should correctly display links', () => {
        templateComponent.content = 'linkTest';
        getContextHelp$.next({...exampleContextHelp, isTooltipVisible: true});
        fixture.detectChanges();
        const nodeList: NodeList = fixture.debugElement.query(By.css('.ds-context-help-content'))
          .nativeElement
          .childNodes;
        const relevantNodes = Array.from(nodeList).filter(node => node.nodeType !== Node.COMMENT_NODE);
        expect(relevantNodes.length).toBe(4);

        const [text1, link1, text2, link2] = relevantNodes;

        expect(text1.nodeType).toBe(Node.TEXT_NODE);
        expect(text1.nodeValue).toMatch(/\s* This is text, \s*/);

        expect(link1.nodeName).toBe('A');
        expect((link1 as any).href).toBe('https://dspace.lyrasis.org/');
        expect(link1.textContent).toBe('this');

        expect(text2.nodeType).toBe(Node.TEXT_NODE);
        expect(text2.nodeValue).toMatch(/\s* is a link, and \s*/);

        expect(link2.nodeName).toBe('A');
        expect((link2 as any).href).toBe('https://google.com/');
        expect(link2.textContent).toBe('so is this');
      });

      it('should not display links if specified not to', () => {
        templateComponent.dontParseLinks = true;
        templateComponent.content = 'linkTest';
        getContextHelp$.next({...exampleContextHelp, isTooltipVisible: true});
        fixture.detectChanges();


        const nodeList: NodeList = fixture.debugElement.query(By.css('.ds-context-help-content'))
          .nativeElement
          .childNodes;
        const relevantNodes = Array.from(nodeList).filter(node => node.nodeType !== Node.COMMENT_NODE);
        expect(relevantNodes.length).toBe(1);

        const [text] = relevantNodes;

        expect(text.nodeType).toBe(Node.TEXT_NODE);
        expect(text.nodeValue).toMatch(
          /\s* This is text, \[this\]\(https:\/\/dspace.lyrasis.org\/\) is a link, and \[so is this\]\(https:\/\/google.com\/\) \s*/);
      });

      describe('after the icon is clicked again', () => {
        beforeEach(() => {
          i.click();
          fixture.detectChanges();
          spyOn(wrapperComponent.tooltip, 'isOpen').and.returnValue(true);
        });

        it('should close the tooltip', () => {
          expect(contextHelpService.toggleTooltip).toHaveBeenCalledWith('test-tooltip');
          getContextHelp$.next({...exampleContextHelp, isTooltipVisible: false});
          fixture.detectChanges();
          expect(wrapperComponent.tooltip.close).toHaveBeenCalled();
        });
      });
    });
  });
});
