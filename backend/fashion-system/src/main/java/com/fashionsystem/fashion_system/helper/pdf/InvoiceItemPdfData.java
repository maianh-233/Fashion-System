package com.fashionsystem.fashion_system.helper.pdf;

import java.math.BigDecimal;

/** Standalone invoice view row; deliberately unrelated to order/product entities. */
public record InvoiceItemPdfData(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) { }
