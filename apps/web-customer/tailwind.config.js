/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{vue,js}', '../../packages/shared/ui/**/*.vue'],
  theme: {
    extend: {
      colors: {
        apple: {
          bg: '#f5f5f7',
          text: '#1d1d1f',
          secondary: '#86868b',
          tertiary: '#aeaeb2',
          accent: '#0071e3',
          'accent-hover': '#0077ed',
          border: '#d2d2d7',
        },
      },
      fontFamily: {
        sans: [
          '-apple-system', 'BlinkMacSystemFont', '"SF Pro Display"',
          '"SF Pro Text"', '"PingFang SC"', '"Helvetica Neue"', 'sans-serif',
        ],
      },
    },
  },
  plugins: [],
};
