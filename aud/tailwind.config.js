/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        base: {
          950: '#0a0a0f',
          900: '#101016',
          850: '#14141c',
          800: '#1a1a23',
          750: '#20202b',
          700: '#2a2a36',
          600: '#3a3a48',
        },
        brand: {
          400: '#3ee87f',
          500: '#1ed760',
          600: '#1db954',
          700: '#169c46',
        },
        text: {
          primary: '#f5f5f7',
          secondary: '#b3b3c0',
          muted: '#75757f',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      boxShadow: {
        'card': '0 16px 40px rgba(0,0,0,0.45)',
        'glow': '0 0 24px rgba(30,215,96,0.35)',
        'player': '0 -8px 40px rgba(0,0,0,0.5)',
      },
      borderRadius: {
        '2xl': '1rem',
        '3xl': '1.5rem',
      },
      keyframes: {
        eq1: { '0%,100%': { transform: 'scaleY(0.3)' }, '50%': { transform: 'scaleY(1)' } },
        eq2: { '0%,100%': { transform: 'scaleY(0.8)' }, '50%': { transform: 'scaleY(0.25)' } },
        eq3: { '0%,100%': { transform: 'scaleY(0.5)' }, '50%': { transform: 'scaleY(1)' } },
        'fade-up': { from: { opacity: '0', transform: 'translateY(12px)' }, to: { opacity: '1', transform: 'translateY(0)' } },
        'toast-in': { from: { opacity: '0', transform: 'translateY(16px) scale(0.96)' }, to: { opacity: '1', transform: 'translateY(0) scale(1)' } },
        shimmer: { from: { backgroundPosition: '200% 0' }, to: { backgroundPosition: '-200% 0' } },
      },
      animation: {
        eq1: 'eq1 0.9s ease-in-out infinite',
        eq2: 'eq2 0.7s ease-in-out infinite',
        eq3: 'eq3 1.1s ease-in-out infinite',
        'fade-up': 'fade-up 0.45s cubic-bezier(0.22,1,0.36,1) both',
        'toast-in': 'toast-in 0.25s cubic-bezier(0.22,1,0.36,1) both',
        shimmer: 'shimmer 1.6s linear infinite',
      },
    },
  },
  plugins: [],
}
