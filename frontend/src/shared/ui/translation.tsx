import type { AppConfig, MessageKeys, NestedKeyOf } from "next-intl";
import { useTranslations } from "next-intl";
import { createElement, type ComponentProps, type JSX } from "react";

type Messages = AppConfig["Messages"];
type MessageKey = MessageKeys<Messages, NestedKeyOf<Messages>>;

type Props<T extends keyof JSX.IntrinsicElements = "span"> = {
  path: MessageKey;
  values?: Record<string, string | number>;
  as?: T;
} & Omit<ComponentProps<T>, "children">;

export function Translation<T extends keyof JSX.IntrinsicElements = "span">({
  path,
  values,
  as,
  ...props
}: Props<T>) {
  const t = useTranslations();

  return createElement(as || "span", props, t(path, values));
}
