package org.ssu.service;


import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.jooq.lambda.Seq;
import org.springframework.stereotype.Service;
import org.ssu.entity.ChairEntity;
import org.ssu.entity.EssuirEperson;
import org.ssu.entity.response.DepositorDivision;
import org.ssu.entity.response.DepositorSimpleUnit;
import org.ssu.entity.response.ItemDepositorResponse;

import javax.annotation.Resource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class ReportService {
    @Resource
    private ItemService essuirItemService;
    @Resource
    private EpersonService epersonService;

    private BiPredicate<LocalDate, Pair<LocalDate, LocalDate>> isDateInRange = (date, range) -> date.isAfter(range.getLeft().minusDays(1)) && date.isBefore(range.getRight().plusDays(1));

    private <T extends DepositorSimpleUnit> List<ItemDepositorResponse> collectStatistics(List<Pair<T, Long>> data) {
        Function<Pair<T, Long>, ItemDepositorResponse> countContributionForSpeciality = (speciality) -> new ItemDepositorResponse.Builder()
                .withName(speciality.getKey().getName())
                .withCount(speciality.getValue().intValue())
                .build();

        BiFunction<DepositorDivision, List<ItemDepositorResponse>, ItemDepositorResponse> fetchDepositorDataForDepositorDivision = (depositor, children) -> new ItemDepositorResponse.Builder()
                .withName(depositor.getName())
                .withDepositors(children)
                .withCount(children.stream().map(ItemDepositorResponse::getCount).mapToInt(Integer::intValue).sum())
                .build();

        Function<Map<ChairEntity, List<ItemDepositorResponse>>, List<ItemDepositorResponse>> fetchDepositorsForFaculty = (it) -> it.entrySet()
                .stream()
                .map(t -> fetchDepositorDataForDepositorDivision.apply(t.getKey(), t.getValue()))
                .collect(Collectors.toList());

        return Seq.seq(data)
                .grouped(item -> item.getKey().getChairEntity().getFacultyEntity(),
                        Collectors.groupingBy(it -> it.getKey().getChairEntity(),
                                Collectors.mapping(countContributionForSpeciality, Collectors.toList())))
                .map(item -> Pair.of(item.v1, fetchDepositorsForFaculty.apply(item.v2)))
                .map(item -> fetchDepositorDataForDepositorDivision.apply(item.getKey(), item.getValue()))
                .collect(Collectors.toList());
    }

    public List<ItemDepositorResponse> getUsersSubmissionCountBetweenDates(Context context, LocalDate from, LocalDate to) throws SQLException, IOException {
        Map<UUID, LocalDate> allDatesAvailable = essuirItemService.getAllDatesAvailable(context);
        List<Pair<EssuirEperson, Long>> submissionsByEperson = Seq.seq(Lists.newArrayList(essuirItemService.findAll(context)))
                .filter(submission -> isDateInRange.test(allDatesAvailable.getOrDefault(submission.getID(), LocalDate.MIN), Pair.of(from, to)))
                .grouped(Item::getSubmitter, Collectors.counting())
                .map(submission -> Pair.of(epersonService.extendEpersonInformation(submission.v1), submission.v2))
                .toList();
        return collectStatistics(submissionsByEperson);
    }

//    public List<Item> getUploadedItemsByFacultyName(String faculty, LocalDate from, LocalDate to) {
//        List<Item> items = databaseService.fetchItemsInArchive();
//
//        return items
//                .stream()
//                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
//                .filter(item -> faculty.equals(item.getSubmitter().getChairEntity().getFacultyEntityName()))
//                .collect(Collectors.toList());
//    }
//
//    public List<Item> getUploadedItemsByChairName(String chair, LocalDate from, LocalDate to) {
//        List<Item> items = databaseService.fetchItemsInArchive();
//
//        return items
//                .stream()
//                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
//                .filter(item -> chair.equals(item.getSubmitter().getChairEntity().getChairName()))
//                .collect(Collectors.toList());
//    }
//
//    public List<Item> getUploadedItemsByPersonEmail(String person, LocalDate from, LocalDate to) {
//        List<Item> items = databaseService.fetchItemsInArchive();
//
//        return items
//                .stream()
//                .filter(item -> isDateInRange.test(item.getDateAvailable(), Pair.of(from, to)))
//                .filter(item -> person.equals(item.getSubmitter().getEmail()))
//                .collect(Collectors.toList());
//    }
//
//    public List<Item> getItemsInSpeciality(String pattern, LocalDate from, LocalDate to) {
//        return specialityReportFetcher.getItemsInSpeciality(pattern, from, to);
//    }
//
//    public List<Item> getBacheoursWithoutSpeciality() {
//        return specialityReportFetcher.getBachelorsWithoutSpeciality();
//    }
//
//    public List<Faculty> getSpecialitySubmissionCountBetweenDates(LocalDate from, LocalDate to) {
//        return collectStatistics(specialityReportFetcher.getSpecialitySubmissionCountBetweenDates(from, to));
//    }
}