package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.entity.AttendanceRecord;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.repository.AttendanceRecordRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceExportService {
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    public byte[] exportAttendanceToExcel(
            UUID storeId, LocalDate fromDate, LocalDate toDate, String status) throws IOException {

        List<AttendanceRecord> records = attendanceRecordRepository.searchRecords(
                storeId, fromDate, toDate, status,
                org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Bảng công");
            createExcelHeader(sheet, workbook);
            populateAttendanceData(sheet, records, workbook);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private void createExcelHeader(Sheet sheet, XSSFWorkbook workbook) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);

        String[] headers = {
            "Mã nhân viên", "Tên nhân viên", "Cửa hàng", "Ngày",
            "Check-in", "Check-out", "Thời gian làm (phút)", "Trạng thái",
            "Vị trí Check-in", "Vị trí Check-out"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 18 * 256);
        }
    }

    private void populateAttendanceData(Sheet sheet, List<AttendanceRecord> records, XSSFWorkbook workbook) {
        CellStyle dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

        int rowIndex = 1;
        for (AttendanceRecord record : records) {
            Row row = sheet.createRow(rowIndex++);

            User user = userRepository.findById(record.getUserId()).orElse(null);
            Store store = record.getStoreId() != null ?
                storeRepository.findById(record.getStoreId()).orElse(null) : null;

            // Mã nhân viên
            row.createCell(0).setCellValue(user != null ? user.getEmployeeCode() : "");

            // Tên nhân viên
            row.createCell(1).setCellValue(user != null ? user.getFullName() : "");

            // Cửa hàng
            row.createCell(2).setCellValue(store != null ? store.getName() : "Global");

            // Ngày
            Cell dateCell = row.createCell(3);
            dateCell.setCellValue(record.getAttendanceDate().toString());
            dateCell.setCellStyle(dateStyle);

            // Check-in
            if (record.getCheckInTime() != null) {
                Cell checkInCell = row.createCell(4);
                checkInCell.setCellValue(record.getCheckInTime().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                checkInCell.setCellStyle(dateTimeStyle);
            } else {
                row.createCell(4).setCellValue("");
            }

            // Check-out
            if (record.getCheckOutTime() != null) {
                Cell checkOutCell = row.createCell(5);
                checkOutCell.setCellValue(record.getCheckOutTime().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                checkOutCell.setCellStyle(dateTimeStyle);
            } else {
                row.createCell(5).setCellValue("");
            }

            // Thời gian làm (phút)
            if (record.getCheckInTime() != null && record.getCheckOutTime() != null) {
                long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    record.getCheckInTime(), record.getCheckOutTime());
                row.createCell(6).setCellValue(minutes);
            } else {
                row.createCell(6).setCellValue("");
            }

            // Trạng thái
            row.createCell(7).setCellValue(record.getStatus());

            // Vị trí Check-in
            String checkInLocation = record.getCheckInLocationLat() != null ?
                String.format("%.6f, %.6f", record.getCheckInLocationLat(), record.getCheckInLocationLon()) :
                "";
            row.createCell(8).setCellValue(checkInLocation);

            // Vị trí Check-out
            String checkOutLocation = record.getCheckOutLocationLat() != null ?
                String.format("%.6f, %.6f", record.getCheckOutLocationLat(), record.getCheckOutLocationLon()) :
                "";
            row.createCell(9).setCellValue(checkOutLocation);
        }
    }

    public byte[] exportLeaveReportToExcel(LocalDate fromDate, LocalDate toDate) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo nghỉ phép");

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Báo cáo Nghỉ phép từ " + fromDate + " đến " + toDate);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}
