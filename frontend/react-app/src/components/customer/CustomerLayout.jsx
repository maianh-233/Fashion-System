import { Outlet } from "react-router-dom";
import { navLinks } from "../../mock/storefrontData";
import { CustomerCartProvider, useCustomerCart } from "../../contexts/CustomerCartContext";
import { CustomerWishlistProvider, useCustomerWishlist } from "../../contexts/CustomerWishlistContext";
import { CustomerRealtimeProvider } from "../../realtime/CustomerRealtimeContext";
import StorefrontFooter from "./StorefrontFooter";
import StorefrontHeader from "./StorefrontHeader";

function Shell() {
  const { cart } = useCustomerCart();
  const wishlist = useCustomerWishlist();
  return (
    <div className="customer-shell min-h-screen overflow-x-clip bg-zinc-950 text-zinc-200 flex flex-col">
      <StorefrontHeader navLinks={navLinks} cartCount={cart.itemCount} wishlistCount={wishlist?.count ?? 0} />
      <main className="flex-1">
        <Outlet />
      </main>
      <StorefrontFooter />
    </div>
  );
}

export default function CustomerLayout() {
  return (
    <CustomerRealtimeProvider>
      <CustomerCartProvider>
        <CustomerWishlistProvider>
          <Shell />
        </CustomerWishlistProvider>
      </CustomerCartProvider>
    </CustomerRealtimeProvider>
  );
}
