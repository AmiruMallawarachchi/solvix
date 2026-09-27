import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Solvix | Ticket workspace",
  description: "A focused workspace for managing support tickets.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
