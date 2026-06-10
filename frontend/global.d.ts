import type messages from "./messages/en.json";

declare module "next-intl" {
  interface AppConfig {
    Messages: typeof messages;
  }
}

declare module "*.svg" {
  import type { FC, SVGProps } from "react";
  const Component: FC<SVGProps<SVGSVGElement> & { title?: string }>;
  export default Component;
}
