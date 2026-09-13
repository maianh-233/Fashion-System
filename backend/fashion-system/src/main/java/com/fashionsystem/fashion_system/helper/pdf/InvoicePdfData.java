package com.fashionsystem.fashion_system.helper.pdf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Standalone invoice PDF view model for infrastructure verification and future mapping. */
public record InvoicePdfData(
        String invoiceNumber,
        LocalDate issueDate,
        String sellerName,
        String buyerName,
        String buyerAddress,
        List<InvoiceItemPdfData> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String currency) {
    public InvoicePdfData {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
