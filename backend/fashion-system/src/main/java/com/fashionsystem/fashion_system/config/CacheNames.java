package com.fashionsystem.fashion_system.config;

/** Central registry for every Redis-backed Spring cache used by the application. */
public final class CacheNames {

    public static final String ACTIVE_MODULES = "module.active.list";
    public static final String AUTHORIZATION_MODULE_LIST = "authorization.module.list";
    public static final String AUTHORIZATION_MODULE_DETAIL = "authorization.module.detail";
    public static final String AUTHORIZATION_GROUP_LIST = "authorization.group.list";
    public static final String AUTHORIZATION_GROUP_DETAIL = "authorization.group.detail";
    public static final String AUTHORIZATION_PERMISSION_LIST = "authorization.permission.list";
    public static final String AUTHORIZATION_PERMISSION_DETAIL = "authorization.permission.detail";
    public static final String AUTHORIZATION_CATALOG = "authorization.catalog.tree";
    public static final String AUTHORIZATION_ROLE_LIST = "authorization.role.list";
    public static final String AUTHORIZATION_ROLE_DETAIL = "authorization.role.detail";
    public static final String AUTHORIZATION_EFFECTIVE_PERMISSIONS = "authorization.effective.permissions";
    public static final String AUTHORIZATION_USER_ROLE_LIST = "authorization.user-role.list";
    public static final String AUTHORIZATION_USER_PERMISSION_LIST = "authorization.user-permission.list";
    public static final String BRAND_DETAIL = "catalog.brand.detail";
    public static final String CATEGORY_DETAIL = "catalog.category.detail";
    public static final String COLLECTION_DETAIL = "catalog.collection.detail";
    public static final String PRODUCT_DETAIL = "catalog.product.detail";
    public static final String PRODUCT_VARIANT_DETAIL = "catalog.product-variant.detail";
    public static final String PRODUCT_ATTRIBUTE_DETAIL = "catalog.product-attribute.detail";
    public static final String PRODUCT_TAG_DETAIL = "catalog.product-tag.detail";
    public static final String PROMOTION_DETAIL = "catalog.promotion.detail";
    public static final String STORE_DETAIL = "reference.store.detail";
    public static final String DEPARTMENT_DETAIL = "reference.department.detail";
    public static final String POSITION_DETAIL = "reference.position.detail";
    public static final String SUPPLIER_DETAIL = "reference.supplier.detail";
    public static final String CUSTOMER_TIER_DETAIL = "reference.customer-tier.detail";
    public static final String CUSTOMER_TIER_ORDERED_LIST = "reference.customer-tier.ordered-list";

    private CacheNames() {
    }
}
