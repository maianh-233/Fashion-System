import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import CustomerLogin from "./pages/customer/CustomerLogin";
import CustomerRegister from "./pages/customer/CustomerRegister";
import NotFound from "./pages/common/NotFound";
import AdminLogin from "./pages/admin/AdminLogin";
import AdminDashboard from "./pages/admin/AdminDashboard";
import EmployeeManagement from "./pages/admin/EmployeeManagement";
import CustomerManagement from "./pages/admin/CustomerManagement";
import OrderManagement from "./pages/admin/OrderManagement";
import AdminLayout from "./components/admin/AdminLayout";
import ImportReceiptManagement from "./pages/admin/ImportReceiptManagement";
import ExportReceiptManagement from "./pages/admin/ExportReceiptManagement";
import StatisticsManagement from "./pages/admin/StatisticsManagement";
import SupplierManagement from "./pages/admin/SupplierManagement";
import StoreManagement from "./components/admin/StoreManagement";
import RoleManagement from "./components/admin/RoleManagement";
import PromotionManagement from "./pages/admin/PromotionManagement";
import BrandManagement from "./pages/admin/ProductItem/BrandManagement";
import CollectionManagement from "./pages/admin/ProductItem/CollectionManagement";
import CategoryManagement from "./pages/admin/ProductItem/CategoryManagement";
import TagManagement from "./pages/admin/ProductItem/TagManagement";
import ProductManagement from "./pages/admin/ProductItem/ProductManagement";
import VariantManagement from "./pages/admin/ProductItem/VariantManagement";
import InventoryManagement from "./pages/admin/InventoryManagement";
import LoManagement from "./pages/admin/LoManagement";
import ProfileAdmin from "./pages/admin/ProfileAdmin";
import CustomerHome from "./pages/customer/CustomerHome";
function App() {
  return (
    <BrowserRouter>
      <Routes>

        <Route path="/" element={<CustomerHome />} />

        <Route path="/adminlogin" element={<AdminLogin />} />
        <Route path="/admindashboard" element={<Navigate to="/admin" replace />} />
        

        {/* Các trang ở admin */}
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<AdminDashboard />} />
          <Route path="employees" element={<EmployeeManagement />} />
          <Route path="customers" element={<CustomerManagement />} />
          <Route path="orders" element={<OrderManagement />} />
          <Route path="imports" element={<ImportReceiptManagement />} />
          <Route path="exports" element={<ExportReceiptManagement />} />
          <Route path="statistics" element={<StatisticsManagement/>} />
          <Route path="suppliers" element={<SupplierManagement/>} />
          <Route path="stores" element={<StoreManagement/>} />
          <Route path="roles" element={<RoleManagement/>} />
          <Route path="promotions" element={<PromotionManagement/>} />
          <Route path="brands" element={<BrandManagement/>} />
          <Route path="collections" element={<CollectionManagement/>} />
          <Route path="categories" element={<CategoryManagement/>} />
          <Route path="product-tags" element={<TagManagement/>} />
          <Route path="products" element={<ProductManagement/>} />
          <Route path="product-variants" element={<VariantManagement/>} />
          <Route path="inventory" element={<InventoryManagement/>} />
          <Route path="logs" element={<LoManagement />} />
          <Route path="profile" element={<ProfileAdmin />} />
        </Route>


        <Route path="/customerlogin" element={<CustomerLogin />} />
        <Route path="/customerregister" element={<CustomerRegister />} />
        

        {/* 404 */}
        <Route path="*" element={<NotFound />} />

      </Routes>
    </BrowserRouter>
  );
}

export default App;
