package com.freight.util;

import com.freight.entity.FreightVesselSchedule;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 解析 SCHE 类船期表。表格不是平表，因此按“港口标题 + 表头 + 港口代码行 + 数据行”识别区块，
 * 不依赖工作表固定行号；一个 ETD 下的多个 ETA 港口会拆成多条记录。
 */
@Slf4j
public final class VesselScheduleExcelParser {

    private static final Pattern PORT_CODE = Pattern.compile("[A-Z]{2,6}");
    private static final Map<String, String> PORT_ALIASES = Map.ofEntries(
        Map.entry("VPR", "VAP"), Map.entry("CLA", "CLL"), Map.entry("BVT", "BUN"),
        Map.entry("GYQ", "GYE"), Map.entry("HCM", "SGN"), Map.entry("HPG", "HPH"),
        Map.entry("CHT", "CGP"), Map.entry("PKL", "PKG"), Map.entry("TKY", "TYO"),
        Map.entry("KBE", "UKB"), Map.entry("VCV", "YVR"), Map.entry("TRT", "YYZ"),
        Map.entry("MTL", "YUL"), Map.entry("KEE", "KEL"), Map.entry("TCG", "TXG"),
        Map.entry("KAO", "KHH"), Map.entry("MEL", "MEL"), Map.entry("SYD", "SYD"),
        Map.entry("LAX", "LAX"), Map.entry("NYC", "NYC"), Map.entry("RTM", "RTM"),
        Map.entry("HAM", "HAM"), Map.entry("FLX", "FLX"), Map.entry("LHV", "LHV"),
        Map.entry("SIN", "SIN"), Map.entry("PKG", "PKG"), Map.entry("JKT", "JKT"),
        Map.entry("SBY", "SBY"), Map.entry("SMG", "SMG"), Map.entry("OSA", "OSA"),
        Map.entry("NGO", "NGO"), Map.entry("CNN", "CNN"), Map.entry("NVS", "NVS"),
        Map.entry("MNL", "MNL"), Map.entry("BKK", "BKK"), Map.entry("HPH", "HPH"),
        Map.entry("SOUTH", "MNL"), Map.entry("NORTH", "MNL"),
        Map.entry("OSLO", "OSL"), Map.entry("HELSINKI", "HEL"),
        Map.entry("ALIAGA", "ALI"), Map.entry("ALSANCAK", "ALS"),
        Map.entry("AQA", "AQJ"), Map.entry("JBL", "DXB"),
        Map.entry("DBN", "DUR"), Map.entry("JHB", "JNB"), Map.entry("HAY", "IST"),
        Map.entry("MTV", "MVD"), Map.entry("PRA", "PRG"), Map.entry("LCB", "LCH"),
        Map.entry("LKG", "LKB"), Map.entry("GTB", "GTB")
    );

    private VesselScheduleExcelParser() {
    }

    public record ParseResult(List<FreightVesselSchedule> records, int skipped, List<String> warnings) {
    }

    public static ParseResult parse(InputStream input, String fileName) throws Exception {
        List<FreightVesselSchedule> records = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int skipped = 0;

        try (Workbook workbook = WorkbookFactory.create(input)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            for (Sheet sheet : workbook) {
                for (int rowNo = 0; rowNo <= sheet.getLastRowNum(); rowNo++) {
                    Row header = sheet.getRow(rowNo);
                    int vesselCol = findColumn(header, "VSL/VOY", "VSL", "VESSEL");
                    if (vesselCol < 0) continue;
                    int etdCol = findColumn(header, "ETD");
                    // 区块标题可能包含 VSL 字样，只有同时具备 ETD 列才是有效表头。
                    if (etdCol < 0) continue;

                    String section = previousTitle(sheet, rowNo);
                    String serviceName = serviceName(section);
                    String portName = portName(section);
                    Map<String, Integer> portColumns = new LinkedHashMap<>();
                    int firstDataRow = rowNo + 1;
                    for (; firstDataRow <= sheet.getLastRowNum(); firstDataRow++) {
                        Row candidate = sheet.getRow(firstDataRow);
                        String vessel = text(candidate, vesselCol, formatter, evaluator);
                        if (!vessel.isBlank()) break;
                        if (candidate != null) for (int col = etdCol + 1; col < candidate.getLastCellNum(); col++) {
                            for (String rawCode : portCodes(text(candidate, col, formatter, evaluator))) {
                                portColumns.putIfAbsent(rawCode, col);
                            }
                        }
                    }
                    if (portColumns.isEmpty()) {
                        for (String fallback : fallbackPortCodes(section)) {
                            portColumns.putIfAbsent(fallback, etdCol + 1);
                        }
                    }
                    if (portColumns.isEmpty()) {
                        warnings.add(sheet.getSheetName() + " 第" + (rowNo + 1) + "行无法识别港口代码: " + section);
                    }

                    for (int dataRow = firstDataRow; dataRow <= sheet.getLastRowNum(); dataRow++) {
                        Row row = sheet.getRow(dataRow);
                        String vessel = text(row, vesselCol, formatter, evaluator);
                        if (vessel.isBlank()) continue;
                        if (isVesselHeader(row)) break;
                        LocalDate etd = date(row, etdCol, evaluator);
                        if (etd == null) {
                            if (startsNextSection(sheet, dataRow, vesselCol)) break;
                            if (isInformationalRow(vessel)) continue;
                            skipped++;
                            warnings.add(sheet.getSheetName() + " 第" + (dataRow + 1) + "行缺少有效 ETD: " + vessel);
                            continue;
                        }
                        String status = status(vessel);
                        Set<String> rowPorts = new HashSet<>();
                        for (Map.Entry<String, Integer> port : portColumns.entrySet()) {
                            String canonicalPort = canonicalPortCode(port.getKey());
                            if (!rowPorts.add(canonicalPort)) continue;
                            FreightVesselSchedule schedule = new FreightVesselSchedule();
                            schedule.setSourceFile(fileName);
                            schedule.setSourceSheet(sheet.getSheetName().trim());
                            schedule.setSectionName(section);
                            schedule.setServiceName(serviceName);
                            schedule.setRawPortCode(port.getKey());
                            schedule.setPortCode(canonicalPort);
                            schedule.setPortName(portName);
                            schedule.setVesselVoyage(vessel);
                            schedule.setCfsClosingDate(date(row, findColumn(header, "CFS"), evaluator));
                            schedule.setStuffingDate(date(row, findColumn(header, "STUFF"), evaluator));
                            schedule.setSiCutoffDate(date(row, findColumn(header, "SI"), evaluator));
                            schedule.setEtd(etd);
                            schedule.setEta(date(row, port.getValue(), evaluator));
                            schedule.setStatus(status);
                            schedule.setSourceRowNo(dataRow + 1);
                            records.add(schedule);
                        }
                    }
                }
            }
        }
        return new ParseResult(records, skipped, Collections.unmodifiableList(warnings));
    }

    public static String canonicalPortCode(String code) {
        if (code == null || code.isBlank()) return null;
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return PORT_ALIASES.getOrDefault(normalized, normalized);
    }

    private static boolean startsNextSection(Sheet sheet, int rowNo, int vesselCol) {
        Row row = sheet.getRow(rowNo);
        if (row == null || text(row, vesselCol, new DataFormatter(), null).isBlank()) return false;
        // 缺 ETD 的船期行仍记录为 skipped；只有后面紧邻下一张表头时才是区块标题。
        for (int next = rowNo + 1; next <= Math.min(sheet.getLastRowNum(), rowNo + 3); next++) {
            Row candidate = sheet.getRow(next);
            if (isVesselHeader(candidate)
                && findColumn(candidate, "ETD") >= 0) return true;
        }
        return false;
    }

    private static boolean isVesselHeader(Row row) {
        if (row == null) return false;
        for (int col = 0; col < row.getLastCellNum(); col++) {
            String value = rawText(row.getCell(col)).toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
            if (value.equals("VSL/VOY") || value.equals("VSL") || value.equals("VESSEL")) return true;
        }
        return false;
    }

    private static boolean isInformationalRow(String vessel) {
        String upper = vessel.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("NOTE:") || upper.startsWith("NOTES:");
    }

    private static boolean isPortCode(String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return PORT_CODE.matcher(normalized).matches() && !normalized.equals("ETA") && !normalized.equals("ETD");
    }

    private static List<String> portCodes(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> codes = new ArrayList<>();
        for (String token : raw.toUpperCase(Locale.ROOT).split("[/,&]+")) {
            String code = token.trim();
            if (isPortCode(code)) codes.add(code);
        }
        return codes;
    }

    private static List<String> fallbackPortCodes(String section) {
        String title = portName(section);
        List<String> codes = new ArrayList<>();
        for (String token : title.split("[/,&]+")) {
            String code = QuoteExcelParser.resolvePortCode(token.trim());
            if (code != null && !codes.contains(code)) codes.add(code);
        }
        if (codes.isEmpty()) {
            String code = QuoteExcelParser.resolvePortCode(section);
            if (code != null) codes.add(code);
        }
        return codes;
    }

    private static String status(String vessel) {
        String upper = vessel.toUpperCase(Locale.ROOT);
        if (upper.contains("SUSPEND")) return "SUSPEND";
        if (upper.contains("TBA") || upper.contains("TO BE NOMINATED")) return "TBA";
        if (upper.contains("BLANK SAILING")) return "BLANK";
        return "NORMAL";
    }

    private static String previousTitle(Sheet sheet, int headerRow) {
        for (int r = headerRow - 1; r >= 0; r--) {
            String title = text(sheet.getRow(r), 0, new DataFormatter(), null);
            if (!title.isBlank()) return title.replaceAll("\\s+", " ").trim();
        }
        return sheet.getSheetName().trim();
    }

    private static String portName(String section) {
        int bracket = section.indexOf('(');
        String name = bracket > 0 ? section.substring(0, bracket) : section;
        return name.replaceAll("\\s+", " ").trim();
    }

    private static String serviceName(String section) {
        int start = section.indexOf('(');
        int end = section.lastIndexOf(')');
        return start >= 0 && end > start ? section.substring(start + 1, end).trim() : null;
    }

    private static int findColumn(Row row, String... labels) {
        if (row == null) return -1;
        for (int c = 0; c < row.getLastCellNum(); c++) {
            String value = rawText(row.getCell(c));
            for (String label : labels) {
                if (value.toUpperCase(Locale.ROOT).contains(label.toUpperCase(Locale.ROOT))) return c;
            }
        }
        return -1;
    }

    private static String text(Row row, int col, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null || col < 0 || row.getCell(col) == null) return "";
        Cell cell = row.getCell(col);
        String value = evaluator == null ? formatter.formatCellValue(cell) : formatter.formatCellValue(cell, evaluator);
        return value == null ? "" : value.trim();
    }

    private static String rawText(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        return new DataFormatter().formatCellValue(cell).trim();
    }

    private static LocalDate date(Row row, int col, FormulaEvaluator evaluator) {
        if (row == null || col < 0 || row.getCell(col) == null) return null;
        Cell cell = row.getCell(col);
        try {
            if (cell.getCellType() == CellType.FORMULA) {
                var value = evaluator.evaluate(cell);
                if (value != null && value.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(value.getNumberValue())) {
                    return DateUtil.getJavaDate(value.getNumberValue()).toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception e) {
            log.debug("船期日期解析失败: row={}, col={}", row.getRowNum() + 1, col + 1, e);
        }
        return null;
    }
}
