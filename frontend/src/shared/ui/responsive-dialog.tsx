"use client";

import { useIsCoarsePointer } from "@/shared/hooks";
import type { PropsWithChildren } from "react";
import { BottomSheet } from "./bottom-sheet";
import { Modal } from "./modal";

type ResponsiveDialogProps = PropsWithChildren<{
  className?: string;
  isOpen: boolean;
  header: {
    className?: string;
    title: string;
    description?: string;
  };
  onClose: () => void;
}>;

export function ResponsiveDialog(props: ResponsiveDialogProps) {
  const isCoarsePointer = useIsCoarsePointer();

  if (isCoarsePointer) {
    return <BottomSheet {...props} />;
  }

  return <Modal {...props} />;
}
