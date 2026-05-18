package com.freight.util;

import com.freight.entity.FreightQuote;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class QuoteExcelParser {

    // ── 黄埔/北沙/滘心 sheet 列索引（0-based）──
    // A=国家 B=目的港 C=体积 D=中转 E=MIN
    // F=乌冲OF G=乌冲头程 H=乌冲大船 I=北沙OF J=北沙头程 K=北沙大船
    // L=滘心OF M=滘心头程 N=滘心大船 O=T/T P=CC Q=CARRIER R=REMARKS
    private static final int COL_COUNTRY = 0;
    private static final int COL_DEST = 1;
    private static final int COL_VOLUME = 2;
    private static final int COL_VIA = 3;
    private static final int COL_MIN = 4;
    private static final int COL_OF_WUCHONG = 5;
    private static final int COL_WUCHONG_FIRST = 6;
    private static final int COL_WUCHONG_MOTHER = 7;
    private static final int COL_OF_BEISHA = 8;
    private static final int COL_BEISHA_FIRST = 9;
    private static final int COL_BEISHA_MOTHER = 10;
    private static final int COL_OF_JIAOXIN = 11;
    private static final int COL_JIAOXIN_FIRST = 12;
    private static final int COL_JIAOXIN_MOTHER = 13;
    private static final int COL_TT = 14;
    private static final int COL_CARRIER = 16;
    private static final int COL_REMARKS = 17;

    // ── 南沙仓 sheet 列索引（0-based）──
    // A=目的港 B=体积 C=中转 D=MIN E=OF F=船期 G=T/T H=CC I=CARRIER J=仓库 K=REMARKS
    private static final int NS_COL_DEST = 0;
    private static final int NS_COL_VOLUME = 1;
    private static final int NS_COL_VIA = 2;
    private static final int NS_COL_MIN = 3;
    private static final int NS_COL_OF = 4;
    private static final int NS_COL_SCHEDULE = 5;
    private static final int NS_COL_TT = 6;
    private static final int NS_COL_CARRIER = 8;
    private static final int NS_COL_WAREHOUSE = 9;
    private static final int NS_COL_REMARKS = 10;

    /**
     * 解析Excel文件，提取所有报价行
     */
    public static List<FreightQuote> parse(InputStream in, String fileName,
                                           LocalDate validFrom, LocalDate validTo) throws Exception {
        List<FreightQuote> result = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet1 = wb.getSheet("黄埔,北沙仓,滘心");
            if (sheet1 != null) {
                result.addAll(parseMainSheet(sheet1, "黄埔,北沙仓,滘心", validFrom, validTo));
            }
            Sheet sheet2 = wb.getSheet("南沙仓");
            if (sheet2 != null) {
                result.addAll(parseNanshaSheet(sheet2, "南沙仓", validFrom, validTo));
            }
        }
        return result;
    }

    /**
     * 解析黄埔/北沙/滘心 sheet（数据从第9行开始，索引8）
     */
    private static List<FreightQuote> parseMainSheet(Sheet sheet, String sheetName,
                                                      LocalDate validFrom, LocalDate validTo) {
        List<FreightQuote> list = new ArrayList<>();
        String currentCountry = null;
        String currentDest = null;
        String currentCarrier = null;
        String currentTT = null;
        String currentRemarks = null;
        String currentVia = null;
        // 头程/大船 carry-forward（第一行有，后续行单元格为空时沿用上一行）
        String curWuchongFirst = null, curWuchongMother = null;
        String curBeishaFirst = null, curBeishaMother = null;
        String curJiaoxinFirst = null, curJiaoxinMother = null;

        for (int r = 8; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String country = cleanCell(row, COL_COUNTRY);
            String dest = cleanCell(row, COL_DEST);
            String volume = cleanCell(row, COL_VOLUME);
            String via = cleanCell(row, COL_VIA);
            String minStr = cleanCell(row, COL_MIN);
            String ofWuchong = cleanCell(row, COL_OF_WUCHONG);
            String wuchongFirst = cleanCell(row, COL_WUCHONG_FIRST);
            String wuchongMother = cleanCell(row, COL_WUCHONG_MOTHER);
            String ofBeisha = cleanCell(row, COL_OF_BEISHA);
            String beishaFirst = cleanCell(row, COL_BEISHA_FIRST);
            String beishaMother = cleanCell(row, COL_BEISHA_MOTHER);
            String ofJiaoxin = cleanCell(row, COL_OF_JIAOXIN);
            String jiaoxinFirst = cleanCell(row, COL_JIAOXIN_FIRST);
            String jiaoxinMother = cleanCell(row, COL_JIAOXIN_MOTHER);
            String tt = cleanCell(row, COL_TT);
            String carrier = cleanCell(row, COL_CARRIER);
            String remarks = cleanCell(row, COL_REMARKS);

            // 更新 carry-forward 值
            if (!country.isEmpty()) currentCountry = country;
            if (!dest.isEmpty()) currentDest = dest;
            if (!carrier.isEmpty()) currentCarrier = carrier;
            if (!tt.isEmpty()) currentTT = tt;
            if (!remarks.isEmpty()) currentRemarks = remarks;
            if (!via.isEmpty()) currentVia = via;
            if (!wuchongFirst.isEmpty()) curWuchongFirst = wuchongFirst;
            if (!wuchongMother.isEmpty()) curWuchongMother = wuchongMother;
            if (!beishaFirst.isEmpty()) curBeishaFirst = beishaFirst;
            if (!beishaMother.isEmpty()) curBeishaMother = beishaMother;
            if (!jiaoxinFirst.isEmpty()) curJiaoxinFirst = jiaoxinFirst;
            if (!jiaoxinMother.isEmpty()) curJiaoxinMother = jiaoxinMother;

            // 跳过无体积区间的行（非数据行）
            if (volume.isEmpty() || currentDest == null) continue;
            if (!isVolumeLine(volume)) continue;

            FreightQuote q = new FreightQuote();
            q.setSourceSheet(sheetName);
            q.setCountry(currentCountry);
            q.setDestination(currentDest);
            q.setVolumeRange(volume);
            q.setVia(currentVia.isEmpty() ? null : currentVia);
            q.setMinCharge(parseIntSafe(minStr));
            q.setOfWuchong(ofWuchong.isEmpty() ? null : ofWuchong);
            q.setWuchongFirstLeg(wuchongFirst.isEmpty() ? curWuchongFirst : wuchongFirst);
            q.setWuchongMotherVessel(wuchongMother.isEmpty() ? curWuchongMother : wuchongMother);
            q.setOfBeisha(ofBeisha.isEmpty() ? null : filterFormula(ofBeisha));
            q.setBeishaFirstLeg(beishaFirst.isEmpty() ? curBeishaFirst : beishaFirst);
            q.setBeishaMotherVessel(beishaMother.isEmpty() ? curBeishaMother : beishaMother);
            q.setOfJiaoxin(ofJiaoxin.isEmpty() ? null : ofJiaoxin);
            q.setJiaoxinFirstLeg(jiaoxinFirst.isEmpty() ? curJiaoxinFirst : jiaoxinFirst);
            q.setJiaoxinMotherVessel(jiaoxinMother.isEmpty() ? curJiaoxinMother : jiaoxinMother);
            q.setTransitTime(currentTT);
            q.setCarrier(currentCarrier);
            q.setRemarks(currentRemarks);
            q.setValidFrom(validFrom);
            q.setValidTo(validTo);
            parseVolumeRange(q, volume);
            list.add(q);
        }
        return list;
    }

    /**
     * 解析南沙仓 sheet（数据从第7行开始，索引6）
     */
    private static List<FreightQuote> parseNanshaSheet(Sheet sheet, String sheetName,
                                                        LocalDate validFrom, LocalDate validTo) {
        List<FreightQuote> list = new ArrayList<>();
        String currentDest = null;
        String currentCarrier = null;
        String currentTT = null;
        String currentRemarks = null;
        String currentSchedule = null;

        for (int r = 6; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String dest = cleanCell(row, NS_COL_DEST);
            String volume = cleanCell(row, NS_COL_VOLUME);
            String via = cleanCell(row, NS_COL_VIA);
            String minStr = cleanCell(row, NS_COL_MIN);
            String of = cleanCell(row, NS_COL_OF);
            String schedule = cleanCell(row, NS_COL_SCHEDULE);
            String tt = cleanCell(row, NS_COL_TT);
            String carrier = cleanCell(row, NS_COL_CARRIER);
            String warehouse = cleanCell(row, NS_COL_WAREHOUSE);
            String remarks = cleanCell(row, NS_COL_REMARKS);

            if (!dest.isEmpty()) currentDest = dest;
            if (!carrier.isEmpty()) currentCarrier = carrier;
            if (!tt.isEmpty()) currentTT = tt;
            if (!remarks.isEmpty()) currentRemarks = remarks;
            if (!schedule.isEmpty()) currentSchedule = schedule;

            if (volume.isEmpty() || currentDest == null) continue;
            if (!isVolumeLine(volume)) continue;

            FreightQuote q = new FreightQuote();
            q.setSourceSheet(sheetName);
            q.setCountry(null);
            q.setDestination(currentDest);
            q.setVolumeRange(volume);
            q.setVia(via.isEmpty() ? null : via);
            q.setMinCharge(parseIntSafe(minStr));
            q.setOfWuchong(null);
            q.setOfBeisha(null);
            q.setOfJiaoxin(of.isEmpty() ? null : of);
            // 船期存到滘心头程，当前行为空时用 carry-forward
            q.setJiaoxinFirstLeg(schedule.isEmpty() ? currentSchedule : schedule);
            q.setTransitTime(currentTT);
            q.setCarrier(currentCarrier);
            q.setRemarks(currentRemarks);
            q.setValidFrom(validFrom);
            q.setValidTo(validTo);
            parseVolumeRange(q, volume);
            list.add(q);
        }
        return list;
    }

    /** 过滤公式引用（如 =H9），公式单元格已经通过 getCellString 处理，这里处理可能残留的 = 前缀 */
    private static String filterFormula(String s) {
        if (s == null || s.isEmpty()) return null;
        if (s.startsWith("=") || s.equals("\\")) return null;
        return s;
    }

    /** 判断是否是体积区间数据行 */
    private static boolean isVolumeLine(String s) {
        if (s == null || s.isBlank()) return false;
        String u = s.toUpperCase();
        return u.contains("CBM") || u.contains("以内") || u.contains("以上")
            || u.contains("-") || u.contains(">") || u.contains("TONS")
            || u.contains("CASE");
    }

    /**
     * 解析体积区间字符串为 volumeMin / volumeMax
     */
    public static void parseVolumeRange(FreightQuote q, String volumeStr) {
        if (volumeStr == null || volumeStr.isBlank()) return;
        String s = volumeStr.trim().toUpperCase()
                .replace("CBM以内", "").replace("CBM以上", "+")
                .replace("CBM", "").replace("以内", "").replace("以上", "+")
                .replace("/5TONS内", "").replace("内", "")
                .replace(">", "").replace("＞", "")
                .trim();

        try {
            if (s.contains("-")) {
                String[] parts = s.split("-");
                q.setVolumeMin(new BigDecimal(parts[0].trim()));
                q.setVolumeMax(new BigDecimal(parts[1].trim()));
            } else if (s.endsWith("+")) {
                q.setVolumeMin(new BigDecimal(s.replace("+", "").trim()));
                q.setVolumeMax(null);
            } else {
                BigDecimal val = new BigDecimal(s.trim());
                q.setVolumeMin(BigDecimal.ZERO);
                q.setVolumeMax(val);
            }
        } catch (Exception e) {
            log.debug("体积区间解析失败: {}", volumeStr);
        }
    }

    /**
     * 从文件名解析有效期
     */
    public static LocalDate[] parseDateFromFileName(String fileName) {
        // 支持 "崴航2026年5月15日~5月21日有效散货报价表.xlsx" 和 "崴航2026年5月15日_5月21日有效散货报价表.xlsx"
        Pattern p = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日[~_](\\d{1,2})月(\\d{1,2})日");
        Matcher m = p.matcher(fileName);
        if (m.find()) {
            int year = Integer.parseInt(m.group(1));
            int month1 = Integer.parseInt(m.group(2));
            int day1 = Integer.parseInt(m.group(3));
            int month2 = Integer.parseInt(m.group(4));
            int day2 = Integer.parseInt(m.group(5));
            return new LocalDate[]{
                LocalDate.of(year, month1, day1),
                LocalDate.of(year, month2, day2)
            };
        }
        return new LocalDate[]{LocalDate.now(), LocalDate.now().plusDays(6)};
    }

    // ── 工具方法 ──

    private static String cleanCell(Row row, int col) {
        if (row == null) return "";
        Cell cell = row.getCell(col);
        return getCellString(cell);
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        try {
            CellType type = cell.getCellType() == CellType.FORMULA
                    ? cell.getCachedFormulaResultType()
                    : cell.getCellType();
            return switch (type) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> {
                    double d = cell.getNumericCellValue();
                    if (d == Math.floor(d) && !Double.isInfinite(d))
                        yield String.valueOf((long) d);
                    yield String.valueOf(d);
                }
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case ERROR -> "";
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private static Integer parseIntSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim().replace("+", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
