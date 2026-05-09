// "use client";

// import { type Locale, usePathname, useRouter } from "@/shared/i18n";
// import { cn } from "@/shared/lib";
// import { Check, ChevronDown } from "lucide-react";
// import { useLocale, useTranslations } from "next-intl";

// type LanguageDef = {
//   code: string;
//   glyph: string;
//   native: string;
//   supported: boolean;
// };

// const LANGUAGES: LanguageDef[] = [
//   { code: "ko", glyph: "KO", native: "한국어", supported: true },
//   { code: "en", glyph: "EN", native: "English", supported: true },
//   { code: "ja", glyph: "JA", native: "日本語", supported: false },
//   { code: "es", glyph: "ES", native: "Español", supported: false },
//   { code: "zh", glyph: "ZH", native: "中文", supported: false },
// ];

// type LanguagePickerProps = {
//   className?: string;
// };

// export function LanguagePicker({ className }: LanguagePickerProps) {
//   const t = useTranslations("header");
//   const router = useRouter();
//   const pathname = usePathname();
//   const currentCode = useLocale();
//   const current = LANGUAGES.find((lang) => lang.code === currentCode) ?? LANGUAGES[1];

//   const handleSelect = (code: string) => {
//     const target = LANGUAGES.find((lang) => lang.code === code);
//     if (!target?.supported || code === currentCode) {
//       return;
//     }
//     router.replace(pathname, { locale: code as Locale });
//   };

//   return (
//     <DropdownMenu>
//       <DropdownMenuTrigger
//         className={cn(
//           "inline-flex h-8 items-center gap-1.5 rounded-full bg-surface-soft px-3 text-button-sm text-ink transition-colors outline-none hover:bg-surface-strong focus-visible:ring-2 focus-visible:ring-primary",
//           "[&[data-state=open]>svg:last-child]:rotate-180",
//           className,
//         )}
//         aria-label={`${t("language")}: ${current.native}`}
//       >
//         <span className="font-mono text-caption-mono text-meta">{current.glyph}</span>
//         <span>{current.native}</span>
//         <ChevronDown className="size-3.5 text-meta transition-transform" />
//       </DropdownMenuTrigger>

//       <DropdownMenuContent className="min-w-[220px]" align="end" sideOffset={6}>
//         <DropdownMenuLabel className="px-2.5 pt-2 pb-1.5 text-micro text-meta uppercase">
//           {t("language-menu-header")}
//         </DropdownMenuLabel>

//         <DropdownMenuRadioGroup value={currentCode} onValueChange={handleSelect}>
//           {LANGUAGES.map((lang) => {
//             const isCurrent = lang.code === currentCode;
//             const disabled = !lang.supported;
//             return (
//               <DropdownMenuRadioItem
//                 key={lang.code}
//                 className={cn("justify-between text-ink", !disabled && "cursor-pointer")}
//                 disabled={disabled}
//                 value={lang.code}
//               >
//                 <span className="inline-flex items-center gap-2.5">
//                   <span
//                     className={cn(
//                       "font-mono text-caption-mono",
//                       isCurrent ? "text-primary" : "text-meta",
//                     )}
//                   >
//                     {lang.glyph}
//                   </span>
//                   <span>{lang.native}</span>
//                 </span>
//                 <span className="inline-flex items-center gap-1.5 text-caption-mono text-meta">
//                   {disabled && <span>{t("soon-suffix")}</span>}
//                   {isCurrent && <Check className="size-3.5" />}
//                 </span>
//               </DropdownMenuRadioItem>
//             );
//           })}
//         </DropdownMenuRadioGroup>

//         <DropdownMenuSeparator />

//         <div className="px-2.5 pt-1 pb-1.5 text-[11px] leading-[1.4] text-meta">
//           {t("language-menu-foot")}
//         </div>
//       </DropdownMenuContent>
//     </DropdownMenu>
//   );
// }
