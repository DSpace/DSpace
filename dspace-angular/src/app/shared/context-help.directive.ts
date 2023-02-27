import {
  ComponentFactoryResolver,
  ComponentRef,
  Directive,
  Input,
  OnChanges,
  TemplateRef,
  ViewContainerRef,
  OnDestroy
} from '@angular/core';
import { PlacementArray } from '@ng-bootstrap/ng-bootstrap/util/positioning';
import { ContextHelpWrapperComponent } from './context-help-wrapper/context-help-wrapper.component';
import { PlacementDir } from './context-help-wrapper/placement-dir.model';
import { ContextHelpService } from './context-help.service';

export interface ContextHelpDirectiveInput {
  content: string;
  id: string;
  tooltipPlacement?: PlacementArray;
  iconPlacement?: PlacementDir;
}

/**
 * Directive to add a clickable tooltip icon to an element.
 * The tooltip icon's position is configurable ('left' or 'right')
 * and so is the position of the tooltip itself (PlacementArray).
 */
@Directive({
  selector: '[dsContextHelp]',
})
export class ContextHelpDirective implements OnChanges, OnDestroy {
  /**
   * Expects an object with the following fields:
   * - content: a string referring to an entry in the i18n files
   * - tooltipPlacement: a PlacementArray describing where the tooltip should expand, relative to the tooltip icon
   * - iconPlacement: a string 'left' or 'right', describing where the tooltip icon should be placed, relative to the element
   */
  @Input() dsContextHelp: ContextHelpDirectiveInput;
  mostRecentId: string | undefined = undefined;

  protected wrapper: ComponentRef<ContextHelpWrapperComponent>;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainerRef: ViewContainerRef,
    private componentFactoryResolver: ComponentFactoryResolver,
    private contextHelpService: ContextHelpService
  ) {}

  ngOnChanges() {
    this.clearMostRecentId();
    this.mostRecentId = this.dsContextHelp.id;
    this.contextHelpService.add({id: this.dsContextHelp.id, isTooltipVisible: false});

    if (this.wrapper === undefined) {
      const factory
        = this.componentFactoryResolver.resolveComponentFactory(ContextHelpWrapperComponent);
      this.wrapper = this.viewContainerRef.createComponent(factory);
    }
    this.wrapper.instance.templateRef = this.templateRef;
    this.wrapper.instance.content = this.dsContextHelp.content;
    this.wrapper.instance.id = this.dsContextHelp.id;
    this.wrapper.instance.tooltipPlacement = this.dsContextHelp.tooltipPlacement;
    this.wrapper.instance.iconPlacement = this.dsContextHelp.iconPlacement;
  }

  ngOnDestroy() {
    this.clearMostRecentId();
  }

  private clearMostRecentId(): void {
    if (this.mostRecentId !== undefined) {
      this.contextHelpService.remove(this.mostRecentId);
    }
  }
}
