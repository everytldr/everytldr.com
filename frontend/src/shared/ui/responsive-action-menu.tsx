"use client";

import { cn, useIsCoarsePointer } from "@/shared/lib";
import { noop } from "lodash-es";
import type { LucideIcon } from "lucide-react";
import { type ReactNode, useState } from "react";
import { BottomSheet } from "./bottom-sheet";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "./dropdown-menu";

export type ActionMenuItem = {
  key: string;
  label: string;
  Icon?: LucideIcon;
  variant?: "default" | "destructive";
  disabled?: boolean;
  onSelect: () => void;
};

type ResponsiveActionMenuProps = {
  className?: string;
  actions: ActionMenuItem[];
  title: string;
  renderTrigger: (state: { isOpen: boolean; open: () => void }) => ReactNode;
};

export function ResponsiveActionMenu(props: ResponsiveActionMenuProps) {
  const isCoarsePointer = useIsCoarsePointer();

  return isCoarsePointer ? <MobileActionMenu {...props} /> : <DesktopActionMenu {...props} />;
}

function DesktopActionMenu({ className, actions, renderTrigger }: ResponsiveActionMenuProps) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className={cn(className)}>
      <DropdownMenu onOpenChange={setIsOpen}>
        <DropdownMenuTrigger asChild>{renderTrigger({ isOpen, open: noop })}</DropdownMenuTrigger>
        <DropdownMenuContent className="min-w-40" align="end">
          {actions.map((action) => (
            <DropdownMenuItem
              key={action.key}
              className={cn(action.variant === "destructive" && "text-semantic-error")}
              disabled={action.disabled}
              onSelect={action.onSelect}
            >
              {action.Icon && <action.Icon className="size-md shrink-0" />}
              {action.label}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}

function MobileActionMenu({ className, actions, title, renderTrigger }: ResponsiveActionMenuProps) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className={cn(className)}>
      {renderTrigger({ isOpen, open: () => setIsOpen(true) })}
      <BottomSheet isOpen={isOpen} header={{ title }} onClose={() => setIsOpen(false)}>
        <div className="flex flex-col gap-2xs" role="menu" aria-label={title}>
          {actions.map((action) => (
            <button
              key={action.key}
              className={cn(
                "flex h-13 w-full cursor-pointer items-center justify-center gap-sm rounded-sm bg-surface-soft text-button-md transition-colors outline-none last:mb-md hover:bg-surface-strong focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset active:bg-surface-pressed disabled:cursor-not-allowed disabled:opacity-50",
                action.variant === "destructive" ? "text-semantic-error" : "text-ink",
              )}
              type="button"
              role="menuitem"
              disabled={action.disabled}
              onClick={() => handleSelect(action)}
            >
              {action.Icon && <action.Icon className="size-5 shrink-0" />}
              {action.label}
            </button>
          ))}
        </div>
      </BottomSheet>
    </div>
  );

  function handleSelect(action: ActionMenuItem) {
    setIsOpen(false);
    action.onSelect();
  }
}
