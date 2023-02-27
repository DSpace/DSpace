import { Pipe, PipeTransform } from '@angular/core';

/**
 * Pipe that allows to iterate over an object and to access to entry key and value :
 *
 * <div *ngFor="let obj of objs | dsObjNgFor">
 *  {{obj.key}} - {{obj.value}}
 * </div>
 *
 */
@Pipe({
  name: 'dsObjNgFor'
})
export class ObjNgFor implements PipeTransform {
  transform(value: any, args: any[] = null): any {
    return Object.keys(value).map((key) => Object.assign({ key }, {value: value[key]}));
  }
}
