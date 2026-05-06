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
