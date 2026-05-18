package com.freight.util;

import com.freight.entity.DestPortCharge;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DestPortChargeParser {

    // 跳过的无效行关键词
    private static final Set<String> SKIP_KEYWORDS = Set.of(
        "返回", "中文项目", "item", "中文", "英文项目", "首页", ""
    );
    // 货币正则
    private static final Pattern CURRENCY_PATTERN =
        Pattern.compile("(USD|EUR|HKD|SGD|THB|AED|KRW|NTD|GBP|AUD|CAD|ZAR|BRL|MOP|TWD)\\s*([\\d,]+\\.?\\d*)",
            Pattern.CASE_INSENSITIVE);
    // 数字正则
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?([\\d,]+\\.?\\d*)$");

    public static List<DestPortCharge> parse(InputStream in, String fileName) throws Exception {
        List<DestPortCharge> result = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(in)) {
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
                String sheetName = sheet.getSheetName();
                if (sheetName.equals("首页") || sheetName.equals("Sheet1")) continue;
                result.addAll(parseSheet(sheet, sheetName, fileName));
            }
        }
        return result;
    }

    private static List<DestPortCharge> parseSheet(Sheet sheet, String sheetName, String fileName) {
        List<DestPortCharge> list = new ArrayList<>();
        String currentDest = null;
        String currentCurrency = null;  // 同港口默认货币
        boolean inFeeSection = false;

        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String col0 = getCellStr(row, 0);
            String col1 = getCellStr(row, 1);
            String col2 = getCellStr(row, 2);

            // 空行跳过
            if (col0.isEmpty() && col1.isEmpty()) continue;

            // 判断是否是港口标题行：col0有内容，col1为空，且不是表头关键词
            if (!col0.isEmpty() && col1.isEmpty()
                    && !SKIP_KEYWORDS.contains(col0.toLowerCase().trim())
                    && !col0.equals("中文")) {
                // 是港口标题行
                currentDest = col0.trim();
                currentCurrency = null;  // 新港口重置
                inFeeSection = false;
                // 跳过 via HK / VIA HKG 中转信息（非真实费用表）
                if (currentDest.toLowerCase().contains("via hk") || currentDest.toLowerCase().contains("via hkg")) {
                    currentDest = null;
                    inFeeSection = false;
                }
                continue;
            }

            // 判断是否是表头行（中文项目/英文项目 这行，跳过）
            if (col0.equals("中文项目") || col0.equals("中文") || col0.equals("Item")
                    || col1.equals("英文项目") || col1.equals("Item") || col1.equals("英文")) {
                inFeeSection = true;
                continue;
            }
            // 跳过Amount/Min./Max. 行
            if (col2.equals("Amount") || col2.equals("amount")) continue;

            if (!inFeeSection || currentDest == null) continue;

            // 解析费用行：col1有英文费项名，或者col0有中文，col1有英文
            // 有些行col0为NaN（继续上一个港口），col1有费项
            if (col1.isEmpty()) continue;

            // 跳过 "NaN"/"-" 等无意义内容
            if (col1.equals("-") || col1.equalsIgnoreCase("NaN")) continue;

            // 因各sheet列位置不完全统一，做自适应读取
            DestPortCharge charge = parseChargeRow(row, sheetName, currentDest, fileName, currentCurrency);
            if (charge != null) {
                // 记住本行货币，供后续无货币代码行使用
                if (charge.getCurrency() != null) currentCurrency = charge.getCurrency();
                else charge.setCurrency(currentCurrency);
                list.add(charge);
            }
        }
        return list;
    }

    /**
     * 根据行内容自适应解析费用条目
     * 主要两种格式:
     * A) col0=中文 col1=英文 col2=直客金额 col3=单位 col4=同行金额 col5=单位 col6=备注
     * B) col0=中文 col1=英文 col2=单位 col3=直客金额 col4=Min col5=Max col6=同行金额 col7=Min col8=Max col9=备注
     */
    private static DestPortCharge parseChargeRow(Row row, String sheetName,
                                                   String dest, String fileName, String defaultCurrency) {
        String feeNameCn = getCellStr(row, 0);
        String feeNameEn = getCellStr(row, 1);
        if (feeNameEn.isEmpty()) return null;
        // 阶梯费率行无论中文名有没有都跳过
        if (isInfoRow(feeNameEn)) return null;

        // 读所有列
        String[] cols = new String[12];
        for (int i = 0; i < 12; i++) cols[i] = getCellStr(row, i);

        DestPortCharge c = new DestPortCharge();
        c.setSourceSheet(sheetName);
        c.setDestination(dest);
        c.setFeeNameCn(feeNameCn.isEmpty() ? null : feeNameCn);
        c.setFeeNameEn(feeNameEn);
        c.setSourceFile(fileName);
        c.setUploadDate(LocalDate.now());

        // 尝试格式A: col2=金额 col3=单位 col4=同行金额 col5=同行单位 col6=备注
        String rawDirect = cols[2];
        String unitDirect = cols[3];
        String rawCoload = cols[4];
        String unitCoload = cols[5];
        String remarks = cols[6];

        // 如果col3看起来是金额而不是单位，说明是格式B（有min/max列）
        // col3="Amount" or is numeric → 格式B
        if (isNumericOrAmount(unitDirect) && !isUnit(unitDirect)) {
            // 格式B: col1=英文 col2=单位 col3=直客金额 col4=Min col5=Max col6=同行金额...
            unitDirect = cols[2];
            rawDirect = cols[3];
            String minStr = cols[4];
            rawCoload = cols[6];
            unitCoload = unitDirect; // 同单位
            remarks = cols[9].isEmpty() ? cols[10] : cols[9];
            c.setMinDirect(parseAmount(minStr));
        }

        // 解析货币和金额
        AmountInfo directInfo = parseAmountInfo(rawDirect, defaultCurrency);
        AmountInfo coloadInfo = parseAmountInfo(rawCoload, defaultCurrency);

        c.setAmountDirectRaw(rawDirect.isEmpty() ? null : rawDirect);
        c.setAmountDirect(directInfo != null ? directInfo.amount : null);
        c.setCurrency(directInfo != null ? directInfo.currency : (coloadInfo != null ? coloadInfo.currency : null));
        c.setUnitDirect(normalizeUnit(unitDirect));

        c.setAmountColoadRaw(rawCoload.isEmpty() ? null : rawCoload);
        c.setAmountCoload(coloadInfo != null ? coloadInfo.amount : null);
        c.setUnitCoload(normalizeUnit(unitCoload.isEmpty() ? unitDirect : unitCoload));

        c.setRemarks(remarks.isEmpty() ? null : remarks);

        // 过滤掉金额为空或"NIL"/"AS ACTUAL"等无法计算的行，但仍保存
        return c;
    }

    /** 解析金额信息：提取货币和数值 */
    private static AmountInfo parseAmountInfo(String raw, String defaultCurrency) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toUpperCase();
        if (s.equals("NIL") || s.equals("-") || s.equals("NAN") || s.startsWith("AS ACTUAL")
                || s.startsWith("AT COST") || s.equals("N/A")) return null;

        // 含货币代码
        Matcher m = CURRENCY_PATTERN.matcher(s);
        if (m.find()) {
            String currency = m.group(1).toUpperCase();
            String numStr = m.group(2).replace(",", "");
            try {
                return new AmountInfo(currency, new BigDecimal(numStr));
            } catch (Exception e) {
                return null;
            }
        }
        // 纯数字（无货币前缀，用当前港口已知货币或留空）
        String cleaned = s.replaceAll("[^\\d.]", "").trim();
        if (!cleaned.isEmpty()) {
            try {
                String cur = defaultCurrency != null ? defaultCurrency : "USD";
                return new AmountInfo(cur, new BigDecimal(cleaned));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static BigDecimal parseAmount(String s) {
        if (s == null || s.isBlank() || s.equals("-")) return null;
        try {
            return new BigDecimal(s.trim().replace(",", ""));
        } catch (Exception e) { return null; }
    }

    /** 过滤非费用行：阶梯费率说明、税费备注等 */
    private static boolean isInfoRow(String s) {
        if (s == null || s.isBlank()) return true;
        String u = s.trim().toUpperCase();
        // 阶梯费率：MIN ~ X CBM / 3~5 CBM / OVER X CBM / under X CBM
        if (u.matches(".*(MIN|UNDER|BELOW|OVER|ABOVE).*[0-9].*(CBM|RT|TON).*")) return true;
        if (u.matches(".*[0-9]\\s*[~\\-]\\s*[0-9].*(CBM|RT|TON).*")) return true;
        if (u.matches(".*[0-9]+\\.?[0-9]*\\s*CBM.*")) return true;
        // 税费/信息行
        if (Set.of("VAT", "SALES TAX", "GST", "NB", "INFORMATION").contains(u)) return true;
        return false;
    }

    /** 标准化计费单位 */
    public static String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) return null;
        String u = unit.trim().toUpperCase()
            .replace("PER ", "").replace("/", "").replace(" ", "");
        return switch (u) {
            case "WM", "W/M", "RT", "W/M.", "CBM", "PERWM", "PERRT",
                 "PERW/M", "WM.", "M/M" -> "WM";
            case "BL", "B/L", "PERBL", "PERB/L" -> "BL";
            case "SET", "PERSET", "SHPT" -> "SET";
            case "TON", "T", "PERTON" -> "TON";
            case "BLOCK" -> "BLOCK";
            case "KG", "100KG", "PER100KG" -> "100KG";
            default -> unit.trim();
        };
    }

    private static boolean isUnit(String s) {
        String u = normalizeUnit(s);
        return u != null && Set.of("WM", "BL", "SET", "TON", "BLOCK", "100KG").contains(u);
    }

    private static boolean isNumericOrAmount(String s) {
        if (s == null || s.isBlank()) return false;
        return s.matches(".*\\d+.*");
    }

    private static String getCellStr(Row row, int col) {
        if (row == null) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        try {
            CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType() : cell.getCellType();
            return switch (type) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> {
                    // 用 DataFormatter 获取格式化后的显示值（保留"NTD1750"等货币前缀）
                    try {
                        String formatted = new DataFormatter().formatCellValue(cell);
                        if (formatted != null && !formatted.isBlank()) {
                            yield formatted.trim();
                        }
                    } catch (Exception ignored) {}
                    double d = cell.getNumericCellValue();
                    if (d == Math.floor(d) && !Double.isInfinite(d))
                        yield String.valueOf((long) d);
                    yield String.valueOf(d);
                }
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case ERROR -> "";
                default -> "";
            };
        } catch (Exception e) { return ""; }
    }

    private record AmountInfo(String currency, BigDecimal amount) {}
}
