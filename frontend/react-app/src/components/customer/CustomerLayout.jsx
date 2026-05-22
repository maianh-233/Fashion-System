import { Outlet } from "react-router-dom";
import { navLinks } from "../../hooks/storefrontData";
import StorefrontFooter from "./StorefrontFooter";
import StorefrontHeader from "./StorefrontHeader";

export default function CustomerLayout() {
  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200 flex flex-col">
      <StorefrontHeader navLinks={navLinks} cartCount={3} />
      <main className="flex-1">
        <Outlet />
      </main>
      <StorefrontFooter />
    </div>
  );
}
