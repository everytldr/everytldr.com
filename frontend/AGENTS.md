<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

# 1. UI Component Conventions

## 1.1. Props type
Every UI component MUST declare a TypeScript `type` alias named `{ComponentName}Props` and use it as the component's parameter type. `{ComponentName}Props` MUST include `className?: string`.

## 1.2. Outer className
Apply the `className` field of `{ComponentName}Props` (§ 1.1.) to the component's outermost rendered element (the root wrapping `<div>` or equivalent tag). Do not forward it to any inner child.

## 1.3. Inner element className
To expose styling for a non-outermost child element, declare an additional optional prop on `{ComponentName}Props` (§ 1.1.) named `{elementName}ClassName: string` (e.g. `labelClassName`, `iconClassName`) and apply it to that specific child.

## 1.4. Children prop
When `{ComponentName}Props` (§ 1.1.) needs `children`, type it via `PropsWithChildren` from `react`; do not declare `children: ReactNode` manually. Import the symbol directly (`import { PropsWithChildren } from "react"`); the namespaced form `React.PropsWithChildren` is forbidden.

# 2. UI Component Sourcing

## 2.1. shadcn precedence
Before authoring a primitive UI component (button, input, dropdown-menu, dialog, popover, etc.) from scratch, check the shadcn/ui registry. If a matching component exists, install it via the shadcn CLI rather than writing a bespoke implementation.

## 2.2. Design conformance
An installed shadcn component is a scaffold, not a finished primitive. Rewrite every visual decision — colors, typography, radii, spacing, borders, elevation, light/dark variants — to satisfy `DESIGN.md`. No registry default may remain in the committed component.

## 2.3. Convention conformance
Installed components are subject to § 1.1.–§ 1.3. Refactor the props type, outer `className` placement, and inner-element `className` props to match before first use.

# 3. TypeScript Type Conventions

## 3.1. Nullish unions
Replace `T | null`, `T | undefined`, and `T | null | undefined` with `Nullable<T>`, `Optional<T>`, and `Maybe<T>` from `@/shared/lib`. Wrap the full non-nullish operand: `Nullable<A | B>`, not `A | Nullable<B>`.

# 4. Internationalization

## 4.1. Translation primitive precedence
For localized text, prefer `Translation` (`@/shared/ui`) over `getTranslations` (the async helper from `next-intl`). Use `getTranslations` only when a plain string is required and JSX cannot be returned — e.g., `generateMetadata`, string-typed HTML attributes.

# 5. Duration Literals

## 5.1. Shared constants precedence
Do not write bare numeric duration literals in application code. Use the duration constants exported from `@/shared/lib`, such as `A_SECOND`, `A_MINUTE`, `AN_HOUR`, and `A_DAY`.

## 5.2. Unit conversion
The constants in § 5.1. are millisecond values. When an API expects seconds, convert explicitly at the call site, e.g. `A_DAY / A_SECOND`. Do not pass a millisecond constant to a seconds-based API.
