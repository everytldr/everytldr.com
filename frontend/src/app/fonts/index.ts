import { JetBrains_Mono } from "next/font/google";
import localFont from "next/font/local";

export const pretendard = localFont({
  src: "./PretendardVariable.woff2",
  variable: "--font-pretendard",
  weight: "45 920",
  style: "normal",
  display: "swap",
});

export const jetbrainsMono = JetBrains_Mono({
  variable: "--font-jetbrains-mono",
  subsets: ["latin"],
  display: "swap",
  weight: ["400", "500"],
});
