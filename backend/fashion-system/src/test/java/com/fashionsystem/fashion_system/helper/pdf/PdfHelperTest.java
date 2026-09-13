package com.fashionsystem.fashion_system.helper.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfHelperTest {
    private final PdfHelper helper = new PdfHelper();

    @Test
    void generatesParagraphTableAndVietnameseText() throws Exception {
        PdfDocumentData data = new PdfDocumentData("Báo cáo tiếng Việt", "Tiêu đề đầu trang", "Chân trang",
                List.of(
                        new PdfText("Cộng hòa xã hội chủ nghĩa Việt Nam – Độc lập, Tự do, Hạnh phúc"),
                        new PdfTable(List.of("Tên", "Giá trị"), List.of(List.of("Sản phẩm", "125.000 ₫")))),
                54, 54, 54, 54);

        byte[] bytes = helper.generatePdf(data);

        try (PDDocument document = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(text).contains("Báo cáo tiếng Việt", "Độc lập", "Sản phẩm", "Trang 1/1");
        }
    }

    @Test
    void automaticallyCreatesMultiplePages() throws Exception {
        List<PdfElement> elements = new ArrayList<>();
        for (int index = 0; index < 180; index++) elements.add(new PdfText("Dòng dữ liệu số " + index));
        byte[] bytes = helper.generatePdf(new PdfDocumentData("Nhiều trang", elements));
        try (PDDocument document = Loader.loadPDF(bytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(2);
        }
    }

    @Test
    void generatesInvoiceFromStandaloneViewModel() throws Exception {
        InvoicePdfData invoice = new InvoicePdfData(
                "INV-001", LocalDate.of(2026, 9, 1), "Công ty Thời Trang", "Nguyễn Văn An", "Hà Nội",
                List.of(new InvoiceItemPdfData("Áo sơ mi", BigDecimal.valueOf(2),
                        BigDecimal.valueOf(250000), BigDecimal.valueOf(500000))),
                BigDecimal.valueOf(500000), BigDecimal.valueOf(50000), BigDecimal.valueOf(550000), "VND");

        byte[] bytes = helper.generateInvoicePdf(invoice);

        try (PDDocument document = Loader.loadPDF(bytes)) {
            assertThat(new PDFTextStripper().getText(document)).contains("HÓA ĐƠN", "INV-001", "Áo sơ mi", "550.000");
        }
    }
}
