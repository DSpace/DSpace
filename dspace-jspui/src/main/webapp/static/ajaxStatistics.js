function show(){
    $.ajax({
        url: "/current",
        cache: false,
        success: function(data){
            var response = JSON.parse(data);
            document.getElementById('TotalCount').innerText = response.total_count;
            document.getElementById('TotalViews').innerText = response.total_views;
            document.getElementById('TotalDownloads').innerText = response.total_downloads;
            document.getElementById('CurrentMonthStatisticsViews').innerText = response.current_month_statistics_views;
            document.getElementById('CurrentMonthStatisticsDownloads').innerText = response.current_month_statistics_downloads;
            document.getElementById('CurrentYearStatisticsViews').innerText = response.current_year_statistics_views;
            document.getElementById('CurrentYearStatisticsDownloads').innerText = response.current_year_statistics_downloads;
        }
    });
}

$(document).ready(function(){
    show();
    setInterval('show()',4000);
});