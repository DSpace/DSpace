import { SearchResult } from '../../search/models/search-result.model';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { SearchResultListElementComponent } from '../search-result-list-element/search-result-list-element.component';
import { Component } from '@angular/core';
import { hasValue, isNotEmpty } from '../../empty.util';
import { Observable, of as observableOf } from 'rxjs';
import { TruncatableService } from '../../truncatable/truncatable.service';
import { LinkService } from '../../../core/cache/builders/link.service';
import { find, map } from 'rxjs/operators';
import { ChildHALResource } from '../../../core/shared/child-hal-resource.model';
import { followLink } from '../../utils/follow-link-config.model';
import { RemoteData } from '../../../core/data/remote-data';
import { Context } from '../../../core/shared/context.model';
import { DSONameService } from '../../../core/breadcrumbs/dso-name.service';

@Component({
  selector: 'ds-sidebar-search-list-element',
  templateUrl: './sidebar-search-list-element.component.html'
})
/**
 * Component displaying a list element for a {@link SearchResult} in the sidebar search modal
 * It displays the name of the parent, title and description of the object. All of which are customizable in the child
 * component by overriding the relevant methods of this component
 */
export class SidebarSearchListElementComponent<T extends SearchResult<K>, K extends DSpaceObject> extends SearchResultListElementComponent<T, K> {
  /**
   * Observable for the title of the parent object (displayed above the object's title)
   */
  parentTitle$: Observable<string>;

  /**
   * A description to display below the title
   */
  description: string;

  public constructor(protected truncatableService: TruncatableService,
                     protected linkService: LinkService,
                     protected dsoNameService: DSONameService
  ) {
    super(truncatableService, dsoNameService, null);
  }

  /**
   * Initialise the component variables
   */
  ngOnInit(): void {
    super.ngOnInit();
    if (hasValue(this.dso)) {
      this.parentTitle$ = this.getParentTitle();
      this.description = this.getDescription();
    }
  }

  /**
   * returns true if this element represents the current dso
   */
  isCurrent(): boolean {
    return this.context === Context.SideBarSearchModalCurrent;
  }

  /**
   * Get the title of the object's parent
   * Retrieve the parent by using the object's parent link and retrieving its 'dc.title' metadata
   */
  getParentTitle(): Observable<string> {
    return this.getParent().pipe(
      map((parentRD: RemoteData<DSpaceObject>) => {
        return hasValue(parentRD) && hasValue(parentRD.payload) ? this.dsoNameService.getName(parentRD.payload) : undefined;
      })
    );
  }

  /**
   * Get the parent of the object
   */
  getParent(): Observable<RemoteData<DSpaceObject>> {
    if (typeof (this.dso as any).getParentLinkKey === 'function') {
      const propertyName = (this.dso as any).getParentLinkKey();
      return this.linkService.resolveLink(this.dso, followLink(propertyName))[propertyName].pipe(
        find((parentRD: RemoteData<ChildHALResource & DSpaceObject>) => parentRD.hasSucceeded || parentRD.statusCode === 204)
      );
    }
    return observableOf(undefined);
  }

  /**
   * Get the description of the object
   * Default: "(dc.publisher, dc.date.issued) authors"
   */
  getDescription(): string {
    const publisher = this.firstMetadataValue('dc.publisher');
    const date = this.firstMetadataValue('dc.date.issued');
    const authors = this.allMetadataValues(['dc.contributor.author', 'dc.creator', 'dc.contributor.*']);
    let description = '';
    if (isNotEmpty(publisher) || isNotEmpty(date)) {
      description += '(';
    }
    if (isNotEmpty(publisher)) {
      description += publisher;
    }
    if (isNotEmpty(date)) {
      if (isNotEmpty(publisher)) {
        description += ', ';
      }
      description += date;
    }
    if (isNotEmpty(description)) {
      description += ') ';
    }
    if (isNotEmpty(authors)) {
      authors.forEach((author, i) => {
        description += author;
        if (i < (authors.length - 1)) {
          description += '; ';
        }
      });
    }
    return this.undefinedIfEmpty(description);
  }

  /**
   * Return undefined if the provided string is empty
   * @param value Value to check
   */
  undefinedIfEmpty(value: string) {
    return this.defaultIfEmpty(value, undefined);
  }

  /**
   * Return a default value if the provided string is empty
   * @param value Value to check
   * @param def   Default in case value is empty
   */
  defaultIfEmpty(value: string, def: string) {
    if (isNotEmpty(value)) {
      return value;
    } else {
      return def;
    }
  }
}
