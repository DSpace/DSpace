function show(){
    $.ajax({
        url: "/stat/current",
        cache: false,
        success: function(data){
            document.getElementById('TotalCount').innerText = data.TotalCount;
            document.getElementById('TotalViews').innerText = data.TotalViews;
            document.getElementById('TotalDownloads').innerText = data.TotalDownloads;
            document.getElementById('CurrentMonthStatisticsViews').innerText = data.CurrentMonthStatisticsViews;
            document.getElementById('CurrentMonthStatisticsDownloads').innerText = data.CurrentMonthStatisticsDownloads;
            document.getElementById('CurrentYearStatisticsViews').innerText = data.CurrentYearStatisticsViews;
            document.getElementById('CurrentYearStatisticsDownloads').innerText = data.CurrentYearStatisticsDownloads;
        }
    });
}

$(document).ready(function(){
    show();
    setInterval('show()',4000);
});