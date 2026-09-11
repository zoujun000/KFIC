package com.freight.util;

import com.freight.entity.FreightQuote;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class QuoteExcelParser {

    // ── 港口缩写映射表：value[0]=港口名关键词, value[1]=缩写 ──
    private static final String[][] PORT_CODE_MAP = {
        {"SINGAPORE", "SIN"},
        {"BUSAN", "BUS"},
        {"DUBAI", "DXB"}, {"JEBEL ALI", "DXB"},
        {"NHAVA SHEVA", "NVS"}, {"NHAVA", "NVS"},
        {"CHENNAI", "CNN"}, {"MADRAS", "CNN"},
        {"COLOMBO", "CMB"},
        {"HAMBURG", "HAM"},
        {"CONSTANTA", "CDN"},
        {"CONSTANTZA", "CDN"},
        {"AARHUS", "AAR"}, {"COPENHAGEN", "CPH"},
        {"HELSINKI", "HEL"}, {"ODESSA", "ODS"},
        {"GOTHENBURG", "GTB"}, {"OSLO", "OSL"},
        {"GENOVA", "GOA"}, {"ALIAGA", "ALI"}, {"ALSANCAK", "ALS"},
        {"MALTA", "MLA"},
        {"FELIXSTOWE", "FLX"},
        {"LE HAVRE", "LHV"},
        {"ROTTERDAM", "RTM"},
        {"BARCELONA", "BAR"},
        {"GENOA", "GOA"},
        {"ISTANBUL", "IST"}, {"AMBARLI", "IST"},
        {"AUCKLAND", "ACK"},
        {"BRISBANE", "BSB"},
        {"CAUCEDO", "CAU"}, {"ACAJUTLA", "ACJ"},
        {"PUERTO QUETZAL", "PTQ"}, {"MANAGUA", "MNG"},
        {"SAN PEDRO SULA", "SPS"},
        {"SANTOS", "STS"},
        {"MANZANILLO", "MZL"},
        {"COLON FREE ZONE", "CLN"}, {"COLON", "CLN"},
        {"JEDDAH", "JED"},
        {"BANGKOK", "BKK"},
        {"HOCHIMINH", "SGN"}, {"HO CHI MINH", "SGN"},
        {"HAIPHONG", "HPH"},
        {"JAKARTA", "JKT"},
        {"PENANG", "PNG"}, {"PASIR GUDANG", "PGD"},
        {"YANGON", "YGN"}, {"CEBU", "CBU"}, {"DANANG", "DAN"},
        {"SIHANOUKVILLE", "SIH"},
        {"SHIMIZU", "SHI"}, {"HAKATA", "HKT"}, {"MOJI", "MOJ"},
        {"PORT KLANG", "PKG"},
        {"MANILA", "MNL"},
        {"TOKYO", "TYO"},
        {"YOKOHAMA", "YOK"},
        {"OSAKA", "OSA"},
        {"KOBE", "UKB"},
        {"NAGOYA", "NGO"},
        {"INCHEON", "INC"}, {"INCHON", "INC"},
        {"MELBOURNE", "MEL"},
        {"SYDNEY", "SYD"},
        {"LOS ANGELES", "LAX"}, {"LONG BEACH", "LGB"},
        {"NEW YORK", "NYC"}, {"NEWARK", "EWR"},
        {"MONTREAL", "YUL"},
        {"TORONTO", "YYZ"},
        {"VANCOUVER", "YVR"},
        {"ROTTERDAM", "RTM"},
        {"FELIXSTOWE", "FLX"},
        {"KARACHI", "KHI"},
        {"CHITTAGONG", "CGP"},
        {"MOMBASA", "MBA"},
        {"TEMA", "TMA"},
        {"DURBAN", "DUR"},
        {"JOHANNESBURG", "JNB"},
        {"CAPE TOWN", "CPT"},
        {"BUENOS AIRES", "BUE"},
        {"MONTEVIDEO", "MVD"}, {"NAVEGANTES", "NAV"},
        {"GUAYAQUIL", "GYE"},
        {"BUENAVENTURA", "BUN"},
        {"CALLAO", "CLL"},
        {"VALPARAISO", "VAP"},
        {"SAN JOSE", "SJO"},
        {"ALEXANDRIA", "ALY"},
        {"CASABLANCA", "CAS"},
        {"ASHDOD", "ASD"},
        {"HAIFA", "HFA"},
        {"PIRAEUS", "PIR"},
        {"LIMASSOL", "LMS"},
        {"THESSALONIKI", "SKG"},
        {"GDYNIA", "GDY"}, {"GDANSK", "GDN"},
        {"KEELUNG", "KEL"}, {"TAICHUNG", "TXG"}, {"KAOHSIUNG", "KHH"},
        {"LAEM CHABANG", "LCH"},
        {"LAT KRABANG", "LKB"},
        {"PORT LOUIS", "PTL"},
        {"CHICAGO", "CCG"}, {"PRAGUE", "PRG"},
        {"SOKHNA", "SOK"},
        {"DAMMAM", "DMM"},
        {"ABU DHABI", "AUH"},
        {"DOHA", "DOH"},
        {"KUWAIT", "KWI"},
        {"MUSCAT", "MCT"},
        {"AQABA", "AQJ"},
    };

    /** 根据目的港名称解析港口缩写 */
    public static String resolvePortCode(String destination) {
        if (destination == null || destination.isBlank()) return null;
        String upper = destination.toUpperCase();
        for (String[] entry : PORT_CODE_MAP) {
            if (upper.contains(entry[0])) return entry[1];
        }
        return null;
    }

    /** 解析中转船期：头程/大船字段含"见XXX船期"时，替换为实际船期 */
    private static final Pattern TRANSIT_PATTERN = Pattern.compile(
        "见\\s*([A-Z]{2,4})\\s*船期(?:\\+?(\\d+)\\s*天)?");

    public static void resolveTransitSchedule(List<FreightQuote> quotes) {
        // 先构建 portCode → FreightQuote 索引
        Map<String, FreightQuote> codeIndex = new HashMap<>();
        for (FreightQuote q : quotes) {
            if (q.getPortCode() != null && !q.getPortCode().isBlank()) {
                codeIndex.putIfAbsent(q.getPortCode().trim().toUpperCase(), q);
            }
        }

        for (FreightQuote q : quotes) {
            resolveTransitField(q.getWuchongFirstLeg(), q.getWuchongMotherVessel(),
                    codeIndex, q, "wuchong");
            resolveTransitField(q.getJiaoxinFirstLeg(), q.getJiaoxinMotherVessel(),
                    codeIndex, q, "jiaoxin");
        }
    }

    private static void resolveTransitField(String firstLeg, String motherVessel,
                                            Map<String, FreightQuote> codeIndex,
                                            FreightQuote target, String wh) {
        String combined = (firstLeg != null ? firstLeg : "") + (motherVessel != null ? motherVessel : "");
        if (combined.isBlank()) return;

        Matcher m = TRANSIT_PATTERN.matcher(combined);
        if (!m.find()) return;

        String transitCode = m.group(1);
        int extraDays = 0;
        if (m.group(2) != null) extraDays = Integer.parseInt(m.group(2));

        FreightQuote transitQuote = codeIndex.get(transitCode);
        if (transitQuote == null) {
            log.debug("中转港口未找到: portCode={}", transitCode);
            return;
        }

        // 复制中转港口的船期
        switch (wh) {
            case "wuchong" -> {
                target.setWuchongFirstLeg(transitQuote.getWuchongFirstLeg());
                target.setWuchongMotherVessel(transitQuote.getWuchongMotherVessel());
            }
            case "jiaoxin" -> {
                target.setJiaoxinFirstLeg(transitQuote.getJiaoxinFirstLeg());
                target.setJiaoxinMotherVessel(transitQuote.getJiaoxinMotherVessel());
            }
        }

        // 时效 = 中转港口的时效 + 额外天数
        if (transitQuote.getTransitTime() != null) {
            try {
                int baseTT = Integer.parseInt(transitQuote.getTransitTime().replaceAll("[^0-9]", ""));
                target.setTransitTime(String.valueOf(baseTT + extraDays));
            } catch (NumberFormatException e) {
                target.setTransitTime(transitQuote.getTransitTime() + "+" + extraDays);
            }
        }
    }

    // ── 黄埔/滘心 sheet 列索引（0-based）──
    // A=国家 B=目的港 C=体积 D=中转 E=MIN
    // F=乌冲OF G=乌冲头程 H=乌冲大船 I=滘心OF J=滘心头程 K=滘心大船
    // L=T/T M=CC N=CARRIER O=注意事项
    private static final int COL_COUNTRY = 0;
    private static final int COL_DEST = 1;
    private static final int COL_VOLUME = 2;
    private static final int COL_VIA = 3;
    private static final int COL_MIN = 4;
    private static final int COL_OF_WUCHONG = 5;
    private static final int COL_WUCHONG_FIRST = 6;
    private static final int COL_WUCHONG_MOTHER = 7;
    private static final int COL_OF_JIAOXIN = 8;
    private static final int COL_JIAOXIN_FIRST = 9;
    private static final int COL_JIAOXIN_MOTHER = 10;
    private static final int COL_TT = 11;
    private static final int COL_CC = 12;
    private static final int COL_CARRIER = 13;
    private static final int COL_REMARKS = 14;

    // ── 南沙仓 sheet 列索引（0-based）──
    // A=目的港 B=体积 C=中转 D=MIN E=OF F=船期 G=T/T H=CC I=CARRIER J=仓库 K=REMARKS
    private static final int NS_COL_DEST = 0;
    private static final int NS_COL_VOLUME = 1;
    private static final int NS_COL_VIA = 2;
    private static final int NS_COL_MIN = 3;
    private static final int NS_COL_OF = 4;
    private static final int NS_COL_SCHEDULE = 5;
    private static final int NS_COL_TT = 6;
    private static final int NS_COL_CC = 7;
    private static final int NS_COL_CARRIER = 8;
    private static final int NS_COL_WAREHOUSE = 9;
    private static final int NS_COL_REMARKS = 10;

    private static final String VOLUME_NUMBER = "\\d+(?:\\.\\d+)?";
    private static final String VOLUME_BOUND = "(?:[<>≤≥＜＞]=?\\s*)?" + VOLUME_NUMBER;
    private static final Pattern VOLUME_LINE_PATTERN = Pattern.compile(
        "(?i)^\\s*(?:CASE\\s+BY\\s+CASE|"
            + VOLUME_BOUND
            + "(?:\\s*(?:CBM|TONS?|CASE))?"
            + "(?:\\s*(?:-|～|~|至)\\s*"
            + VOLUME_BOUND
            + "(?:\\s*(?:CBM|TONS?|CASE))?)?"
            + "(?:\\s*/\\s*" + VOLUME_NUMBER + "\\s*TONS?)?"
            + "(?:\\s*(?:以内|以上|内|\\+))?)\\s*$");

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
        // 补充港口缩写
        for (FreightQuote q : result) {
            if (q.getPortCode() == null) {
                q.setPortCode(resolvePortCode(q.getDestination()));
            }
        }
        // 解析中转船期引用（见XXX船期 → 实际船期+时效）
        resolveTransitSchedule(result);
        return result;
    }

    /** 解析黄埔/滘心 sheet（数据从第9行开始，索引8） */
    private static List<FreightQuote> parseMainSheet(Sheet sheet, String sheetName,
                                                      LocalDate validFrom, LocalDate validTo) {
        List<FreightQuote> list = new ArrayList<>();
        String currentCountry = null;
        String currentDest = null;
        String currentCarrier = null;
        String currentTT = null;
        String currentRemarks = null;
        String currentVia = "";
        // 头程/大船 carry-forward（第一行有，后续行单元格为空时沿用上一行）
        String curWuchongFirst = null, curWuchongMother = null;
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
            String ofJiaoxin = cleanCell(row, COL_OF_JIAOXIN);
            String jiaoxinFirst = cleanCell(row, COL_JIAOXIN_FIRST);
            String jiaoxinMother = cleanCell(row, COL_JIAOXIN_MOTHER);
            String tt = cleanCell(row, COL_TT);
            String cc = cleanCell(row, COL_CC);
            String carrier = cleanCell(row, COL_CARRIER);
            String remarks = cleanCell(row, COL_REMARKS);

            validateLength(tt, 100, r + 1, "T/T");
            validateLength(cc, 50, r + 1, "CC");
            validateLength(carrier, 100, r + 1, "CARRIER");

            // 更新 carry-forward 值
            if (!country.isEmpty()) currentCountry = country;
            if (!dest.isEmpty()) currentDest = dest;
            if (!carrier.isEmpty()) currentCarrier = carrier;
            if (!tt.isEmpty()) currentTT = tt;
            if (!remarks.isEmpty()) currentRemarks = remarks;
            if (!via.isEmpty()) currentVia = via;
            if (!wuchongFirst.isEmpty()) curWuchongFirst = wuchongFirst;
            if (!wuchongMother.isEmpty()) curWuchongMother = wuchongMother;
            if (!jiaoxinFirst.isEmpty()) curJiaoxinFirst = jiaoxinFirst;
            if (!jiaoxinMother.isEmpty()) curJiaoxinMother = jiaoxinMother;

            // 跳过无体积区间的行（非数据行）
            if (volume.isEmpty() || currentDest == null) continue;
            // Excel 合并单元格产生的辅助行只有体积和船期公式，没有报价数据
            if (dest.isEmpty() && !hasQuoteValues(via, minStr, ofWuchong, ofJiaoxin, tt, cc, carrier)) {
                continue;
            }
            if (!isVolumeLine(volume)) continue;
            validateLength(volume, 50, r + 1, "体积分段");

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
            q.setOfJiaoxin(ofJiaoxin.isEmpty() ? null : ofJiaoxin);
            q.setJiaoxinFirstLeg(jiaoxinFirst.isEmpty() ? curJiaoxinFirst : jiaoxinFirst);
            q.setJiaoxinMotherVessel(jiaoxinMother.isEmpty() ? curJiaoxinMother : jiaoxinMother);
            q.setTransitTime(currentTT);
            q.setCc(cc.isEmpty() ? null : cc);
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
            String cc = cleanCell(row, NS_COL_CC);
            String carrier = cleanCell(row, NS_COL_CARRIER);
            String warehouse = cleanCell(row, NS_COL_WAREHOUSE);
            String remarks = cleanCell(row, NS_COL_REMARKS);

            validateLength(tt, 100, r + 1, "T/T");
            validateLength(cc, 50, r + 1, "CC");
            validateLength(carrier, 100, r + 1, "CARRIER");

            if (!dest.isEmpty()) currentDest = dest;
            if (!carrier.isEmpty()) currentCarrier = carrier;
            if (!tt.isEmpty()) currentTT = tt;
            if (!remarks.isEmpty()) currentRemarks = remarks;
            if (!schedule.isEmpty()) currentSchedule = schedule;

            if (volume.isEmpty() || currentDest == null) continue;
            if (!isVolumeLine(volume)) continue;
            validateLength(volume, 50, r + 1, "体积分段");

            FreightQuote q = new FreightQuote();
            q.setSourceSheet(sheetName);
            q.setCountry(null);
            q.setDestination(currentDest);
            q.setVolumeRange(volume);
            q.setVia(via.isEmpty() ? null : via);
            q.setMinCharge(parseIntSafe(minStr));
            q.setOfWuchong(null);
            q.setOfJiaoxin(of.isEmpty() ? null : of);
            // 船期存到滘心头程，当前行为空时用 carry-forward
            q.setJiaoxinFirstLeg(schedule.isEmpty() ? currentSchedule : schedule);
            q.setTransitTime(currentTT);
            q.setCc(cc.isEmpty() ? null : cc);
            q.setCarrier(currentCarrier);
            q.setRemarks(currentRemarks);
            q.setValidFrom(validFrom);
            q.setValidTo(validTo);
            parseVolumeRange(q, volume);
            list.add(q);
        }
        return list;
    }

    /** 判断是否是体积区间数据行 */
    private static boolean isVolumeLine(String s) {
        if (s == null || s.isBlank()) return false;
        String[] lines = s.replace('\r', '\n').split("\\n");
        for (String line : lines) {
            if (!VOLUME_LINE_PATTERN.matcher(line).matches()) return false;
        }
        return true;
    }

    private static boolean hasQuoteValues(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return true;
        }
        return false;
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

    private static void validateLength(String value, int max, int rowNo, String column) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException("第" + rowNo + "行" + column + "超过" + max + "个字符");
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
