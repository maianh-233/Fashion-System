// Compatibility metadata for the existing Admin pages. The permission API is
// still the source of truth; this file only maps its module/group codes to the
// routes and labels that already exist in the frontend.
export const legacyAdminModules = [
  {
    code: "OVERVIEW",
    name: "Tổng quan",
    icon: "house",
    groups: [
      { code: "DASHBOARD", name: "Trang chủ", icon: "house", path: "/admin" },
    ],
  },
  {
    code: "CATALOG",
    name: "Sản phẩm",
    icon: "shirt",
    groups: [
      { code: "BRAND", name: "Quản lý brand", icon: "badge-check", path: "/admin/brands" },
      { code: "COLLECTION", name: "Quản lý bộ sưu tập", icon: "layers-3", path: "/admin/collections" },
      { code: "CATEGORY", name: "Quản lý danh mục", icon: "folder-tree", path: "/admin/categories" },
      {
        code: "PRODUCT",
        name: "Quản lý sản phẩm",
        icon: "package-2",
        path: "/admin/products",
      },
      {
        code: "PRODUCT_VARIANT",
        name: "Quản lý biến thể",
        icon: "boxes",
        path: "/admin/product-variants",
      },
      { code: "INVENTORY", name: "Quản lý kho", icon: "boxes", path: "/admin/inventory" },
      { code: "TAG", name: "Quản lý tag", icon: "tag", path: "/admin/product-tags" },
    ],
  },
  {
    code: "SALES",
    name: "Bán hàng",
    icon: "shopping-bag",
    groups: [
      { code: "ORDER", name: "Đơn hàng", icon: "shopping-bag", path: "/admin/orders" },
      { code: "CUSTOMER", name: "Khách hàng", icon: "users", path: "/admin/customers" },
      { code: "PROMOTION", name: "Khuyến mãi", icon: "tags", path: "/admin/promotions" },
    ],
  },
  {
    code: "HUMAN_RESOURCE",
    name: "Nhân sự",
    icon: "users",
    groups: [
      { code: "EMPLOYEE", name: "Nhân viên", icon: "user-cog", path: "/admin/employees" },
      { code: "DEPARTMENT", name: "Phòng ban", icon: "building", path: "/admin/departments" },
      { code: "POSITION", name: "Vị trí", icon: "briefcase-business", path: "/admin/positions" },
      { code: "STORE", name: "Cửa hàng", icon: "building-2", path: "/admin/stores" },
      { code: "TASK", name: "Công việc & hiệu suất", icon: "clipboard-check", path: "/admin/tasks" },
    ],
  },
  {
    code: "WAREHOUSE",
    name: "Kho vận",
    icon: "warehouse",
    groups: [
      { code: "IMPORT_RECEIPT", name: "Phiếu nhập", icon: "package-plus", path: "/admin/imports" },
      { code: "EXPORT_RECEIPT", name: "Phiếu xuất", icon: "package-minus", path: "/admin/exports" },
      { code: "SUPPLIER", name: "Nhà cung cấp", icon: "truck", path: "/admin/suppliers" },
    ],
  },
  {
    code: "REPORTING",
    name: "Báo cáo",
    icon: "chart-column",
    groups: [
      { code: "STATISTICS", name: "Thống kê", icon: "chart-column", path: "/admin/statistics" },
    ],
  },
  {
    code: "SYSTEM",
    name: "Hệ thống",
    icon: "settings",
    groups: [
      { code: "ROLE", name: "Phân quyền", icon: "shield-check", path: "/admin/roles" },
      { code: "SETTINGS", name: "Quản lý setting", icon: "settings", path: "/admin/settings" },
      { code: "LOG", name: "Log hệ thống", icon: "logs", path: "/admin/logs" },
    ],
  },
];

const normalizeCode = (value) => String(value || "").trim().toUpperCase();

const groupMetadata = legacyAdminModules.flatMap((module) =>
  module.groups.map((group) => ({ ...group, moduleCode: module.code })),
);

function findGroupMetadata(group) {
  const groupCode = normalizeCode(group?.code);
  const permissionPrefixes = (group?.permissions || []).map((permission) =>
    normalizeCode(permission?.code).split("_")[0],
  );

  return groupMetadata.find((metadata) => {
    const metadataCode = normalizeCode(metadata.code);
    return metadataCode === groupCode || permissionPrefixes.includes(metadataCode);
  });
}

function adaptGroup(group, fallbackGroup, isDemo = false) {
  const metadata = findGroupMetadata(group) || fallbackGroup;

  return {
    ...group,
    code: normalizeCode(group?.code || metadata?.code),
    name: group?.name || metadata?.name || group?.code,
    icon: group?.icon || metadata?.icon || "folder",
    permissions: Array.isArray(group?.permissions) ? group.permissions : [],
    path: metadata?.path || null,
    routeAliases: metadata?.routeAliases || [],
    isDemo,
  };
}

function adaptModule(module, fallbackModule, isDemo = false) {
  const groups = Array.isArray(module?.groups) ? module.groups : [];
  const adaptedGroups = groups.map((group) => adaptGroup(group));
  const hasVariantPath = adaptedGroups.some((group) => group.path === "/admin/product-variants");
  const productGroup = adaptedGroups.find((group) => group.path === "/admin/products");
  const canViewVariants = productGroup?.permissions?.some(
    (permission) => normalizeCode(permission?.code) === "PRODUCT_VARIANT_VIEW",
  );

  if (!hasVariantPath && canViewVariants) {
    const metadata = groupMetadata.find((group) => group.code === "PRODUCT_VARIANT");
    adaptedGroups.push({
      ...metadata,
      code: "PRODUCT_VARIANT",
      permissions: productGroup.permissions.filter(
        (permission) => normalizeCode(permission?.code).startsWith("PRODUCT_VARIANT_"),
      ),
      isDemo,
    });
  }

  return {
    ...module,
    code: normalizeCode(module?.code || fallbackModule?.code),
    name: module?.name || fallbackModule?.name || module?.code,
    icon: String(module?.icon || fallbackModule?.icon || "boxes").toLowerCase(),
    groups: adaptedGroups,
    isDemo,
  };
}

function createDemoModule(module) {
  return {
    ...module,
    groups: module.groups.map((group) => adaptGroup(group, group, true)),
    isDemo: true,
  };
}

export function buildAdminNavigation(apiModules, includeDemoFallback = false) {
  const safeModules = Array.isArray(apiModules) ? apiModules : [];
  const navigation = safeModules.map((module) => {
    const fallbackModule = legacyAdminModules.find(
      (item) => normalizeCode(item.code) === normalizeCode(module.code),
    );
    const adaptedModule = adaptModule(module, fallbackModule);

    if (!includeDemoFallback || !fallbackModule) return adaptedModule;

    const mappedPaths = new Set(adaptedModule.groups.map((group) => group.path).filter(Boolean));
    const missingDemoGroups = fallbackModule.groups
      .filter((group) => !mappedPaths.has(group.path))
      .map((group) => adaptGroup(group, group, true));

    return { ...adaptedModule, groups: [...adaptedModule.groups, ...missingDemoGroups] };
  });

  if (!includeDemoFallback) return navigation;

  const apiModuleCodes = new Set(navigation.map((module) => module.code));
  const missingDemoModules = legacyAdminModules
    .filter((module) => !apiModuleCodes.has(normalizeCode(module.code)))
    .map(createDemoModule);

  return [...navigation, ...missingDemoModules];
}

export function pathMatches(pathname, path) {
  if (!path) return false;
  if (path === "/admin") return pathname === path || pathname === `${path}/`;
  return pathname === path || pathname.startsWith(`${path}/`);
}

export function findActiveNavigation(navigation, pathname) {
  for (const module of navigation) {
    for (const group of module.groups) {
      const paths = [group.path, ...(group.routeAliases || [])];
      if (paths.some((path) => pathMatches(pathname, path))) {
        return { activeModule: module, activeGroup: group };
      }
    }
  }

  return { activeModule: null, activeGroup: null };
}

export function getModuleLandingPath(module) {
  return module?.groups?.find((group) => group.path)?.path || null;
}
