import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import AdminLayout from "./components/admin/AdminLayout";
import RoleManagement from "./components/admin/RoleManagement";
import StoreManagement from "./components/admin/StoreManagement";
import CustomerLayout from "./components/customer/CustomerLayout";
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminLogin from "./pages/admin/AdminLogin";
import CustomerManagement from "./pages/admin/CustomerManagement";
import EmployeeManagement from "./pages/admin/EmployeeManagement";
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
import StorePage from "./pages/customer/StorePage";
import CustomerInfo from "./pages/customer/CustomerInfor";
import CartPage from "./pages/customer/CartPage";
import MyOrdersPage from "./pages/customer/MyOrdersPage";
import CheckoutPage from "./pages/customer/CheckoutPage";
import ProductDetail from "./pages/customer/ProductDetail";
import OrderDetailPage from "./pages/customer/OrderDetailPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>

        <Route path="/" element={<CustomerLayout />}>
          <Route index element={<CustomerHome />} />
          <Route path="products" element={<ProductPage />} />
          <Route path="promotions" element={<PromotionPage />} />
          <Route path="collections" element={<CollectionPage />} />
          <Route path="store" element={<StorePage />} />
          <Route path="brand" element={<BrandPage />} />
          <Route path="profile" element={<CustomerInfo />} />
          <Route path="carts" element={<CartPage/>} />
          <Route path="orders" element={<MyOrdersPage/>} />
          <Route path="checkout" element={<CheckoutPage />} />
          <Route path="productdetail" element={<ProductDetail />} />
          <Route path="orderdetail" element={<OrderDetailPage />} />
        </Route>

        <Route path="/adminlogin" element={<AdminLogin />} />
      
        

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
