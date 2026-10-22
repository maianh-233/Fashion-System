package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.storefront.StorefrontStoreDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cửa hàng cho khách (chỉ đọc, công khai): danh sách cửa hàng đang hoạt động và cửa hàng còn hàng của một biến thể.
 * Tồn tại quầy là tồn offline ({@code available_quantity}), đúng số lượng mà bán tại quầy dùng; khách chỉ thấy
 * "còn hàng" hoặc "sắp hết", không thấy số lượng.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorefrontStoreService {
    /** Còn từ số này trở xuống thì báo "sắp hết". */
    static final int LOW_STOCK_THRESHOLD = 2;

    private static final String STORE_COLUMNS = """
            s.id, s.code, s.name, s.address, s.phone, s.latitude, s.longitude, s.opening_hours, s.image_url, s.services""";

    private final NamedParameterJdbcTemplate jdbc;

    public List<StorefrontStoreDtos.Store> stores() {
        return jdbc.query("select " + STORE_COLUMNS + " from stores s where s.active = true order by s.name",
                new MapSqlParameterSource(), (rs, row) -> toStore(rs));
    }

    /** Cửa hàng đang hoạt động còn hàng của biến thể đang bán; biến thể không bán thì báo không tồn tại. */
    public List<StorefrontStoreDtos.Availability> availability(UUID variantId) {
        MapSqlParameterSource params = new MapSqlParameterSource("variantId", variantId)
                .addValue("low", LOW_STOCK_THRESHOLD);
        Boolean sellable = jdbc.query("""
                select v.active = true and p.status = 'ACTIVE' from product_variants v
                join products p on p.id = v.product_id where v.id = :variantId""", params,
                rs -> rs.next() ? rs.getBoolean(1) : null);
        if (!Boolean.TRUE.equals(sellable)) throw BusinessException.notFound("Sản phẩm không tồn tại hoặc đã ngừng bán");
        return jdbc.query("select " + STORE_COLUMNS + """
                , case when b.available_quantity <= :low then 'LOW_STOCK' else 'IN_STOCK' end as stock
                from inventory_balances b join stores s on s.id = b.store_id
                where b.product_variant_id = :variantId and b.available_quantity > 0 and s.active = true
                order by s.name""", params,
                (rs, row) -> new StorefrontStoreDtos.Availability(toStore(rs), rs.getString("stock")));
    }

    private static StorefrontStoreDtos.Store toStore(ResultSet rs) throws SQLException {
        Array services = rs.getArray("services");
        return new StorefrontStoreDtos.Store(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                rs.getString("address"), rs.getString("phone"), rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"), rs.getString("opening_hours"), rs.getString("image_url"),
                services == null ? List.of() : Arrays.asList((String[]) services.getArray()));
    }
}
