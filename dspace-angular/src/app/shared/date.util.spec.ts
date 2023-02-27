import { dateToString, dateToNgbDateStruct, dateToISOFormat, isValidDate, yearFromString } from './date.util';

describe('Date Utils', () => {

    describe('dateToISOFormat', () => {
        it('should convert Date to YYYY-MM-DDThh:mm:ssZ string', () => {
            // NOTE: month is zero indexed which is why it increases by one
            expect(dateToISOFormat(new Date(Date.UTC(2022, 5, 3)))).toEqual('2022-06-03T00:00:00Z');
        });
        it('should convert Date string to YYYY-MM-DDThh:mm:ssZ string', () => {
            expect(dateToISOFormat('2022-06-03')).toEqual('2022-06-03T00:00:00Z');
        });
        it('should convert Month string to YYYY-MM-DDThh:mm:ssZ string', () => {
            expect(dateToISOFormat('2022-06')).toEqual('2022-06-01T00:00:00Z');
        });
        it('should convert Year string to YYYY-MM-DDThh:mm:ssZ string', () => {
            expect(dateToISOFormat('2022')).toEqual('2022-01-01T00:00:00Z');
        });
        it('should convert ISO Date string to YYYY-MM-DDThh:mm:ssZ string', () => {
            // NOTE: Time is always zeroed out as proven by this test.
            expect(dateToISOFormat('2022-06-03T03:24:04Z')).toEqual('2022-06-03T00:00:00Z');
        });
        it('should convert NgbDateStruct to YYYY-MM-DDThh:mm:ssZ string', () => {
            // NOTE: month is zero indexed which is why it increases by one
            const date = new Date(Date.UTC(2022, 5, 3));
            expect(dateToISOFormat(dateToNgbDateStruct(date))).toEqual('2022-06-03T00:00:00Z');
        });
    });

    describe('dateToString', () => {
        it('should convert Date to YYYY-MM-DD string', () => {
            // NOTE: month is zero indexed which is why it increases by one
            expect(dateToString(new Date(Date.UTC(2022, 5, 3)))).toEqual('2022-06-03');
        });
        it('should convert Date with time to YYYY-MM-DD string', () => {
            // NOTE: month is zero indexed which is why it increases by one
            expect(dateToString(new Date(Date.UTC(2022, 5, 3, 3, 24, 0)))).toEqual('2022-06-03');
        });
        it('should convert Month only to YYYY-MM-DD string', () => {
            // NOTE: month is zero indexed which is why it increases by one
            expect(dateToString(new Date(Date.UTC(2022, 5)))).toEqual('2022-06-01');
        });
        it('should convert ISO Date to YYYY-MM-DD string', () => {
            expect(dateToString(new Date('2022-06-03T03:24:00Z'))).toEqual('2022-06-03');
        });
        it('should convert NgbDateStruct to YYYY-MM-DD string', () => {
            // NOTE: month is zero indexed which is why it increases by one
            const date = new Date(Date.UTC(2022, 5, 3));
            expect(dateToString(dateToNgbDateStruct(date))).toEqual('2022-06-03');
        });
    });


    describe('isValidDate', () => {
        it('should return false for null', () => {
            expect(isValidDate(null)).toBe(false);
        });
        it('should return false for empty string', () => {
            expect(isValidDate('')).toBe(false);
        });
        it('should return false for text', () => {
            expect(isValidDate('test')).toBe(false);
        });
        it('should return true for YYYY', () => {
            expect(isValidDate('2022')).toBe(true);
        });
        it('should return true for YYYY-MM', () => {
            expect(isValidDate('2022-12')).toBe(true);
        });
        it('should return true for YYYY-MM-DD', () => {
            expect(isValidDate('2022-06-03')).toBe(true);
        });
        it('should return true for YYYY-MM-DDTHH:MM:SS', () => {
            expect(isValidDate('2022-06-03T10:20:30')).toBe(true);
        });
        it('should return true for YYYY-MM-DDTHH:MM:SSZ', () => {
            expect(isValidDate('2022-06-03T10:20:30Z')).toBe(true);
        });
        it('should return false for a month that does not exist', () => {
            expect(isValidDate('2022-13')).toBe(false);
        });
        it('should return false for a day that does not exist', () => {
            expect(isValidDate('2022-02-60')).toBe(false);
        });
        it('should return false for a time that does not exist', () => {
            expect(isValidDate('2022-02-60T10:60:20')).toBe(false);
        });
    });

    describe('yearFromString', () => {
        it('should return year from YYYY string', () => {
            expect(yearFromString('2022')).toEqual(2022);
        });
        it('should return year from YYYY-MM string', () => {
            expect(yearFromString('1970-06')).toEqual(1970);
        });
        it('should return year from YYYY-MM-DD string', () => {
            expect(yearFromString('1914-10-23')).toEqual(1914);
        });
        it('should return year from YYYY-MM-DDTHH:MM:SSZ string', () => {
            expect(yearFromString('1914-10-23T10:20:30Z')).toEqual(1914);
        });
        it('should return null if invalid date', () => {
            expect(yearFromString('test')).toBeNull();
        });
    });
});
