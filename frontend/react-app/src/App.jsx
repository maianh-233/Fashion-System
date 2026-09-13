import { BrowserRouter, Route, Routes } from "react-router-dom";

import AdminLayout from "./components/admin/AdminLayout";
import AdminOnlyRoute from "./components/admin/AdminOnlyRoute";
import AdminPermissionRoute from "./components/admin/AdminPermissionRoute";
import AdminProtectedRoute from "./components/admin/AdminProtectedRoute";
import RoleManagement from "./components/admin/RoleManagement";
import { AdminAuthProvider } from "./contexts/AdminAuthContext";

import CustomerLayout from "./components/customer/CustomerLayout";
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminLogin from "./pages/admin/AdminLogin";
import AdminForgotPassword from "./pages/admin/AdminForgotPassword";
import AuthorizationSettings from "./pages/admin/AuthorizationSettings";
import CustomerManagement from "./pages/admin/CustomerManagement";
import EmployeeManagement from "./pages/admin/EmployeeManagement";
import DepartmentManagement from "./pages/admin/DepartmentManagement";
import PositionManagement from "./pages/admin/PositionManagement";
import StoreManagement from "./pages/admin/StoreManagement";
import TaskManagement from "./pages/admin/TaskManagement";
import ExportReceiptManagement from "./pages/admin/ExportReceiptManagement";
import ImportReceiptManagement from "./pages/admin/ImportReceiptManagement";
import InventoryManagement from "./pages/admin/InventoryManagement";
import LoManagement from "./pages/admin/LoManagement";
import OrderManagement from "./pages/admin/OrderManagement";
import BrandManagement from "./pages/admin/ProductItem/BrandManagement";
import CategoryManagement from "./pages/admin/ProductItem/CategoryManagement";
import CollectionManagement from "./pages/admin/ProductItem/CollectionManagement";
import ProductManagement from "./pages/admin/ProductItem/ProductManagement";
import TagManagement from "./pages/admin/ProductItem/TagManagement";
import VariantManagement from "./pages/admin/ProductItem/VariantManagement";
import ProfileAdmin from "./pages/admin/ProfileAdmin";
import PromotionManagement from "./pages/admin/PromotionManagement";
import StatisticsManagement from "./pages/admin/StatisticsManagement";
import SupplierManagement from "./pages/admin/SupplierManagement";
import NotFound from "./pages/common/NotFound";
import BrandPage from "./pages/customer/BrandPage";
import CollectionPage from "./pages/customer/CollectionPage";
import CustomerHome from "./pages/customer/CustomerHome";
import CustomerLogin from "./pages/customer/CustomerLogin";
import CustomerRegister from "./pages/customer/CustomerRegister";
import ProductPage from "./pages/customer/ProductPage";
import PromotionPage from "./pages/customer/PromotionPage";

import CustomerInfo from "./pages/customer/CustomerInfor";
import CartPage from "./pages/customer/CartPage";
import MyOrdersPage from "./pages/customer/MyOrdersPage";
import CheckoutPage from "./pages/customer/CheckoutPage";
import ProductDetail from "./pages/customer/ProductDetail";
import OrderDetailPage from "./pages/customer/OrderDetailPage";
import CollectionDetailPage from "./pages/customer/CollectionDetailPage";
import BrandDetail from "./pages/customer/BrandDetail";
import StorePage from "./pages/customer/StorePage";
import CustomerInformationPage from "./pages/customer/CustomerInformationPage";

function App() {
  return (
    <BrowserRouter>
      <AdminAuthProvider>
        <Routes>

        <Route path="/" element={<CustomerLayout />}>
          <Route index element={<CustomerHome />} />
          <Route path="products" element={<ProductPage />} />
          <Route path="promotions" element={<PromotionPage />} />
          <Route path="collections" element={<CollectionPage />} />
          <Route path="brand" element={<BrandPage />} />
          <Route path="profile" element={<CustomerInfo />} />
          <Route path="carts" element={<CartPage/>} />
          <Route path="orders" element={<MyOrdersPage/>} />
          <Route path="checkout" element={<CheckoutPage />} />
          <Route path="productdetail" element={<ProductDetail />} />
          <Route path="orderdetail" element={<OrderDetailPage />} />
          <Route path="collectiondetail" element={<CollectionDetailPage />} />
          <Route path="branddetail" element={<BrandDetail />} />
          <Route path="stores" element={<StorePage />} />
          <Route path="about" element={<CustomerInformationPage />} />
        </Route>

        <Route path="/adminlogin" element={<AdminLogin />} />
        <Route path="/admin/forgot-password" element={<AdminForgotPassword />} />
      
        

        {/* Các trang ở admin */}
        <Route
          path="/admin"
          element={
            <AdminProtectedRoute>
              <AdminLayout />
            </AdminProtectedRoute>
          }
        >
          <Route index element={<AdminPermissionRoute permission="DASHBOARD_VIEW"><AdminDashboard /></AdminPermissionRoute>} />
          <Route path="employees" element={<AdminPermissionRoute permission="USER_VIEW"><EmployeeManagement /></AdminPermissionRoute>} />
          <Route path="departments" element={<AdminPermissionRoute permission="DEPARTMENT_VIEW"><DepartmentManagement /></AdminPermissionRoute>} />
          <Route path="positions" element={<AdminPermissionRoute permission="POSITION_VIEW"><PositionManagement /></AdminPermissionRoute>} />
          <Route path="stores" element={<AdminOnlyRoute><AdminPermissionRoute permission="STORE_VIEW"><StoreManagement /></AdminPermissionRoute></AdminOnlyRoute>} />
          <Route path="tasks" element={<AdminPermissionRoute permission="TASK_VIEW"><TaskManagement /></AdminPermissionRoute>} />
          <Route path="customers" element={<AdminPermissionRoute permission="CUSTOMER_VIEW"><CustomerManagement /></AdminPermissionRoute>} />
          <Route path="orders" element={<AdminPermissionRoute permission="ORDER_VIEW"><OrderManagement /></AdminPermissionRoute>} />
          <Route path="imports" element={<AdminPermissionRoute permission="IMPORT_RECEIPT_VIEW"><ImportReceiptManagement /></AdminPermissionRoute>} />
          <Route path="exports" element={<AdminPermissionRoute permission="EXPORT_RECEIPT_VIEW"><ExportReceiptManagement /></AdminPermissionRoute>} />
          <Route path="statistics" element={<AdminPermissionRoute permission="STATISTICS_VIEW"><StatisticsManagement/></AdminPermissionRoute>} />
          <Route path="suppliers" element={<AdminPermissionRoute permission="SUPPLIER_VIEW"><SupplierManagement/></AdminPermissionRoute>} />
      
          <Route path="roles" element={<AdminPermissionRoute permission="ROLE_VIEW"><RoleManagement/></AdminPermissionRoute>} />
          <Route path="settings" element={<AdminOnlyRoute><AdminPermissionRoute permission="SETTINGS_MANAGE"><AuthorizationSettings /></AdminPermissionRoute></AdminOnlyRoute>} />
          <Route path="promotions" element={<AdminPermissionRoute permission="PROMOTION_VIEW"><PromotionManagement/></AdminPermissionRoute>} />
          <Route path="brands" element={<AdminPermissionRoute permission="BRAND_VIEW"><BrandManagement/></AdminPermissionRoute>} />
          <Route path="collections" element={<AdminPermissionRoute permission="COLLECTION_VIEW"><CollectionManagement/></AdminPermissionRoute>} />
          <Route path="categories" element={<AdminPermissionRoute permission="CATEGORY_VIEW"><CategoryManagement/></AdminPermissionRoute>} />
          <Route path="product-tags" element={<AdminPermissionRoute permission="TAG_VIEW"><TagManagement/></AdminPermissionRoute>} />
          <Route path="products" element={<AdminPermissionRoute permission="PRODUCT_VIEW"><ProductManagement/></AdminPermissionRoute>} />
          <Route path="product-variants" element={<AdminPermissionRoute permission="PRODUCT_VARIANT_VIEW"><VariantManagement/></AdminPermissionRoute>} />
          <Route path="inventory" element={<AdminPermissionRoute permission="INVENTORY_VIEW"><InventoryManagement/></AdminPermissionRoute>} />
          <Route path="logs" element={<AdminPermissionRoute permission="LOG_VIEW"><LoManagement /></AdminPermissionRoute>} />
          <Route path="profile" element={<ProfileAdmin />} />

          
        </Route>


        <Route path="/customerlogin" element={<CustomerLogin />} />
        <Route path="/customerregister" element={<CustomerRegister />} />
        

        {/* 404 */}
        <Route path="*" element={<NotFound />} />

        </Routes>
      </AdminAuthProvider>
    </BrowserRouter>
  );
}

export default App;
