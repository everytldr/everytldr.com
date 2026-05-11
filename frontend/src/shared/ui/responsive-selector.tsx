"use client";

import { useIsCoarsePointer } from "@/shared/hooks";
import { cn, type Optional } from "@/shared/lib";
import { Check } from "lucide-react";
import { type ReactNode, useState } from "react";
import { BottomSheet } from "./bottom-sheet";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "./dropdown-menu";

export type SelectorOption<T extends string> = {
  value: T;
  content: ReactNode;
};

type ResponsiveSelectorSingleProps<T extends string> = {
  className?: string;
  multiple?: false;
  value: T;
  options: readonly SelectorOption<T>[];
  title: string;
  renderMobileTrigger: (state: {
    selected: Optional<SelectorOption<T>>;
    open: () => void;
  }) => ReactNode;
  renderDesktopTrigger: (state: { selected: Optional<SelectorOption<T>> }) => ReactNode;
  onChange: (value: T) => void;
};

type ResponsiveSelectorMultiProps<T extends string> = {
  className?: string;
  multiple: true;
  value: readonly T[];
  options: readonly SelectorOption<T>[];
  title: string;
  renderMobileTrigger: (state: {
    selected: readonly SelectorOption<T>[];
    open: () => void;
  }) => ReactNode;
  renderDesktopTrigger: (state: { selected: readonly SelectorOption<T>[] }) => ReactNode;
  onChange: (values: readonly T[]) => void;
};

type ResponsiveSelectorProps<T extends string> =
  | ResponsiveSelectorSingleProps<T>
  | ResponsiveSelectorMultiProps<T>;

function toggleValue<T>(values: readonly T[], target: T): readonly T[] {
  return values.includes(target) ? values.filter((v) => v !== target) : [...values, target];
}

export function ResponsiveSelector<T extends string>(props: ResponsiveSelectorProps<T>) {
  return props.multiple ? (
    <MultiResponsiveSelector<T> {...props} />
  ) : (
    <SingleResponsiveSelector<T> {...props} />
  );
}

function SingleResponsiveSelector<T extends string>(props: ResponsiveSelectorSingleProps<T>) {
  const isCoarsePointer = useIsCoarsePointer();

  return isCoarsePointer ? (
    <MobileSingleSelector<T> {...props} />
  ) : (
    <DesktopSingleSelector<T> {...props} />
  );
}

function MultiResponsiveSelector<T extends string>(props: ResponsiveSelectorMultiProps<T>) {
  const isCoarsePointer = useIsCoarsePointer();

  return isCoarsePointer ? (
    <MobileMultiSelector<T> {...props} />
  ) : (
    <DesktopMultiSelector<T> {...props} />
  );
}

function MobileSingleSelector<T extends string>({
  className,
  value,
  options,
  title,
  renderMobileTrigger,
  onChange,
}: ResponsiveSelectorSingleProps<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const selected = options.find((opt) => opt.value === value);

  return (
    <div className={cn(className)}>
      {renderMobileTrigger({ selected, open: () => setIsOpen(true) })}
      <BottomSheet isOpen={isOpen} header={{ title }} onClose={() => setIsOpen(false)}>
        <div className="flex flex-col gap-2xs px-md pb-md" role="radiogroup" aria-label={title}>
          {options.map((opt) => {
            const isCurrent = opt.value === value;
            return (
              <button
                key={opt.value}
                className="flex h-12 cursor-pointer items-center justify-between gap-sm rounded-md px-md text-button-md text-ink transition-colors outline-none hover:bg-surface-soft focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:bg-surface-strong dark:hover:bg-surface-strong dark:active:bg-surface-pressed"
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

function DesktopSingleSelector<T extends string>({
  className,
  value,
  options,
  renderDesktopTrigger,
  onChange,
}: ResponsiveSelectorSingleProps<T>) {
  const selected = options.find((opt) => opt.value === value);

  return (
    <div className={cn(className)}>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>{renderDesktopTrigger({ selected })}</DropdownMenuTrigger>
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

function MobileMultiSelector<T extends string>({
  className,
  value,
  options,
  title,
  renderMobileTrigger,
  onChange,
}: ResponsiveSelectorMultiProps<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const selected = options.filter((opt) => value.includes(opt.value));

  return (
    <div className={cn(className)}>
      {renderMobileTrigger({ selected, open: () => setIsOpen(true) })}
      <BottomSheet isOpen={isOpen} header={{ title }} onClose={() => setIsOpen(false)}>
        <div className="flex flex-col gap-2xs px-md pb-md" role="group" aria-label={title}>
          {options.map((opt) => {
            const isCurrent = value.includes(opt.value);
            return (
              <button
                key={opt.value}
                className="flex h-12 cursor-pointer items-center justify-between gap-sm rounded-md px-md text-button-md text-ink transition-colors outline-none hover:bg-surface-soft focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:bg-surface-strong dark:hover:bg-surface-strong dark:active:bg-surface-pressed"
                type="button"
                role="checkbox"
                aria-checked={isCurrent}
                onClick={() => onChange(toggleValue(value, opt.value))}
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
}

function DesktopMultiSelector<T extends string>({
  className,
  value,
  options,
  renderDesktopTrigger,
  onChange,
}: ResponsiveSelectorMultiProps<T>) {
  const selected = options.filter((opt) => value.includes(opt.value));

  return (
    <div className={cn(className)}>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>{renderDesktopTrigger({ selected })}</DropdownMenuTrigger>
        <DropdownMenuContent className="min-w-40" align="end">
          {options.map((opt) => (
            <DropdownMenuCheckboxItem
              key={opt.value}
              checked={value.includes(opt.value)}
              onCheckedChange={() => onChange(toggleValue(value, opt.value))}
              // keep menu open on toggle
              onSelect={(e) => e.preventDefault()}
            >
              {opt.content}
            </DropdownMenuCheckboxItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
