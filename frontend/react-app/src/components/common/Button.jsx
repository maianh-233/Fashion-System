import { forwardRef } from "react";
import { useOptionalAdminPermissions } from "../../contexts/AdminPermissionsContext";

function inferVariant(className, style) {
  if (style?.backgroundColor || style?.background) return "unstyled";
  if (typeof className !== "string") return "ghost";

  if (/bg-red-/.test(className)) return "danger-solid";
  if (/(?:text|border)-red-|danger/.test(className)) return "danger";
  if (/\bw-full\b/.test(className) && /\btext-left\b/.test(className)) {
    return "unstyled";
  }
  if (
    /(?:bg|from|to)-(?:amber|orange|yellow|blue|emerald|green)-/.test(className)
  ) {
    return "primary";
  }
  if (/(?:bg)-(?:zinc|gray)-/.test(className)) return "secondary";
  if (/(?:bg)-black(?:\/|\s|$)/.test(className)) return "unstyled";
  if (/\b(?:theme-toggle|pagination__button)\b/.test(className)) return "unstyled";
  if (
    /\bw-full\b/.test(className) &&
    /\b(?:items-center|justify-between)\b/.test(className)
  ) {
    return "unstyled";
  }

  return "ghost";
}

const Button = forwardRef(function Button(
  {
    variant = "auto",
    size,
    loading = false,
    permission,
    anyPermission,
    globalOnly = false,
    className = "",
    children,
    disabled,
    style,
    ...props
  },
  ref,
) {
  const permissions = useOptionalAdminPermissions();
  const requiredPermissions = Array.isArray(anyPermission) ? anyPermission : [];
  const productMasterMutation = typeof permission === "string" &&
    /^(PRODUCT(?:_VARIANT|_IMAGE)?|BRAND|CATEGORY|COLLECTION|TAG)_(?!VIEW$)/.test(permission);
  const isAllowed = !permissions || (
    (!permission || permissions.hasPermission(permission)) &&
    (requiredPermissions.length === 0 || requiredPermissions.some(permissions.hasPermission)) &&
    (!(globalOnly || productMasterMutation) || permissions.isGlobal)
  );
  if (!isAllowed) return null;

  const resolvedVariant =
    variant === "auto" ? inferVariant(className, style) : variant;
  const classes = [
    "ui-button",
    `ui-button--${resolvedVariant}`,
    size ? `ui-button--${size}` : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      ref={ref}
      className={classes}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      style={style}
      {...props}
    >
      {loading && <span className="ui-button__spinner" aria-hidden="true" />}
      {children}
    </button>
  );
});

export default Button;
