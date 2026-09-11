package com.freight.util;

import com.freight.entity.FreightVesselSchedule;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VesselScheduleExcelParserTest {

    @Test
    void splitsMultiplePortLabelsAndStopsAtNextSectionTitle() throws Exception {
        byte[] workbook = workbook(sheet -> {
            row(sheet, 0, "MANILA (TSL)");
            row(sheet, 1, "VSL/VOY", "CFS CLSG", "STUFFING DATE", "SI CUT OFF", "ETD", "ETA", "ETA");
            row(sheet, 2, "", "", "", "", "", "SOUTH", "NORTH");
            row(sheet, 3, "ONE VESSEL V.001", "2026-08-01", "2026-08-02", "2026-08-02", "2026-08-03", "2026-08-10", "2026-08-10");
            row(sheet, 5, "AARHUS/COPENHAGEN (COSCO)");
            row(sheet, 6, "VSL/VOY", "CFS CLSG", "STUFFING DATE", "SI CUT OFF", "ETD", "ETA");
            row(sheet, 7, "", "", "", "", "", "AAR/CPH");
            row(sheet, 8, "COSCO V.002", "2026-08-04", "2026-08-05", "2026-08-05", "2026-08-06", "2026-08-20");
        });

        VesselScheduleExcelParser.ParseResult result = VesselScheduleExcelParser.parse(
            new ByteArrayInputStream(workbook), "SCHE.xlsx");

        assertThat(result.skipped()).isZero();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.records()).hasSize(3);
        assertThat(result.records()).extracting(FreightVesselSchedule::getPortCode)
            .containsExactlyInAnyOrder("MNL", "AAR", "CPH");
        assertThat(result.records()).filteredOn(r -> r.getVesselVoyage().equals("COSCO V.002"))
            .extracting(FreightVesselSchedule::getPortCode)
            .containsExactlyInAnyOrder("AAR", "CPH");
    }

    @Test
    void canonicalizesScheduleAliasesToQuoteCodes() {
        assertThat(VesselScheduleExcelParser.canonicalPortCode("HCM")).isEqualTo("SGN");
        assertThat(VesselScheduleExcelParser.canonicalPortCode("SOUTH")).isEqualTo("MNL");
        assertThat(VesselScheduleExcelParser.canonicalPortCode("AQA")).isEqualTo("AQJ");
        assertThat(VesselScheduleExcelParser.canonicalPortCode("JBL")).isEqualTo("DXB");
    }

    private byte[] workbook(java.util.function.Consumer<org.apache.poi.ss.usermodel.Sheet> writer) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Europe");
            writer.accept(sheet);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        }
    }

    private void row(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String... values) {
        var row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            if (values[i].matches("\\d{4}-\\d{2}-\\d{2}")) {
                row.createCell(i).setCellValue(java.sql.Date.valueOf(LocalDate.parse(values[i])));
            } else {
                row.createCell(i).setCellValue(values[i]);
            }
        }
    }
}
