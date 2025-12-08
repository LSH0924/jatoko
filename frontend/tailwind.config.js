/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        primary: "#05B084",
        "background-light": "#F1EDEA",
        "background-dark": "#121212",
        "line": "#015A84",
        "button-secondary": "#BADFCD",
      },
      fontFamily: {
        display: ["Roboto", "sans-serif"],
      },
      borderRadius: {
        DEFAULT: "0.5rem", // 8px
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/typography'),
  ],
}
