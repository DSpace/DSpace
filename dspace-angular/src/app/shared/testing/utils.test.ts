import { ComponentFixture, TestBed } from '@angular/core/testing';
import { buildPaginatedList, PaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { Observable } from 'rxjs/internal/Observable';
import { of as observableOf } from 'rxjs/internal/observable/of';
import { UnCacheableObject } from '../../core/shared/uncacheable-object.model';
import { RequestEntryState } from '../../core/data/request-entry-state.model';
import { RequestEntry } from '../../core/data/request-entry.model';

/**
 * Returns true if a Native Element has a specified css class.
 *
 * @param element
 *    the Native Element
 * @param className
 *    the class name to find
 */
export const hasClass = (element: any, className: string): boolean => {
  const classes = element.getAttribute('class');
  return classes.split(' ').indexOf(className) !== -1;
};

/**
 * Creates an instance of a component and returns test fixture.
 *
 * @param html
 *    the component's template as html
 * @param type
 *    the type of the component to instantiate
 */
export const createTestComponent = <T>(html: string, type: new (...args: any[]) => T ): ComponentFixture<T> => {
  TestBed.overrideComponent(type, {
    set: { template: html }
  });
  const fixture = TestBed.createComponent(type);

  fixture.detectChanges();
  return fixture as ComponentFixture<T>;
};

/**
 * Allows you to spy on a read only property
 *
 * @param obj
 *    The object to spy on
 * @param prop
 *    The property to spy on
 */
export function spyOnOperator(obj: any, prop: string): any {
  const oldProp = obj[prop];
  Object.defineProperty(obj, prop, {
    configurable: true,
    enumerable: true,
    value: oldProp,
    writable: true
  });

  return spyOn(obj, prop);
}

/**
 * Method to create a paginated list for an array of objects
 * @param objects An array representing the paginated list's page
 */
export function createPaginatedList<T>(objects?: T[]): PaginatedList<T> {
  return buildPaginatedList(new PageInfo(), objects);
}

/**
 * Creates a jasmine spy for an exported function
 * @param target The object to spy on
 * @param prop The property/function to spy on
 */
export function spyOnExported<T>(target: T, prop: keyof T): jasmine.Spy {
  const spy = jasmine.createSpy(`${prop}Spy`);
  spyOnProperty(target, prop).and.returnValue(spy);
  return spy;
}

/**
 * Create a mock request entry to be used in testing
 * @param unCacheableObject
 * @param statusCode
 * @param errorMessage
 */
export function createRequestEntry$(unCacheableObject?: UnCacheableObject, statusCode = 200, errorMessage?: string): Observable<RequestEntry> {
  return observableOf({
    request: undefined,
    state: RequestEntryState.Success,
    response: {
      timeCompleted: new Date().getTime(),
      statusCode,
      errorMessage,
      unCacheableObject
    },
    lastUpdated: new Date().getTime()
  });
}

/**
 * Get the argument (method parameter) a Spy method got called with first
 * Example case:
 * - We spy on method mock(testNumber: number)
 * - During the tests, mock gets called 3 times with numbers 8, 5 and 7
 * - This function will return 8, as it's the first number mock got called with
 * @param spyMethod     The method that got spied on
 * @param argumentIndex The index of the argument, only necessary if the spy method contains more than one parameter
 */
export function getFirstUsedArgumentOfSpyMethod(spyMethod: jasmine.Spy, argumentIndex: number = 0): any {
  return spyMethod.calls.argsFor(0)[argumentIndex];
}
