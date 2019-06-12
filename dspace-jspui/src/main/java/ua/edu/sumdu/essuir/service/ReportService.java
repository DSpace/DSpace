package ua.edu.sumdu.essuir.service;


import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import ua.edu.sumdu.essuir.entity.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;


@Service
public class ReportService {
    @Resource
    private DatabaseService databaseService;

    @Resource
    private SpecialityReportFetcher specialityReportFetcher;

    private BiPredicate<LocalDate, Pair<LocalDate, LocalDate>> isDateInRange = (date, range) -> date.isAfter(range.getLeft().minusDays(1)) && date.isBefore(range.getRight().plusDays(1));

    private List<Faculty> collectStatistics(Map<Depositor, Long> data) {
        Map<String, Faculty> result = new HashMap<>();
        for (Map.Entry<Depositor, Long> submission : data.entrySet()) {
            String facultyName = submission.getKey().getChairEntity().getFacultyEntityName();
            String chairName = submission.getKey().getChairEntity().getChairName();
            String specialityName = submission.getKey().getName();
            result.putIfAbsent(facultyName, new Faculty(facultyName));
            result.get(facultyName).addSubmission(chairName, specialityName, submission.getValue().intValue());
        }

        return new ArrayList<>(result.values());
    }

    public List<Faculty> getUsersSubmissionCountBetweenDates(LocalDate from, LocalDate to) {
        List<Item> items = databaseService.fetchItemsInArchive();
        Map<Depositor, Long> data = items.stream()
                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
                .collect(Collectors.groupingBy(Item::getSubmitter, Collectors.counting()));

        return collectStatistics(data);
    }

    public List<Item> getUploadedItemsByFacultyName(String faculty, LocalDate from, LocalDate to) {
        List<Item> items = databaseService.fetchItemsInArchive();

        return items
                .stream()
                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
                .filter(item -> faculty.equals(item.getSubmitter().getChairEntity().getFacultyEntityName()))
                .collect(Collectors.toList());
    }

    public List<Item> getUploadedItemsByChairName(String chair, LocalDate from, LocalDate to) {
        List<Item> items = databaseService.fetchItemsInArchive();

        return items
                .stream()
                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
                .filter(item -> chair.equals(item.getSubmitter().getChairEntity().getChairName()))
                .collect(Collectors.toList());
    }

    public List<Item> getUploadedItemsByPersonEmail(String person, LocalDate from, LocalDate to) {
        List<Item> items = databaseService.fetchItemsInArchive();

        return items
                .stream()
                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
                .filter(item -> person.equals(item.getSubmitter().getEmail()))
                .collect(Collectors.toList());
    }

    public List<Item> getItemsInSpeciality(String pattern, LocalDate from, LocalDate to) {
        return specialityReportFetcher.getItemsInSpeciality(pattern, from, to);
    }

    public List<Item> getBacheoursWithoutSpeciality() {
        return specialityReportFetcher.getBachelorsWithoutSpeciality();
    }

    public List<Faculty> getSpecialitySubmissionCountBetweenDates(LocalDate from, LocalDate to) {
        return collectStatistics(specialityReportFetcher.getSpecialitySubmissionCountBetweenDates(from, to));
    }
}
