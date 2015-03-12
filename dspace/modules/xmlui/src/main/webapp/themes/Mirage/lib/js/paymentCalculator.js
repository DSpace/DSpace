/*
   paymentCalculator.js
   Dryad Digital Repository
*/
$(document).ready(function() {
    // on-success ajax callback; 
    // 'plans' is the data from file payment-calculator.json 
    var setPaymentHandler = function(plans) {
        if (plans === null) noPaymentResults(null, "Null calculator data", null);
        $('#calculate_button').click(function() {
            // calculate()
            // plan: plan-sustainer, plan-supporter, plan-advocate, plan-nonmember
            // plan_rates: fee object above
            // articlesPerYear: integer
            // depositsPerYear: integer
            // grossIncomePerYearUnderTenMillion: bool
            // returns object with 3 properties, corresponding to the @class attribute of the
            //     corresponding cell in the page's table
            function calculate(plan, articlesPerYear, depositsPerYear, grossIncomePerYearUnderTenMillion) {
              // set supporter/sustainer row to n/a's based on $10M radio value
              if (  (plan === "plan-sustainer" &&  grossIncomePerYearUnderTenMillion)
                 || (plan === "plan-supporter" && !grossIncomePerYearUnderTenMillion))
              {
                  return {
                    "cost-voucher"      : "n/a",
                    "cost-deferred"     : "n/a",
                    "cost-subscription" : "n/a"
                 };
              }
              return {
                  "cost-voucher"      : (plans[plan].voucher       * depositsPerYear) + plans[plan].dues,
                  "cost-deferred"     : (plans[plan].deferred      * depositsPerYear) + plans[plan].dues,
                  "cost-subscription" : (plans[plan].subscription  * articlesPerYear) + plans[plan].dues
              };
            }
            // calculatePercentage
            // articlesPerYear: integer
            // percentageWithDeposits: integer (0-100)
            // grossIncomePerYearUnderTenMillion: boolean
            // returns object with 3 properties
            function calculatePercentage(plan, articlesPerYear, percentageWithDeposits, grossIncomePerYearUnderTenMillion) {
              var depositsPerYear = (percentageWithDeposits / 100.0) * articlesPerYear;
              return calculate(plan, articlesPerYear, depositsPerYear, grossIncomePerYearUnderTenMillion);
            }
            Number.prototype.formatMoney = function(c, d, t){
            var n = this,
                c = isNaN(c = Math.abs(c)) ? 2 : c,
                d = d == undefined ? "." : d,
                t = t == undefined ? "," : t,
                s = n < 0 ? "-" : "",
                i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "",
                j = (j = i.length) > 3 ? j % 3 : 0;
               return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
            };
            function toDollars(amt) {
              if (isNaN(amt)) return amt;
              return '$' + amt.formatMoney(0, '.', ',');
            }
            function updateResults() {
              var articlesPerYear = $('#articles_per_year').val();
              var percentageWithDeposits = $('#percentage_with_deposits').val();
              var articlesPerYearError;
              var percentageWithDepositsError;
            
              // make sure articles per year is a number
              if(articlesPerYear.length == 0 || isNaN(Number(articlesPerYear))) {
                articlesPerYearError = "Please enter a number.";
              }
              // make sure percentage with deposits is a number
              if(percentageWithDeposits.length == 0 || isNaN(Number(percentageWithDeposits))) {
                percentageWithDepositsError = "Please enter a number.";
              }
              // check if articles per year is less than 0
              if(Number(articlesPerYear) < 0) {
                articlesPerYearError = "Please enter a number greater than 0.";
              }
              // check if percentage is within range
              if(Number(percentageWithDeposits) < 0 || Number(percentageWithDeposits) > 100) {
                percentageWithDepositsError = "Please enter a number between 0 and 100.";
              }
              var grossIncomePerYearUnderTenMillion = $("input[name='gross_income_under_10_million']").filter(':checked').val() == "yes";
              
              if(articlesPerYearError || percentageWithDepositsError) {
                if(articlesPerYearError) {
                  $('#articles_per_year_error').text('Error: ' + articlesPerYearError);
                  $('#articles_per_year_error').show();
                } else {
                  $('#articles_per_year_error').hide();
                }
                if(percentageWithDepositsError) {
                  $('#percentage_with_deposits_error').text('Error: ' + percentageWithDepositsError);
                  $('#percentage_with_deposits_error').show();
                } else {
                  $('#percentage_with_deposits_error').hide();
                }
              } else {
                // no errors
                $('.pricing_errors').text('');
                $('.pricing_errors').hide();
                for (var plan in plans) {
                    if (!plans.hasOwnProperty(plan)) continue;
                    var cells = calculatePercentage(plan, articlesPerYear, percentageWithDeposits, grossIncomePerYearUnderTenMillion);
                    for (var cell in cells) {
                        if (!cells.hasOwnProperty(cell)) continue;
                        $('#' + plan).find('.' + cell).text(toDollars(cells[cell]));
                    }
                }
              }
            }
            // do update
            updateResults();
        }); // $('#calculate_button').click(function() {
    }; // setPaymentHandler
    var noPaymentResults = function(/*jQuery.jqXHR*/ jqXHR, /*String*/ textStatus, /*String*/ errorThrown) {
        // console.log(textStatus);
    };
    jQuery.ajax({
        dataType: 'json',
        url:      document.location.protocol + '//' + document.location.host + '/static/json/payment-calculator.json',
        success:  setPaymentHandler,
        error:    noPaymentResults
    });
    // jQuery document-ready return value
    return false;
});
