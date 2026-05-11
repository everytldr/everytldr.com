"use client";

import { useIsCoarsePointer } from "@/shared/hooks";
import { cn, type Optional } from "@/shared/lib";
import { Check } from "lucide-react";
import { type ReactNode, useState } from "react";
import { BottomSheet } from "./bottom-sheet";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "./dropdown-menu";

export type SelectorOption<T extends string> = {
  value: T;
  content: ReactNode;
};

type ResponsiveSelectorTriggerState<T extends string> = {
  isOpen: boolean;
  selected: Optional<SelectorOption<T>>;
};

type ResponsiveSelectorMobileTriggerState<T extends string> = ResponsiveSelectorTriggerState<T> & {
  openSheet: () => void;
};

type ResponsiveSelectorProps<T extends string> = {
  className?: string;
  value: T;
  options: readonly SelectorOption<T>[];
  title: string;
  renderMobileTrigger: (state: ResponsiveSelectorMobileTriggerState<T>) => ReactNode;
  renderDesktopTrigger: (state: ResponsiveSelectorTriggerState<T>) => ReactNode;
  onChange: (value: T) => void;
};

export function ResponsiveSelector<T extends string>({
  className,
  value,
  options,
  title,
  renderMobileTrigger,
  renderDesktopTrigger,
  onChange,
}: ResponsiveSelectorProps<T>) {
  const isCoarsePointer = useIsCoarsePointer();

  return isCoarsePointer ? (
    <MobileResponsiveSelector
      className={className}
      value={value}
      options={options}
      title={title}
      renderTrigger={renderMobileTrigger}
      onChange={onChange}
    />
  ) : (
    <DesktopResponsiveSelector
      className={className}
      value={value}
      options={options}
      renderTrigger={renderDesktopTrigger}
      onChange={onChange}
    />
  );
}

type MobileResponsiveSelectorProps<T extends string> = {
  className?: string;
  value: T;
  options: readonly SelectorOption<T>[];
  title: string;
  renderTrigger: (state: ResponsiveSelectorMobileTriggerState<T>) => ReactNode;
  onChange: (value: T) => void;
};

function MobileResponsiveSelector<T extends string>({
  className,
  value,
  options,
  title,
  renderTrigger,
  onChange,
}: MobileResponsiveSelectorProps<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const selected = options.find((opt) => opt.value === value);

  return (
    <div className={cn(className)}>
      {renderTrigger({ isOpen, selected, openSheet: () => setIsOpen(true) })}
      <BottomSheet isOpen={isOpen} header={{ title }} onClose={() => setIsOpen(false)}>
        <div className="flex flex-col gap-2xs px-md pb-md" role="radiogroup" aria-label={title}>
          {options.map((opt) => {
            const isCurrent = opt.value === value;
            return (
              <button
                key={opt.value}
                className="flex h-12 cursor-pointer items-center justify-between gap-sm rounded-md px-md text-button-md text-ink transition-colors outline-none hover:bg-surface-soft focus-visible:ring-2 focus-visible:ring-primary active:bg-surface-strong dark:hover:bg-surface-strong dark:active:bg-surface-pressed"
                type="button"
                role="radio"
                aria-checked={isCurrent}
                onClick={() => handleSelect(opt.value)}
              >
                {opt.content}
                {isCurrent && <Check className="size-5 shrink-0 text-ink" />}
              </button>
            );
          })}
        </div>
      </BottomSheet>
    </div>
  );

  function handleSelect(next: T) {
    setIsOpen(false);
    if (next !== value) {
      onChange(next);
    }
  }
}

type DesktopResponsiveSelectorProps<T extends string> = {
  className?: string;
  value: T;
  options: readonly SelectorOption<T>[];
  renderTrigger: (state: ResponsiveSelectorTriggerState<T>) => ReactNode;
  onChange: (value: T) => void;
};

function DesktopResponsiveSelector<T extends string>({
  className,
  value,
  options,
  renderTrigger,
  onChange,
}: DesktopResponsiveSelectorProps<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const selected = options.find((opt) => opt.value === value);

  return (
    <div className={cn(className)}>
      <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
        <DropdownMenuTrigger asChild>{renderTrigger({ isOpen, selected })}</DropdownMenuTrigger>
        <DropdownMenuContent className="min-w-40" align="end">
          <DropdownMenuRadioGroup value={value} onValueChange={handleSelect}>
            {options.map((opt) => (
              <DropdownMenuRadioItem key={opt.value} value={opt.value}>
                {opt.content}
              </DropdownMenuRadioItem>
            ))}
          </DropdownMenuRadioGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );

  function handleSelect(next: string) {
    const target = options.find((opt) => opt.value === next);
    if (target && target.value !== value) {
      onChange(target.value);
    }
  }
}
