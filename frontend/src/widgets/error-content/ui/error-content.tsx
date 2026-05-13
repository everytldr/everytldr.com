"use client";

import { Link } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Button, Container, Translation } from "@/shared/ui";
import { useState, type ComponentProps } from "react";
import ErrorIllustration from "./error-illustration.svg";
import NotFoundIllustration from "./not-found-illustration.svg";

type TranslationKey = ComponentProps<typeof Translation>["tKey"];

type ErrorContentProps = {
  className?: string;
  variant: "error" | "not-found";
  titleTKey: TranslationKey;
  descriptionTKey: TranslationKey;
  errorDigest?: string;
};

export function ErrorContent({
  className,
  variant,
  titleTKey,
  descriptionTKey,
  errorDigest,
}: ErrorContentProps) {
  const [showDetails, setShowDetails] = useState(false);
  const Illustration = variant === "error" ? ErrorIllustration : NotFoundIllustration;

  return (
    <Container
      className={cn(
        "flex flex-col items-center justify-center gap-xl py-section text-center",
        className,
      )}
    >
      <Illustration className="w-full max-w-112" aria-hidden="true" />
      <div className="flex w-full max-w-112 flex-col gap-sm">
        <Translation className="text-display-xl text-ink" tKey={titleTKey} as="h1" />
        <Translation className="text-body-md text-meta" tKey={descriptionTKey} as="p" />
      </div>
      <div className="flex flex-col items-center gap-sm">
        <Button variant="primary" asChild>
          <Link href="/">
            <Translation tKey="common.error-content.cta-home" />
          </Link>
        </Button>
        {errorDigest && (
          <div className="flex flex-col items-center gap-xs">
            <Button variant="link" onClick={handleToggleDetails}>
              <Translation
                tKey={
                  showDetails
                    ? "common.error-content.hide-details"
                    : "common.error-content.show-details"
                }
              />
            </Button>
            {showDetails && (
              <code className="rounded-xs bg-surface-soft px-xs py-2xs text-caption-mono text-meta select-all">
                {errorDigest}
              </code>
            )}
          </div>
        )}
      </div>
    </Container>
  );

  function handleToggleDetails() {
    setShowDetails((prev) => !prev);
  }
}
