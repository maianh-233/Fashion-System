package com.fashionsystem.fashion_system.helper.pdf;

/** Marker for provider-neutral PDF content blocks. */
public sealed interface PdfElement permits PdfText, PdfTable, PdfImage { }
