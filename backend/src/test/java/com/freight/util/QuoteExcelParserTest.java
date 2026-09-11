package com.freight.util;

import com.freight.entity.FreightQuote;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteExcelParserTest {

    @Test
    void resolvesPortLouisToTheSchedulePortCode() {
        assertThat(QuoteExcelParser.resolvePortCode("PORT LOUIS")).isEqualTo("PTL");
    }

    @Test
    void parsesCurrentMainSheetColumnsWithoutPuttingRemarksIntoTransitTime() throws Exception {
        byte[] workbook = createCurrentMainSheet();

        List<FreightQuote> quotes = QuoteExcelParser.parse(
                new ByteArrayInputStream(workbook),
                "报价.xlsx",
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 9, 3));

        assertThat(quotes).hasSize(1);
        FreightQuote quote = quotes.get(0);
        assertThat(quote.getOfWuchong()).isEqualTo("20");
        assertThat(quote.getOfJiaoxin()).isEqualTo("-90");
        assertThat(quote.getTransitTime()).isEqualTo("5");
        assertThat(quote.getCc()).isEqualTo("Y");
        assertThat(quote.getCarrier()).isEqualTo("SINOKOR/WHL");
        assertThat(quote.getRemarks()).startsWith("1. 薰蒸");
    }

    @Test
    void skipsNotesAtBottomOfMainSheetInsteadOfTreatingThemAsVolumeRanges() throws Exception {
        byte[] workbook = createMainSheetWithNotes();

        List<FreightQuote> quotes = QuoteExcelParser.parse(
                new ByteArrayInputStream(workbook),
                "报价.xlsx",
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 9, 3));

        assertThat(quotes).hasSize(1);
        assertThat(quotes.get(0).getVolumeRange()).isEqualTo("1-3CBM以内");
    }

    private byte[] createCurrentMainSheet() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("黄埔,北沙仓,滘心");

            row(sheet, 5, "LANE/COUNTRY", "DESTINATION", "体积", "VIA", "MIN", "乌冲", "", "", "滘心", "", "", "T/T", "CC", "CARRIER", "注意事项");
            row(sheet, 6, "", "", "", "", "", "OF", "头程", "大船", "OF", "头程", "大船", "", "", "", "");
            row(sheet, 8, "THAILAND", "BANGKOK", "1CBM以内", "DIR", "1", "20", "2J4K,5J2K", "4J6K", "-90", "2J4K", "2J3K", "5", "Y", "SINOKOR/WHL", "1. 薰蒸\n2. 货物需有唛头");

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        }
    }

    private byte[] createMainSheetWithNotes() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("黄埔,北沙仓,滘心");

            row(sheet, 5, "LANE/COUNTRY", "DESTINATION", "体积", "VIA", "MIN", "乌冲", "", "", "滘心", "", "", "T/T", "CC", "CARRIER", "注意事项");
            row(sheet, 6, "", "", "", "", "", "OF", "头程", "大船", "OF", "头程", "大船", "", "", "", "");
            row(sheet, 8, "THAILAND", "BANGKOK", "1-3CBM以内", "DIR", "1", "20", "2J4K", "4J6K", "-90", "2J4K", "2J3K", "5", "Y", "SINOKOR", "");
            row(sheet, 9, "", "", "1.以上一条龙报价只针对5CBM以下的货物...");
            row(sheet, 10, "", "", "2.以上报价不含文件费...");
            row(sheet, 11, "", "", "5.单件货物超长（3米或以上）...");
            row(sheet, 12, "", "", "如故意隐瞒伪报，所产生的一切后果...");

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        }
    }

    private void row(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String... values) {
        var row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
