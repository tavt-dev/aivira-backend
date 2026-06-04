/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Outfit", "sans-serif"],
        serif: ["Cormorant Garamond", "serif"],
        display: ["Bebas Neue", "sans-serif"]
      }
    }
  },
  plugins: []
};
