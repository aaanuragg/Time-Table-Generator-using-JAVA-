package com.timetable.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.timetable.model.DayOfWeek;
import com.timetable.model.TimetableEntry;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableExporter {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    // Export a single timetable to PDF in landscape format
    public void exportPdf(List<TimetableEntry> entries, String title, String viewType, File file) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();

        addTimetableToPdf(doc, entries, title, viewType);

        doc.close();
    }

    // Export multiple timetables into one multi-page PDF
    public void exportAllPdf(Map<String, List<TimetableEntry>> timetables, String viewType, File file) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();

        boolean first = true;
        for (Map.Entry<String, List<TimetableEntry>> entry : timetables.entrySet()) {
            if (!first) {
                doc.newPage();
            }
            addTimetableToPdf(doc, entry.getValue(), entry.getKey(), viewType);
            first = false;
        }

        doc.close();
    }

    private void addTimetableToPdf(Document doc, List<TimetableEntry> entries, String title, String viewType) throws DocumentException {
        // Timetable title
        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 41, 59));
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(10);
        doc.add(titlePara);

        // Build 6 days x 5 periods data
        String[][] grid = buildGrid(entries, viewType);
        String[] periodHeaders = getPeriodHeaders(entries);

        // 1 column for Day, 5 columns for Periods
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{14f, 17.2f, 17.2f, 17.2f, 17.2f, 17.2f});

        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, new Color(30, 41, 59));
        Font labFont = new Font(Font.HELVETICA, 8.5f, Font.BOLD, new Color(3, 105, 161));
        Font emptyFont = new Font(Font.HELVETICA, 8.5f, Font.ITALIC, new Color(156, 163, 175));

        // Header row
        PdfPCell dayHeader = new PdfPCell(new Phrase("Day", headerFont));
        dayHeader.setBackgroundColor(new Color(55, 65, 81));
        dayHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        dayHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dayHeader.setPadding(6);
        table.addCell(dayHeader);

        for (String ph : periodHeaders) {
            PdfPCell cell = new PdfPCell(new Phrase(ph, headerFont));
            cell.setBackgroundColor(new Color(55, 65, 81));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Data rows for Monday through Saturday
        DayOfWeek[] days = DayOfWeek.values();
        for (int d = 0; d < days.length; d++) {
            Font dayFont = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(55, 65, 81));
            PdfPCell dayCell = new PdfPCell(new Phrase(days[d].toString(), dayFont));
            dayCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dayCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dayCell.setPadding(6);
            dayCell.setMinimumHeight(56f);
            dayCell.setBackgroundColor(new Color(243, 244, 246));
            table.addCell(dayCell);

            for (int p = 0; p < 5; p++) {
                String cellText = grid[d][p];
                boolean isLab = cellText.contains("[LAB]");
                boolean isEmpty = "---".equals(cellText);

                Font useFont = isLab ? labFont : (isEmpty ? emptyFont : cellFont);
                Paragraph pPara = new Paragraph(cellText, useFont);
                pPara.setAlignment(Element.ALIGN_CENTER);
                pPara.setLeading(11f);

                PdfPCell cell = new PdfPCell(pPara);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(6);
                cell.setMinimumHeight(56f);

                if (isLab) {
                    cell.setBackgroundColor(new Color(224, 242, 254)); // light blue for lab cells
                } else if (isEmpty) {
                    cell.setBackgroundColor(new Color(249, 250, 251));
                } else {
                    cell.setBackgroundColor(Color.WHITE);
                }

                table.addCell(cell);
            }
        }

        doc.add(table);

        // Small footer: class/teacher name and generated date
        String dateStr = LocalDate.now().format(DATE_FMT);
        String targetName = title;
        if (targetName.endsWith(" Timetable")) {
            targetName = targetName.substring(0, targetName.length() - 10).trim();
        }

        Font footerFont = new Font(Font.HELVETICA, 8.5f, Font.ITALIC, new Color(100, 116, 139));
        Paragraph footerPara = new Paragraph(targetName + "  |  Generated on " + dateStr, footerFont);
        footerPara.setAlignment(Element.ALIGN_RIGHT);
        footerPara.setSpacingBefore(12);
        doc.add(footerPara);
    }

    // Export a single timetable to Excel (.xlsx)
    public void exportExcel(List<TimetableEntry> entries, String title, String viewType, File file) throws IOException {
        Workbook wb = new XSSFWorkbook();
        addTimetableSheet(wb, entries, title, viewType);
        try (FileOutputStream out = new FileOutputStream(file)) {
            wb.write(out);
        }
        wb.close();
    }

    private void addTimetableSheet(Workbook wb, List<TimetableEntry> entries, String title, String viewType) {
        String sheetName = title.length() > 31 ? title.substring(0, 31) : title;
        Sheet sheet = wb.createSheet(sheetName);

        // Light blue color matching PDF (RGB 224, 242, 254)
        XSSFColor lightBlueColor = new XSSFColor(new java.awt.Color(224, 242, 254), new DefaultIndexedColorMap());

        // Header style
        CellStyle headerStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFontPoi = wb.createFont();
        headerFontPoi.setBold(true);
        headerFontPoi.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFontPoi);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        setBorders(headerStyle);

        // Day column style
        CellStyle dayStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font dayFontPoi = wb.createFont();
        dayFontPoi.setBold(true);
        dayStyle.setFont(dayFontPoi);
        dayStyle.setAlignment(HorizontalAlignment.CENTER);
        dayStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dayStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        dayStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        dayStyle.setWrapText(true);
        setBorders(dayStyle);

        // Normal theory cell style
        CellStyle normalStyle = wb.createCellStyle();
        normalStyle.setAlignment(HorizontalAlignment.CENTER);
        normalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        normalStyle.setWrapText(true);
        setBorders(normalStyle);

        // Lab cell style with identical light blue background
        CellStyle labStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font labFontPoi = wb.createFont();
        labFontPoi.setBold(true);
        labFontPoi.setColor(IndexedColors.DARK_TEAL.getIndex());
        labStyle.setFont(labFontPoi);
        labStyle.setAlignment(HorizontalAlignment.CENTER);
        labStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        labStyle.setWrapText(true);
        ((XSSFCellStyle) labStyle).setFillForegroundColor(lightBlueColor);
        labStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(labStyle);

        // Title row
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(24);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        org.apache.poi.ss.usermodel.Font titleFontPoi = wb.createFont();
        titleFontPoi.setBold(true);
        titleFontPoi.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(titleFontPoi);
        titleCell.setCellStyle(titleStyle);

        // Build grid data
        String[][] grid = buildGrid(entries, viewType);
        String[] periodHeaders = getPeriodHeaders(entries);

        // Header row
        Row headerRow = sheet.createRow(2);
        headerRow.setHeightInPoints(28);
        Cell dayHeaderCell = headerRow.createCell(0);
        dayHeaderCell.setCellValue("Day");
        dayHeaderCell.setCellStyle(headerStyle);

        for (int p = 0; p < 5; p++) {
            Cell cell = headerRow.createCell(p + 1);
            cell.setCellValue(periodHeaders[p]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        DayOfWeek[] days = DayOfWeek.values();
        for (int d = 0; d < days.length; d++) {
            Row row = sheet.createRow(d + 3);
            row.setHeightInPoints(55); // Plenty of height for three lines of text

            Cell dayCellExcel = row.createCell(0);
            dayCellExcel.setCellValue(days[d].toString());
            dayCellExcel.setCellStyle(dayStyle);

            for (int p = 0; p < 5; p++) {
                Cell cell = row.createCell(p + 1);
                String text = grid[d][p];
                cell.setCellValue(text);

                if (text.contains("[LAB]")) {
                    cell.setCellStyle(labStyle);
                } else {
                    cell.setCellStyle(normalStyle);
                }
            }
        }

        // Column widths (Day column and 5 Period columns)
        sheet.setColumnWidth(0, 4200);
        for (int p = 1; p <= 5; p++) {
            sheet.setColumnWidth(p, 8000);
        }
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    // Build grid with 3 lines per cell: Subject on line 1, Teacher/Class on line 2, Room on line 3
    private String[][] buildGrid(List<TimetableEntry> entries, String viewType) {
        Map<String, TimetableEntry> entryMap = new HashMap<>();
        for (TimetableEntry te : entries) {
            entryMap.put(te.getDayOfWeek() + "_" + te.getPeriodNumber(), te);
        }

        DayOfWeek[] days = DayOfWeek.values();
        String[][] grid = new String[days.length][5];

        for (int d = 0; d < days.length; d++) {
            for (int p = 1; p <= 5; p++) {
                String key = days[d] + "_" + p;
                TimetableEntry te = entryMap.get(key);

                if (te == null) {
                    grid[d][p - 1] = "---";
                } else {
                    String labTag = te.isRequiresLab() ? " [LAB]" : "";
                    if ("Class".equals(viewType)) {
                        grid[d][p - 1] = te.getSubjectName() + labTag + "\n" + te.getTeacherName() + "\n" + te.getRoomName();
                    } else if ("Teacher".equals(viewType)) {
                        grid[d][p - 1] = te.getSubjectName() + labTag + "\n" + te.getClassName() + "\n" + te.getRoomName();
                    } else {
                        grid[d][p - 1] = te.getSubjectName() + labTag + "\n" + te.getClassName() + "\n" + te.getTeacherName();
                    }
                }
            }
        }

        return grid;
    }

    // Build period headers with time ranges
    private String[] getPeriodHeaders(List<TimetableEntry> entries) {
        String[] headers = new String[5];
        Map<Integer, TimetableEntry> periodToEntry = new HashMap<>();
        for (TimetableEntry te : entries) {
            if (!periodToEntry.containsKey(te.getPeriodNumber())) {
                periodToEntry.put(te.getPeriodNumber(), te);
            }
        }

        for (int p = 1; p <= 5; p++) {
            TimetableEntry te = periodToEntry.get(p);
            if (te != null && te.getStartTime() != null && te.getEndTime() != null) {
                headers[p - 1] = "Period " + p + "\n" + te.getStartTime().format(TIME_FMT) + " - " + te.getEndTime().format(TIME_FMT);
            } else {
                headers[p - 1] = "Period " + p;
            }
        }

        return headers;
    }
}
