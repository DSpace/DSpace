/*
   paymentCalculator.js
   Dryad Digital Repository
*/

/* Constants */
// membership dues
var MEMBERSHIP_DUES_HIGH = 5000;
var MEMBERSHIP_DUES_LOW = 1000;

// subscription
var SUB_NON_MEMBER_PRICE_PER_ARTICLE = 30;
var SUB_MEMBER_PRICE_PER_ARTICLE = 25;

// vouchers
var VCH_NON_MEMBER_PRICE_PER_ARTICLE = 70;
var VCH_MEMBER_PRICE_PER_ARTICLE = 65;

// deferred
var DEF_NON_MEMBER_PRICE_PER_ARTICLE = 75;
var DEF_MEMBER_PRICE_PER_ARTICLE = 70;

// calculate()
// articlesPerYear: integer
// depositsPerYear: integer
// grossIncomePerYearUnderTenMillion: bool
// returns object with 6 properties

function calculate(articlesPerYear, depositsPerYear, grossIncomePerYearUnderTenMillion) {
  var calculatedResults = {};
  var membershipCost = grossIncomePerYearUnderTenMillion ? MEMBERSHIP_DUES_LOW : MEMBERSHIP_DUES_HIGH;
  
  // subscriptions
  calculatedResults.subscriptionCostNonMember = SUB_NON_MEMBER_PRICE_PER_ARTICLE * articlesPerYear;
  calculatedResults.subscriptionCostMember = (SUB_MEMBER_PRICE_PER_ARTICLE * articlesPerYear) + membershipCost;
  
  // vouchers
  calculatedResults.voucherCostNonMember = VCH_NON_MEMBER_PRICE_PER_ARTICLE * depositsPerYear;
  calculatedResults.voucherCostMember = (VCH_MEMBER_PRICE_PER_ARTICLE * depositsPerYear) + membershipCost;
  
  // deferred
  calculatedResults.deferredCostNonMember = DEF_NON_MEMBER_PRICE_PER_ARTICLE * depositsPerYear;
  calculatedResults.deferredCostMember = (DEF_MEMBER_PRICE_PER_ARTICLE * depositsPerYear) + membershipCost;
  
  return calculatedResults;
}

// calculatePercentage
// articlesPerYear: integer
// percentageWithDeposits: integer (0-100)
// grossIncomePerYearUnderTenMillion: boolean
// returns object with 6 properties
function calculatePercentage(articlesPerYear, percentageWithDeposits, grossIncomePerYearUnderTenMillion) {
  var depositsPerYear = (percentageWithDeposits / 100.0) * articlesPerYear;
  return calculate(articlesPerYear, depositsPerYear, grossIncomePerYearUnderTenMillion);
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
  return '$' + amt.formatMoney(2, '.', ',');
}

function updateResults() {
  var articlesPerYear = $('#articles_per_year').val();
  var percentageWithDeposits = $('#percentage_with_deposits').val();
  var error;
  
  // make sure articles per year is a number
  if(articlesPerYear.length == 0 || isNaN(Number(articlesPerYear))) {
    // return errors!
    error = "Please enter a number.";
  }
  
  // make sure percentage with deposits is a number
  if(percentageWithDeposits.length == 0 || isNaN(Number(percentageWithDeposits))) {
    error = "Please enter a number.";
  }
  
  // check if articles per year is less than 0
  if(Number(articlesPerYear) < 0) {
    error = "Please enter a number greater than 0.";
  }
  
  // check if percentage is within range
  if(Number(percentageWithDeposits) < 0 || Number(percentageWithDeposits) > 100) {
    error = "Please enter a percentage between 0 and 100.";
  }
  
  var grossIncomePerYearUnderTenMillion = $("input[name='gross_income_under_10_million']").filter(':checked').val() == "yes";
  var result = calculatePercentage(articlesPerYear, percentageWithDeposits, grossIncomePerYearUnderTenMillion);
  
  if(error) {
    $('#errors').text(error);
  } else {
    $('#errors').text('');
    $('#subscription_cost_non_member').text(toDollars(result.subscriptionCostNonMember));
    $('#subscription_cost_member').text(toDollars(result.subscriptionCostMember));
    $('#deferred_cost_non_member').text(toDollars(result.deferredCostNonMember));
    $('#deferred_cost_member').text(toDollars(result.deferredCostMember));
    $('#voucher_cost_non_member').text(toDollars(result.voucherCostNonMember));
    $('#voucher_cost_member').text(toDollars(result.voucherCostMember));
  }
}

$(document).ready(function() {
  $('#calculate_button').click(function() {
    updateResults();
    return false;
  });
});
