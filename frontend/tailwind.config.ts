import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: ["./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        background: "#09090B",
        foreground: "#FAFAFA",
        muted: "#27272A",
        card: "#18181B",
        border: "#27272A",
        primary: "#22D3EE",
        secondary: "#A3E635"
      }
    }
  },
  plugins: []
};

export default config;
