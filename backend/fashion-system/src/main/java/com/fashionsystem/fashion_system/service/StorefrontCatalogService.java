package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.storefront.StorefrontDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục sản phẩm cho cửa hàng online (chỉ đọc, công khai).
 *
 * <p>Sản phẩm hiện khi đang kinh doanh (ACTIVE) và có ít nhất một biến thể đang hoạt động — cùng điều kiện
 * "còn bán được" với lúc đặt hàng. Tồn kho tính như lúc phân đơn online: tồn online của cửa hàng đang hoạt động
 * và có tọa độ. Tìm kiếm không phân biệt dấu tiếng Việt.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorefrontCatalogService {
    public static final int MAX_PAGE_SIZE = 48;
    private static final String WITH_ACCENTS = "áàảãạăắằẳẵặâấầẩẫậđéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵ";
    private static final String WITHOUT_ACCENTS = "aaaaaaaaaaaaaaaaadeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyy";
    private static final List<String> SIZE_ORDER = List.of("XS", "S", "M", "L", "XL", "XXL", "XXXL", "FREESIZE");
    private static final Map<String, String> GENDER_LABELS = new LinkedHashMap<>();

    static {
        GENDER_LABELS.put("FEMALE", "Thời trang nữ");
        GENDER_LABELS.put("MALE", "Thời trang nam");
        GENDER_LABELS.put("UNISEX", "Thiết kế unisex");
    }

    /** Tồn online theo biến thể, chỉ tính cửa hàng nhận đơn online (giống bước phân đơn khi khách đặt). */
    private static final String STOCK_CTE = """
            stock as (
                select b.product_variant_id, sum(b.online_quantity) as qty
                from inventory_balances b join stores s on s.id = b.store_id
                where b.online_quantity > 0 and s.active = true and s.latitude is not null and s.longitude is not null
                group by b.product_variant_id
            )""";

    private static final String PRODUCT_AGG = """
            with %s,
            agg as (
                select v.product_id,
                       min(coalesce(v.sale_price, v.price)) as min_price,
                       max(coalesce(v.sale_price, v.price)) as max_price,
                       min(v.price) as min_original,
                       bool_or(v.sale_price is not null and v.sale_price < v.price) as on_sale,
                       coalesce(sum(st.qty), 0) as stock
                from product_variants v left join stock st on st.product_variant_id = v.id
                where v.active = true
                group by v.product_id
            )
            """.formatted(STOCK_CTE);

    private final NamedParameterJdbcTemplate jdbc;

    /** Kiểu tìm kiếm của ô tìm kiếm phía khách. */
    public enum SearchType {
        PRODUCT, BRAND, COLLECTION, TAG
    }

    public enum Sort {
        NEWEST("p.created_at desc nulls last, p.code"),
        PRICE_ASC("a.min_price, p.code"),
        PRICE_DESC("a.min_price desc, p.code"),
        NAME("p.name, p.code");

        final String orderBy;

        Sort(String orderBy) {
            this.orderBy = orderBy;
        }
    }

    /**
     * @param brandId     chỉ sản phẩm của thương hiệu (trang chi tiết thương hiệu)
     * @param collectionId chỉ sản phẩm của bộ sưu tập (trang chi tiết bộ sưu tập)
     * @param promotionId chỉ sản phẩm được khuyến mãi theo sản phẩm áp dụng
     */
    public record Query(String keyword, SearchType searchType, Set<String> genders, Set<UUID> categoryIds,
                        BigDecimal minPrice, BigDecimal maxPrice, boolean inStockOnly, Sort sort, int page, int size,
                        UUID brandId, UUID collectionId, UUID promotionId) {
    }

    public Page<StorefrontDtos.ProductCard> search(Query query) {
        int size = Math.clamp(query.size(), 1, MAX_PAGE_SIZE);
        int page = Math.max(query.page(), 0);
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" where p.status = 'ACTIVE'");

        String keyword = fold(query.keyword());
        if (!keyword.isEmpty()) {
            params.addValue("pattern", "%" + keyword + "%");
            where.append(switch (query.searchType() == null ? SearchType.PRODUCT : query.searchType()) {
                case PRODUCT -> " and (" + folded("p.name") + " like :pattern or lower(p.code) like :pattern)";
                case BRAND -> " and " + folded("coalesce(br.name, '')") + " like :pattern";
                case COLLECTION -> " and c.status = 'ACTIVE' and " + folded("coalesce(c.name, '')") + " like :pattern";
                case TAG -> " and exists (select 1 from product_tag_mappings m join product_tags t on t.id = m.tag_id"
                        + " where m.product_id = p.id and t.active = true and " + folded("t.name") + " like :pattern)";
            });
        }
        if (query.genders() != null && !query.genders().isEmpty()) {
            params.addValue("genders", query.genders());
            where.append(" and p.gender in (:genders)");
        }
        if (query.categoryIds() != null && !query.categoryIds().isEmpty()) {
            params.addValue("categories", withDescendants(query.categoryIds()));
            where.append(" and p.category_id in (:categories)");
        }
        if (query.minPrice() != null) {
            params.addValue("minPrice", query.minPrice());
            where.append(" and a.min_price >= :minPrice");
        }
        if (query.maxPrice() != null) {
            params.addValue("maxPrice", query.maxPrice());
            where.append(" and a.min_price <= :maxPrice");
        }
        if (query.inStockOnly()) where.append(" and a.stock > 0");
        if (query.brandId() != null) {
            params.addValue("brandId", query.brandId());
            where.append(" and p.brand_id = :brandId");
        }
        if (query.collectionId() != null) {
            params.addValue("collectionId", query.collectionId());
            where.append(" and p.collection_id = :collectionId");
        }
        if (query.promotionId() != null) {
            params.addValue("promotionId", query.promotionId());
            where.append(" and exists (select 1 from promotion_products pp where pp.promotion_id = :promotionId"
                    + " and pp.product_id = p.id)");
        }

        String from = """
                 from products p
                 join agg a on a.product_id = p.id
                 left join brands br on br.id = p.brand_id
                 left join collections c on c.id = p.collection_id
                """;
        Long total = jdbc.queryForObject(PRODUCT_AGG + "select count(*)" + from + where, params, Long.class);
        if (total == null || total == 0) return Page.empty(PageRequest.of(page, size));

        params.addValue("limit", size).addValue("offset", (long) page * size);
        String sql = PRODUCT_AGG + """
                select p.id, p.slug, p.code, p.name, p.gender, br.name as brand_name,
                       coalesce(nullif(p.image_url, ''), (
                           select i.image_url from product_images i join product_variants iv on iv.id = i.product_variant_id
                           where iv.product_id = p.id order by i.is_primary desc nulls last, i.sort_order nulls last limit 1
                       )) as image_url,
                       a.min_price, a.max_price, a.min_original, a.on_sale, a.stock
                """ + from + where + " order by " + (query.sort() == null ? Sort.NEWEST : query.sort()).orderBy
                + " limit :limit offset :offset";
        List<UUID> ids = new ArrayList<>();
        List<Object[]> rows = jdbc.query(sql, params, (rs, i) -> {
            UUID id = rs.getObject("id", UUID.class);
            ids.add(id);
            return new Object[]{id, rs.getString("slug"), rs.getString("code"), rs.getString("name"),
                    rs.getString("image_url"), rs.getString("gender"), rs.getString("brand_name"),
                    rs.getBigDecimal("min_price"), rs.getBigDecimal("max_price"), rs.getBigDecimal("min_original"),
                    rs.getBoolean("on_sale"), rs.getLong("stock") > 0};
        });
        Map<UUID, List<String>> tags = tagsOf(ids);
        List<StorefrontDtos.ProductCard> cards = rows.stream().map(r -> new StorefrontDtos.ProductCard((UUID) r[0],
                (String) r[1], (String) r[2], (String) r[3], (String) r[4], (String) r[5], (String) r[6],
                (BigDecimal) r[7], (BigDecimal) r[8], (BigDecimal) r[9], (Boolean) r[10], (Boolean) r[11],
                tags.getOrDefault((UUID) r[0], List.of()))).toList();
        return new PageImpl<>(cards, PageRequest.of(page, size), total);
    }

    public StorefrontDtos.Filters filters() {
        Map<UUID, Long> counts = new HashMap<>();
        jdbc.query(PRODUCT_AGG + "select p.category_id, count(*) as n from products p join agg a on a.product_id = p.id"
                        + " where p.status = 'ACTIVE' and p.category_id is not null group by p.category_id",
                new MapSqlParameterSource(), rs -> {
                    counts.put(rs.getObject("category_id", UUID.class), rs.getLong("n"));
                });
        record Row(UUID id, UUID parentId, String name) {
        }
        List<Row> rows = jdbc.query("select id, parent_id, name from categories where active = true order by name",
                new MapSqlParameterSource(), (rs, i) -> new Row(rs.getObject("id", UUID.class),
                        rs.getObject("parent_id", UUID.class), rs.getString("name")));
        List<StorefrontDtos.Category> roots = new ArrayList<>();
        for (Row root : rows) {
            if (root.parentId() != null) continue;
            List<StorefrontDtos.Category> children = rows.stream().filter(r -> root.id().equals(r.parentId()))
                    .map(r -> new StorefrontDtos.Category(r.id(), r.name(), counts.getOrDefault(r.id(), 0L), List.of()))
                    .filter(c -> c.productCount() > 0).toList();
            long total = counts.getOrDefault(root.id(), 0L) + children.stream().mapToLong(StorefrontDtos.Category::productCount).sum();
            if (total > 0) roots.add(new StorefrontDtos.Category(root.id(), root.name(), total, children));
        }

        Map<String, Long> genderCounts = new HashMap<>();
        jdbc.query(PRODUCT_AGG + "select p.gender, count(*) as n from products p join agg a on a.product_id = p.id"
                + " where p.status = 'ACTIVE' group by p.gender", new MapSqlParameterSource(),
                rs -> {
                    genderCounts.put(rs.getString("gender"), rs.getLong("n"));
                });
        List<StorefrontDtos.Option> genders = GENDER_LABELS.entrySet().stream()
                .filter(e -> genderCounts.getOrDefault(e.getKey(), 0L) > 0)
                .map(e -> new StorefrontDtos.Option(e.getKey(), e.getValue(), genderCounts.get(e.getKey()))).toList();

        BigDecimal[] bounds = jdbc.queryForObject(PRODUCT_AGG + "select min(a.min_price) as lo, max(a.max_price) as hi"
                        + " from products p join agg a on a.product_id = p.id where p.status = 'ACTIVE'",
                new MapSqlParameterSource(), (rs, i) -> new BigDecimal[]{rs.getBigDecimal("lo"), rs.getBigDecimal("hi")});
        return new StorefrontDtos.Filters(roots, genders, bounds[0], bounds[1]);
    }

    /** Chi tiết theo id hoặc slug; sản phẩm ngừng kinh doanh hoặc không còn biến thể nào hoạt động thì như không tồn tại. */
    public StorefrontDtos.ProductDetail detail(String idOrSlug) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String condition;
        try {
            params.addValue("id", UUID.fromString(idOrSlug));
            condition = "p.id = :id";
        } catch (IllegalArgumentException notUuid) {
            params.addValue("slug", idOrSlug.trim().toLowerCase(Locale.ROOT));
            condition = "lower(p.slug) = :slug";
        }
        List<StorefrontDtos.ProductDetail> found = jdbc.query("""
                select p.id, p.slug, p.code, p.name, p.description, p.material, p.fit, p.gender, p.image_url,
                       br.id as brand_id, br.name as brand_name,
                       c.id as collection_id, c.name as collection_name, c.season, c.year, c.status as collection_status,
                       cat.id as category_id, cat.name as category_name, parent.id as parent_id, parent.name as parent_name
                from products p
                left join brands br on br.id = p.brand_id
                left join collections c on c.id = p.collection_id
                left join categories cat on cat.id = p.category_id
                left join categories parent on parent.id = cat.parent_id
                where p.status = 'ACTIVE'
                """ + " and " + condition, params, (rs, i) -> toDetail(rs));
        if (found.isEmpty()) throw BusinessException.notFound("Sản phẩm không tồn tại hoặc đã ngừng kinh doanh");
        StorefrontDtos.ProductDetail base = found.getFirst();

        MapSqlParameterSource byProduct = new MapSqlParameterSource("productId", base.id());
        List<StorefrontDtos.Variant> variants = new ArrayList<>(jdbc.query("with " + STOCK_CTE + """
                 select v.id, v.sku, v.color, v.size, v.price, v.sale_price, coalesce(st.qty, 0) as available
                 from product_variants v left join stock st on st.product_variant_id = v.id
                 where v.product_id = :productId and v.active = true
                """, byProduct, (rs, i) -> {
            BigDecimal price = rs.getBigDecimal("price");
            BigDecimal sale = rs.getBigDecimal("sale_price");
            return new StorefrontDtos.Variant(rs.getObject("id", UUID.class), rs.getString("sku"), rs.getString("color"),
                    rs.getString("size"), price, sale, sale != null ? sale : price, rs.getInt("available"));
        }));
        if (variants.isEmpty()) throw BusinessException.notFound("Sản phẩm không tồn tại hoặc đã ngừng kinh doanh");
        variants.sort((a, b) -> {
            int byColor = String.valueOf(a.color()).compareTo(String.valueOf(b.color()));
            return byColor != 0 ? byColor : Integer.compare(sizeRank(a.size()), sizeRank(b.size()));
        });

        List<String> colors = variants.stream().map(StorefrontDtos.Variant::color).filter(c -> c != null && !c.isBlank())
                .distinct().toList();
        List<String> sizes = variants.stream().map(StorefrontDtos.Variant::size).filter(s -> s != null && !s.isBlank())
                .distinct().sorted((a, b) -> Integer.compare(sizeRank(a), sizeRank(b))).toList();

        List<StorefrontDtos.Image> images = new ArrayList<>();
        String cover = coverOf(base.id());
        if (cover != null) images.add(new StorefrontDtos.Image(cover, null));
        jdbc.query("""
                select i.image_url, v.color from product_images i join product_variants v on v.id = i.product_variant_id
                where v.product_id = :productId and v.active = true
                order by v.color, i.is_primary desc nulls last, i.sort_order nulls last, i.created_at
                """, byProduct, rs -> {
            String url = rs.getString("image_url");
            if (url != null && !url.isBlank() && images.stream().noneMatch(img -> img.url().equals(url)))
                images.add(new StorefrontDtos.Image(url, rs.getString("color")));
        });

        List<StorefrontDtos.Attribute> attributes = jdbc.query("""
                select attribute_name, attribute_value from product_attributes
                where product_id = :productId order by created_at, attribute_name
                """, byProduct, (rs, i) -> new StorefrontDtos.Attribute(rs.getString(1), rs.getString(2)));

        BigDecimal min = variants.stream().map(StorefrontDtos.Variant::unitPrice).min(BigDecimal::compareTo).orElse(null);
        BigDecimal max = variants.stream().map(StorefrontDtos.Variant::unitPrice).max(BigDecimal::compareTo).orElse(null);
        return new StorefrontDtos.ProductDetail(base.id(), base.slug(), base.code(), base.name(), base.description(),
                base.material(), base.fit(), base.gender(), base.brand(), base.collection(), base.category(), min, max,
                variants.stream().anyMatch(v -> v.available() > 0), colors, sizes, variants, images, attributes,
                tagsOf(List.of(base.id())).getOrDefault(base.id(), List.of()));
    }

    /**
     * Thông tin hiện tại của các biến thể cho giỏ hàng: giá bán, tồn online (như lúc phân đơn) và còn bán được
     * hay không (sản phẩm ACTIVE và biến thể đang hoạt động).
     */
    public Map<UUID, com.fashionsystem.fashion_system.dto.storefront.CartDtos.VariantInfo> variants(Collection<UUID> variantIds) {
        Map<UUID, com.fashionsystem.fashion_system.dto.storefront.CartDtos.VariantInfo> result = new LinkedHashMap<>();
        if (variantIds.isEmpty()) return result;
        jdbc.query("with " + STOCK_CTE + """
                 select v.id, p.id as product_id, p.slug, p.name, br.name as brand_name, v.sku, v.color, v.size,
                        v.price, v.sale_price, coalesce(st.qty, 0) as available,
                        (v.active = true and p.status = 'ACTIVE') as sellable,
                        coalesce((select i.image_url from product_images i where i.product_variant_id = v.id
                                  order by i.is_primary desc nulls last, i.sort_order nulls last limit 1),
                                 nullif(p.image_url, '')) as image_url
                 from product_variants v
                 join products p on p.id = v.product_id
                 left join brands br on br.id = p.brand_id
                 left join stock st on st.product_variant_id = v.id
                 where v.id in (:ids)
                """, new MapSqlParameterSource("ids", variantIds), rs -> {
            UUID id = rs.getObject("id", UUID.class);
            result.put(id, new com.fashionsystem.fashion_system.dto.storefront.CartDtos.VariantInfo(id,
                    rs.getObject("product_id", UUID.class), rs.getString("slug"), rs.getString("name"),
                    rs.getString("brand_name"), rs.getString("sku"), rs.getString("color"), rs.getString("size"),
                    rs.getString("image_url"), rs.getBigDecimal("price"), rs.getBigDecimal("sale_price"),
                    rs.getInt("available"), rs.getBoolean("sellable")));
        });
        return result;
    }

    private String coverOf(UUID productId) {
        List<String> urls = jdbc.query("select image_url from products where id = :id and coalesce(image_url, '') <> ''",
                new MapSqlParameterSource("id", productId), (rs, i) -> rs.getString(1));
        return urls.isEmpty() ? null : urls.getFirst();
    }

    private static StorefrontDtos.ProductDetail toDetail(ResultSet rs) throws SQLException {
        UUID brandId = rs.getObject("brand_id", UUID.class);
        UUID collectionId = rs.getObject("collection_id", UUID.class);
        UUID categoryId = rs.getObject("category_id", UUID.class);
        UUID parentId = rs.getObject("parent_id", UUID.class);
        Integer year = (Integer) rs.getObject("year");
        // Bộ sưu tập chưa phát hành (DRAFT) không hiện cho khách.
        var collection = collectionId == null || !"ACTIVE".equals(rs.getString("collection_status")) ? null
                : new StorefrontDtos.CollectionRef(collectionId, rs.getString("collection_name"), rs.getString("season"), year);
        return new StorefrontDtos.ProductDetail(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("code"),
                rs.getString("name"), rs.getString("description"), rs.getString("material"), rs.getString("fit"),
                rs.getString("gender"), brandId == null ? null : new StorefrontDtos.Ref(brandId, rs.getString("brand_name")),
                collection, categoryId == null ? null : new StorefrontDtos.CategoryRef(categoryId, rs.getString("category_name"),
                parentId == null ? null : new StorefrontDtos.Ref(parentId, rs.getString("parent_name"))),
                null, null, false, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private Map<UUID, List<String>> tagsOf(Collection<UUID> productIds) {
        Map<UUID, List<String>> tags = new HashMap<>();
        if (productIds.isEmpty()) return tags;
        jdbc.query("""
                select m.product_id, t.name from product_tag_mappings m join product_tags t on t.id = m.tag_id
                where m.product_id in (:ids) and t.active = true order by t.name
                """, new MapSqlParameterSource("ids", productIds),
                rs -> {
                    tags.computeIfAbsent(rs.getObject("product_id", UUID.class), ignored -> new ArrayList<>())
                            .add(rs.getString("name"));
                });
        return tags;
    }

    /** Lọc theo danh mục cha thì gồm cả các danh mục con. */
    private Set<UUID> withDescendants(Set<UUID> categoryIds) {
        Set<UUID> all = new LinkedHashSet<>(categoryIds);
        all.addAll(jdbc.queryForList("select id from categories where parent_id in (:ids)",
                new MapSqlParameterSource("ids", categoryIds), UUID.class));
        return all;
    }

    private static int sizeRank(String size) {
        int index = size == null ? -1 : SIZE_ORDER.indexOf(size.trim().toUpperCase(Locale.ROOT));
        return index < 0 ? SIZE_ORDER.size() : index;
    }

    /** Biểu thức SQL bỏ dấu tiếng Việt và viết thường một cột. */
    static String folded(String column) {
        return "translate(lower(" + column + "), '" + WITH_ACCENTS + "', '" + WITHOUT_ACCENTS + "')";
    }

    /** Từ khóa viết thường, bỏ dấu, bỏ ký tự đại diện của LIKE. */
    static String fold(String keyword) {
        if (keyword == null) return "";
        String lower = keyword.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');
        String stripped = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return stripped.replace("%", "").replace("_", "").replace("\\", "");
    }
}
