const fs = require('fs');
const revFile = 'app/src/main/java/com/example/myapplication/features/finance/RevenueActivity.java';
let rev = fs.readFileSync(revFile, 'utf8');
rev = rev.split('updateReportStats(fmt);').join('updateReportStats(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")));');
fs.writeFileSync(revFile, rev);
