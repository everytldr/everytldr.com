"use client";

import { ErrorContent } from "@/widgets/error-content";

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

export default function Error({ error }: ErrorProps) {
  return (
    <ErrorContent
      variant="error"
      titleTKey="common.error.title"
      descriptionTKey="common.error.description"
      errorDigest={error.digest}
    />
  );
}
