import { HomePage } from "@/pages/home";
import { SubCategorySlug } from "@/shared/config";

export default function Page() {
  return <HomePage categorySlug={SubCategorySlug.Latest} />;
}
