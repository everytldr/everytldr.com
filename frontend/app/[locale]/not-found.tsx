import { ErrorContent } from "@/widgets/error-content";

export default function NotFound() {
  return (
    <ErrorContent
      variant="not-found"
      titleTKey="common.not-found.title"
      descriptionTKey="common.not-found.description"
    />
  );
}
