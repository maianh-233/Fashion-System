package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.ReturnSizeReportDtos.FitHint;
import com.fashionsystem.fashion_system.service.ReturnSizeReportService.ProductInfo;
import com.fashionsystem.fashion_system.service.ReturnSizeReportService.ReturnedRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnSizeReportServiceTest {
    final UUID shirt = UUID.randomUUID(), jeans = UUID.randomUUID(), cap = UUID.randomUUID();
    final LocalDate from = LocalDate.of(2026, 8, 1), to = LocalDate.of(2026, 10, 31);

    @Test void ratesAreReturnedOverSoldPerSizeAndWrongSizeExchangesShowDirection() {
        var report = ReturnSizeReportService.build(from, to,
                Map.of(shirt, Map.of("S", 10L, "M", 20L, "L", 10L), jeans, Map.of("30", 8L, "31", 8L)),
                List.of(new ReturnedRow(shirt, "M", "WRONG_SIZE", "L", 3),
                        new ReturnedRow(shirt, "M", "WRONG_SIZE", null, 1),
                        new ReturnedRow(shirt, "L", "DEFECTIVE", null, 1),
                        new ReturnedRow(jeans, "31", "WRONG_SIZE", "30", 1)),
                Map.of(shirt, new ProductInfo("Áo sơ mi", null, "Áo"), jeans, new ProductInfo("Quần jean", null, "Quần")));

        assertThat(report.summary().soldQuantity()).isEqualTo(56);
        assertThat(report.summary().returnedQuantity()).isEqualTo(6);
        assertThat(report.summary().wrongSizeQuantity()).isEqualTo(5);
        assertThat(report.summary().wrongSizeRate()).isEqualTo(8.9);
        assertThat(report.reasons().getFirst().code()).isEqualTo("WRONG_SIZE");
        assertThat(report.reasons().getFirst().label()).isEqualTo("Không vừa size");

        var shirtRow = report.products().getFirst();
        assertThat(shirtRow.productName()).isEqualTo("Áo sơ mi");
        assertThat(shirtRow.flagged()).isTrue();
        assertThat(shirtRow.wrongSizeRate()).isEqualTo(10.0);
        assertThat(shirtRow.fitHint()).isEqualTo(FitHint.RUNS_SMALL);
        assertThat(shirtRow.sizes()).extracting(s -> s.size()).containsExactly("S", "M", "L");
        var medium = shirtRow.sizes().get(1);
        assertThat(medium.returnedQuantity()).isEqualTo(4);
        assertThat(medium.wrongSizeRate()).isEqualTo(20.0);
        assertThat(medium.exchangedUp()).isEqualTo(3);
        assertThat(medium.exchanges()).singleElement().satisfies(f -> {
            assertThat(f.toSize()).isEqualTo("L");
            assertThat(f.quantity()).isEqualTo(3);
        });

        var jeansRow = report.products().get(1);
        assertThat(jeansRow.flagged()).isFalse(); // chỉ 1 món, dưới ngưỡng
        assertThat(jeansRow.fitHint()).isEqualTo(FitHint.RUNS_LARGE);
        assertThat(report.flaggedProducts()).isEqualTo(1);
    }

    @Test void productsWithoutReturnsAreLeftOutButCountTowardsSold() {
        var report = ReturnSizeReportService.build(from, to, Map.of(cap, Map.of("FREESIZE", 40L)), List.of(), Map.of());
        assertThat(report.products()).isEmpty();
        assertThat(report.summary().soldQuantity()).isEqualTo(40);
        assertThat(report.summary().returnRate()).isZero();
    }

    @Test void directionComparesLetterAndNumericSizesOnly() {
        assertThat(ReturnSizeReportService.direction("M", "XL")).isPositive();
        assertThat(ReturnSizeReportService.direction("32", "30")).isNegative();
        assertThat(ReturnSizeReportService.direction("M", "M")).isZero();
        assertThat(ReturnSizeReportService.direction("M", "32")).isNull();
        assertThat(ReturnSizeReportService.direction("M", null)).isNull();
    }
}
