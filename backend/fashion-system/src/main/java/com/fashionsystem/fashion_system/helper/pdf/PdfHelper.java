package com.fashionsystem.fashion_system.helper.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** PDFBox-based renderer with embedded Unicode fonts and automatic page breaks. */
@Component
public class PdfHelper {
    private static final String REGULAR_FONT = "fonts/NotoSerif-Regular.ttf";
    private static final String BOLD_FONT = "fonts/NotoSerif-Bold.ttf";

    public byte[] generatePdf(String text) {
        return generatePdf(new PdfDocumentData(null, List.of(new PdfText(text))));
    }

    public byte[] generatePdf(PdfDocumentData data) {
        if (data == null) throw new IllegalArgumentException("PDF document data is required");
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont regular = loadFont(document, REGULAR_FONT);
            PDFont bold = loadFont(document, BOLD_FONT);
            RenderState state = new RenderState(document, data, regular, bold);
            state.newPage();
            if (StringUtils.hasText(data.title())) {
                state.renderText(new PdfText(data.title(), 18, true, PdfAlignment.CENTER, 16));
            }
            for (PdfElement element : data.elements()) state.render(element);
            state.closeContent();
            state.addFootersAndPageNumbers();
            document.save(output);
            return output.toByteArray();
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalArgumentException illegalArgument) throw illegalArgument;
            throw new PdfGenerationException("PDF generation failed", exception);
        }
    }

    public byte[] generateInvoicePdf(InvoicePdfData invoice) {
        if (invoice == null) throw new IllegalArgumentException("Invoice data is required");
        NumberFormat money = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        List<List<String>> rows = invoice.items().stream().map(item -> List.of(
                value(item.description()), decimal(item.quantity()), money(item.unitPrice(), money), money(item.amount(), money)))
                .toList();
        List<PdfElement> elements = new ArrayList<>();
        elements.add(new PdfText("Số hóa đơn: " + value(invoice.invoiceNumber()), 11, false, PdfAlignment.LEFT, 4));
        elements.add(new PdfText("Ngày: " + value(invoice.issueDate()), 11, false, PdfAlignment.LEFT, 10));
        elements.add(new PdfText("Đơn vị bán: " + value(invoice.sellerName())));
        elements.add(new PdfText("Khách hàng: " + value(invoice.buyerName())));
        elements.add(new PdfText("Địa chỉ: " + value(invoice.buyerAddress()), 11, false, PdfAlignment.LEFT, 12));
        elements.add(new PdfTable(List.of("Mô tả", "Số lượng", "Đơn giá", "Thành tiền"), rows,
                List.of(0.46f, 0.14f, 0.20f, 0.20f), 9));
        elements.add(new PdfText("Tạm tính: " + money(invoice.subtotal(), money), 11, false, PdfAlignment.RIGHT, 4));
        elements.add(new PdfText("Thuế: " + money(invoice.tax(), money), 11, false, PdfAlignment.RIGHT, 4));
        elements.add(new PdfText("Tổng cộng: " + money(invoice.total(), money) + " " + value(invoice.currency()),
                12, true, PdfAlignment.RIGHT, 8));
        return generatePdf(new PdfDocumentData("HÓA ĐƠN", null, null, elements, 54, 54, 54, 54));
    }

    private PDFont loadFont(PDDocument document, String path) throws IOException {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return PDType0Font.load(document, input, true);
        }
    }

    private String value(Object value) { return value == null ? "" : value.toString(); }
    private String decimal(BigDecimal value) { return value == null ? "" : value.stripTrailingZeros().toPlainString(); }
    private String money(BigDecimal value, NumberFormat format) { return value == null ? "" : format.format(value); }

    private static final class RenderState {
        private static final float LEADING_FACTOR = 1.35f;
        private final PDDocument document;
        private final PdfDocumentData data;
        private final PDFont regular;
        private final PDFont bold;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        private RenderState(PDDocument document, PdfDocumentData data, PDFont regular, PDFont bold) {
            this.document = document;
            this.data = data;
            this.regular = regular;
            this.bold = bold;
        }

        private void render(PdfElement element) throws IOException {
            if (element == null) return;
            if (element instanceof PdfText text) renderText(text);
            else if (element instanceof PdfTable table) renderTable(table);
            else if (element instanceof PdfImage image) renderImage(image);
        }

        private void newPage() throws IOException {
            closeContent();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - data.marginTop();
            if (StringUtils.hasText(data.header())) {
                drawLine(data.header(), regular, 9, data.marginLeft(), y + 20);
            }
        }

        private void renderText(PdfText text) throws IOException {
            if (text.text() == null) return;
            float size = text.fontSize() > 0 ? text.fontSize() : 11;
            PDFont font = text.bold() ? bold : regular;
            float width = usableWidth();
            for (String paragraph : text.text().split("\\R", -1)) {
                List<String> lines = wrap(paragraph, font, size, width);
                for (String line : lines) {
                    ensureSpace(size * LEADING_FACTOR);
                    float lineWidth = textWidth(line, font, size);
                    float x = alignedX(text.alignment(), lineWidth);
                    drawLine(line, font, size, x, y);
                    y -= size * LEADING_FACTOR;
                }
            }
            y -= Math.max(0, text.spacingAfter());
        }

        private void renderTable(PdfTable table) throws IOException {
            if (table.headers().isEmpty()) return;
            int columns = table.headers().size();
            float[] widths = columnWidths(table.columnWidths(), columns);
            drawTableRow(table.headers(), widths, table.fontSize(), true);
            for (List<String> row : table.rows()) {
                List<String> normalized = new ArrayList<>(row);
                while (normalized.size() < columns) normalized.add("");
                drawTableRow(normalized.subList(0, columns), widths, table.fontSize(), false);
            }
            y -= 10;
        }

        private void drawTableRow(List<String> cells, float[] widths, float size, boolean header) throws IOException {
            float padding = 4;
            List<List<String>> wrapped = new ArrayList<>();
            int maxLines = 1;
            for (int i = 0; i < widths.length; i++) {
                List<String> lines = wrap(valueAt(cells, i), header ? bold : regular, size, widths[i] - padding * 2);
                wrapped.add(lines);
                maxLines = Math.max(maxLines, lines.size());
            }
            float height = maxLines * size * LEADING_FACTOR + padding * 2;
            ensureSpace(height);
            float x = data.marginLeft();
            for (int i = 0; i < widths.length; i++) {
                if (header) {
                    content.setNonStrokingColor(new Color(230, 234, 239));
                    content.addRect(x, y - height, widths[i], height);
                    content.fill();
                    content.setNonStrokingColor(Color.BLACK);
                }
                content.addRect(x, y - height, widths[i], height);
                content.stroke();
                float lineY = y - padding - size;
                for (String line : wrapped.get(i)) {
                    drawLine(line, header ? bold : regular, size, x + padding, lineY);
                    lineY -= size * LEADING_FACTOR;
                }
                x += widths[i];
            }
            y -= height;
        }

        private void renderImage(PdfImage image) throws IOException {
            if (image.content() == null || image.content().length == 0 || image.width() <= 0 || image.height() <= 0) {
                throw new IllegalArgumentException("PDF image content and dimensions are required");
            }
            ensureSpace(image.height());
            PDImageXObject object = PDImageXObject.createFromByteArray(document, image.content(), "image");
            float x = alignedX(image.alignment(), image.width());
            content.drawImage(object, x, y - image.height(), image.width(), image.height());
            y -= image.height() + Math.max(0, image.spacingAfter());
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < data.marginBottom() + 16) newPage();
        }

        private void addFootersAndPageNumbers() throws IOException {
            int total = document.getNumberOfPages();
            for (int index = 0; index < total; index++) {
                PDPage target = document.getPage(index);
                try (PDPageContentStream footer = new PDPageContentStream(
                        document, target, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    String left = StringUtils.hasText(data.footer()) ? data.footer() : "";
                    drawOn(footer, left, regular, 8, data.marginLeft(), data.marginBottom() / 2);
                    String pageNumber = "Trang " + (index + 1) + "/" + total;
                    float x = target.getMediaBox().getWidth() - data.marginRight() - textWidth(pageNumber, regular, 8);
                    drawOn(footer, pageNumber, regular, 8, x, data.marginBottom() / 2);
                }
            }
        }

        private void drawLine(String text, PDFont font, float size, float x, float lineY) throws IOException {
            drawOn(content, text, font, size, x, lineY);
        }

        private void drawOn(PDPageContentStream stream, String text, PDFont font, float size, float x, float lineY)
                throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, lineY);
            stream.showText(text == null ? "" : text);
            stream.endText();
        }

        private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
            if (!StringUtils.hasText(text)) return List.of("");
            List<String> lines = new ArrayList<>();
            String current = "";
            for (String word : text.trim().split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (!current.isEmpty() && textWidth(candidate, font, size) > maxWidth) {
                    lines.add(current);
                    current = word;
                } else {
                    current = candidate;
                }
            }
            if (!current.isEmpty()) lines.add(current);
            return lines.isEmpty() ? List.of("") : lines;
        }

        private float[] columnWidths(List<Float> configured, int count) {
            float[] widths = new float[count];
            if (configured.size() == count) {
                float total = 0;
                for (Float value : configured) total += value == null ? 0 : value;
                if (total > 0) {
                    for (int i = 0; i < count; i++) widths[i] = usableWidth() * configured.get(i) / total;
                    return widths;
                }
            }
            for (int i = 0; i < count; i++) widths[i] = usableWidth() / count;
            return widths;
        }

        private float alignedX(PdfAlignment alignment, float itemWidth) {
            PdfAlignment safe = alignment == null ? PdfAlignment.LEFT : alignment;
            return switch (safe) {
                case LEFT -> data.marginLeft();
                case CENTER -> data.marginLeft() + (usableWidth() - itemWidth) / 2;
                case RIGHT -> page.getMediaBox().getWidth() - data.marginRight() - itemWidth;
            };
        }

        private float usableWidth() { return page.getMediaBox().getWidth() - data.marginLeft() - data.marginRight(); }
        private float textWidth(String text, PDFont font, float size) throws IOException {
            return font.getStringWidth(text == null ? "" : text) / 1000f * size;
        }
        private String valueAt(List<String> values, int index) {
            return index < values.size() && values.get(index) != null ? values.get(index) : "";
        }
        private void closeContent() throws IOException {
            if (content != null) { content.close(); content = null; }
        }
    }
}
