package org.ssu.service;


import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.ChairEntity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.essuir.DepositorDivision;
import org.dspace.eperson.essuir.DepositorSimpleUnit;
import org.jooq.lambda.Seq;
import org.springframework.stereotype.Service;
import org.ssu.entity.response.ItemDepositorResponse;
import org.ssu.entity.response.ItemResponse;

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
    @Resource
    private SpecialityReportFetcher specialityReportFetcher;

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
                .filter(person -> person.getKey().getChair() != null)
                .grouped(item -> item.getKey().getChair().getFacultyEntity(),
                        Collectors.groupingBy(it -> it.getKey().getChair(),
                                Collectors.mapping(countContributionForSpeciality, Collectors.toList())))
                .map(item -> Pair.of(item.v1, fetchDepositorsForFaculty.apply(item.v2)))
                .map(item -> fetchDepositorDataForDepositorDivision.apply(item.getKey(), item.getValue()))
                .collect(Collectors.toList());
    }

    private Seq<Item> getItemsBetweenDates(Context context, LocalDate from, LocalDate to) throws SQLException, IOException {
        Map<UUID, LocalDate> allDatesAvailable = essuirItemService.getAllDatesAvailable(context);
        return Seq.seq(Lists.newArrayList(essuirItemService.findAll(context)))
                .filter(submission -> isDateInRange.test(allDatesAvailable.getOrDefault(submission.getID(), LocalDate.MIN), Pair.of(from, to)));
    }

    public List<ItemDepositorResponse> getUsersSubmissionCountBetweenDates(Context context, LocalDate from, LocalDate to) throws SQLException, IOException {
        List<Pair<EPerson, Long>> submissionsByEperson = getItemsBetweenDates(context, from, to)
                .grouped(Item::getSubmitter, Collectors.counting())
                .map(submission -> Pair.of(submission.v1, submission.v2))
                .toList();
        return collectStatistics(submissionsByEperson);
    }

    public List<ItemResponse> getUploadedItemsByFacultyName(Context context, String faculty, LocalDate from, LocalDate to) throws SQLException, IOException {
        return getItemsBetweenDates(context, from, to)
                .filter(item -> faculty.equals(item.getSubmitter().getChair().getFacultyEntityName()))
                .map(item -> new ItemResponse.Builder().withTitle(item.getName()).withHandle(item.getHandle()).build())
                .collect(Collectors.toList());
    }

    public List<ItemResponse> getUploadedItemsByChairName(Context context, String chair, LocalDate from, LocalDate to) throws SQLException, IOException {
        return getItemsBetweenDates(context, from, to)
                .filter(item -> chair.equals(item.getSubmitter().getChair().getName()))
                .map(item -> new ItemResponse.Builder().withTitle(item.getName()).withHandle(item.getHandle()).build())
                .collect(Collectors.toList());
    }

    public List<ItemResponse> getUploadedItemsByPersonEmail(Context context, String person, LocalDate from, LocalDate to) throws SQLException, IOException {
        return getItemsBetweenDates(context, from, to)
                .filter(item -> person.equals(item.getSubmitter().getEmail()))
                .map(item -> new ItemResponse.Builder().withTitle(item.getName()).withHandle(item.getHandle()).build())
                .collect(Collectors.toList());
    }

    public List<Item> getItemsInSpeciality(Context context, String pattern, LocalDate from, LocalDate to) throws IOException, SQLException {
        return specialityReportFetcher.getItemsInSpeciality(context, pattern, from, to);
    }

    public List<Item> getBacheoursWithoutSpeciality(Context context) {
        return specialityReportFetcher.getBachelorsWithoutSpeciality(context);
    }

    public List<ItemDepositorResponse> getSpecialitySubmissionCountBetweenDates(Context context, LocalDate from, LocalDate to) throws IOException, SQLException {
        return collectStatistics(specialityReportFetcher.getSpecialitySubmissionCountBetweenDates(context, from, to));
    }
}