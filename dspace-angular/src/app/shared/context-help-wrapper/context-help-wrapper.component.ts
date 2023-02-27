import { Component, Input, OnInit, TemplateRef, OnDestroy, ViewChild } from '@angular/core';
import { PlacementArray } from '@ng-bootstrap/ng-bootstrap/util/positioning';
import { TranslateService } from '@ngx-translate/core';
import { Observable, Subscription, BehaviorSubject, combineLatest } from 'rxjs';
import { map, distinctUntilChanged, mergeMap } from 'rxjs/operators';
import { PlacementDir } from './placement-dir.model';
import { ContextHelpService } from '../context-help.service';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { hasValueOperator } from '../empty.util';
import { ContextHelp } from '../context-help.model';

type ParsedContent = (string | {href: string, text: string})[];

/**
 * This component renders an info icon next to the wrapped element which
 * produces a tooltip when clicked.
 */
@Component({
  selector: 'ds-context-help-wrapper',
  templateUrl: './context-help-wrapper.component.html',
  styleUrls: ['./context-help-wrapper.component.scss'],
})
export class ContextHelpWrapperComponent implements OnInit, OnDestroy {
  /**
   * Template reference for the wrapped element.
   */
  @Input() templateRef: TemplateRef<any>;

  /**
   * Identifier for the context help tooltip.
   */
  @Input() id: string;

  /**
   * Indicate where the tooltip should show up, relative to the info icon.
   */
  @Input() tooltipPlacement?: PlacementArray = [];

  /**
   * Indicate whether the info icon should appear to the left or to
   * the right of the wrapped element.
   */
  @Input() iconPlacement?: PlacementDir = 'left';

  /**
   * If true, don't process text to render links.
   */
  @Input() set dontParseLinks(dont: boolean) {
    this.dontParseLinks$.next(dont);
  }
  private dontParseLinks$: BehaviorSubject<boolean> = new BehaviorSubject(false);

  shouldShowIcon$: Observable<boolean>;

  tooltip: NgbTooltip;

  @Input() set content(translateKey: string) {
    this.content$.next(translateKey);
  }
  private content$: BehaviorSubject<string | undefined> = new BehaviorSubject(undefined);

  parsedContent$: Observable<ParsedContent>;

  private subs: Subscription[] = [];

  constructor(
    private translateService: TranslateService,
    private contextHelpService: ContextHelpService
  ) { }

  ngOnInit() {
    this.parsedContent$ = combineLatest([
      this.content$.pipe(distinctUntilChanged(), mergeMap(translateKey => this.translateService.get(translateKey))),
      this.dontParseLinks$.pipe(distinctUntilChanged())
    ]).pipe(
      map(([text, dontParseLinks]) =>
        dontParseLinks ? [text] : this.parseLinks(text))
    );
    this.shouldShowIcon$ = this.contextHelpService.shouldShowIcons$();
  }

  @ViewChild('tooltip', { static: false }) set setTooltip(tooltip: NgbTooltip) {
    this.tooltip = tooltip;
    this.clearSubs();
    if (this.tooltip !== undefined) {
      this.subs = [
        this.contextHelpService.getContextHelp$(this.id)
          .pipe(hasValueOperator())
          .subscribe((ch: ContextHelp) => {

            if (ch.isTooltipVisible && !this.tooltip.isOpen()) {
              this.tooltip.open();
            } else if (!ch.isTooltipVisible && this.tooltip.isOpen()) {
              this.tooltip.close();
            }
          }),

        this.tooltip.shown.subscribe(() => {
          this.contextHelpService.showTooltip(this.id);
        }),

        this.tooltip.hidden.subscribe(() => {
          this.contextHelpService.hideTooltip(this.id);
        })
      ];
    }
  }

  ngOnDestroy() {
    this.clearSubs();
  }

  onClick() {
    this.contextHelpService.toggleTooltip(this.id);
  }

  /**
   * Parses Markdown-style links, splitting up a given text
   * into link-free pieces of text and objects of the form
   * {href: string, text: string} (which represent links).
   * This function makes no effort to check whether the href is a
   * correct URL. Currently, this function does not support escape
   * characters: its behavior when given a string containing square
   * brackets that do not deliminate a link is undefined.
   * Regular parentheses outside of links do work, however.
   *
   * For example:
   * parseLinks("This is text, [this](https://google.com) is a link, and [so is this](https://youtube.com)")
   * =
   * [ "This is text, ",
   *   {href: "https://google.com", text: "this"},
   *   " is a link, and ",
   *   {href: "https://youtube.com", text: "so is this"}
   * ]
   */
  private parseLinks(text: string): ParsedContent {
    // Implementation note: due to `matchAll` method on strings not being available for all versions,
    // separate "split" and "parse" steps are needed.

    // We use splitRegexp (the outer `match` call) to split the text
    // into link-free pieces of text (matched by /[^\[]+/) and pieces
    // of text of the form "[some link text](some.link.here)" (matched
    // by /\[([^\]]*)\]\(([^\)]*)\)/)
    const splitRegexp = /[^\[]+|\[([^\]]*)\]\(([^\)]*)\)/g;

    // Once the array is split up in link-representing strings and
    // non-link-representing strings, we use parseRegexp (the inner
    // `match` call) to transform the link-representing strings into
    // {href: string, text: string} objects.
    const parseRegexp = /^\[([^\]]*)\]\(([^\)]*)\)$/;

    return text.match(splitRegexp).map((substring: string) => {
      const match = substring.match(parseRegexp);
      return match === null
        ? substring
        : ({href: match[2], text: match[1]});
    });
  }

  private clearSubs() {
    this.subs.forEach(sub => sub.unsubscribe());
    this.subs = [];
  }
}
