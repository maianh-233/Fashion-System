import Button from "./Button";
import { Moon, Sun } from "lucide-react";
import { useTheme } from "../../contexts/ThemeContext";

export default function ThemeToggle({ className = "" }) {
  const { isDark, toggleTheme } = useTheme();
  const nextTheme = isDark ? "sáng" : "tối";

  return (
    <Button
      type="button"
      onClick={toggleTheme}
      className={`theme-toggle ${className}`}
      aria-label={`Chuyển sang giao diện ${nextTheme}`}
      title={`Chuyển sang giao diện ${nextTheme}`}
    >
      <span className="theme-toggle__track" aria-hidden="true">
        <Sun className="theme-toggle__sun" size={15} />
        <Moon className="theme-toggle__moon" size={15} />
        <span className="theme-toggle__thumb" />
      </span>
      <span className="hidden xl:inline">{isDark ? "Tối" : "Sáng"}</span>
    </Button>
  );
}
