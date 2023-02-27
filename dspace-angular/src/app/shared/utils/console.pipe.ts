import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'dsConsole'
})
export class ConsolePipe implements PipeTransform {
  transform(value: any): string {
    console.log(value);
    return '';
  }
}
