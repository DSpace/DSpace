package ua.edu.sumdu.essuir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.edu.sumdu.essuir.entity.GeneralStatistics;

import java.util.List;

public interface GeneralStatisticsRepository extends JpaRepository<GeneralStatistics, Integer> {

    @Query("SELECT s FROM GeneralStatistics s WHERE s.month = -1 ORDER BY s.year DESC")
    List<GeneralStatistics> findAllYearsStatistics();

    @Query("SELECT s.viewsCount FROM GeneralStatistics s WHERE s.year = :year AND s.month <> -1 ORDER BY s.month")
    List<Integer> findAllMonthsViewsStatisticsByYear(@Param("year") Integer year);

    @Query("SELECT s.downloadsCount FROM GeneralStatistics s WHERE s.year = :year AND s.month <> -1 ORDER BY s.month")
    List<Integer> findAllMonthsDownloadsStatisticsByYear(@Param("year") Integer year);

    @Query("SELECT s FROM GeneralStatistics s WHERE s.year = :year AND s.month = :month")
    GeneralStatistics findCurrentYearTotalStatistics(@Param("year") Integer year, @Param("month") Integer month);
}
