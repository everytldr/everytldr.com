<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

# Component conventions

- A UI component should define a `className` prop and apply it to its outermost `<div>` (or whatever the outermost wrapping element is).
- To expose styling for inner children, define additional props with a `ClassName` suffix (e.g. `labelClassName`, `iconClassName`). Do not forward the outer `className` prop to inner children.
