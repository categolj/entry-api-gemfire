/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        paper: '#E9E5DB',
        sheet: '#F7F4EC',
        ink: '#171811',
        'ink-soft': '#3B3C34',
        muted: '#68675C',
        rule: '#C9C4B5',
        pencil: '#2B3FD0',
        'pencil-deep': '#2233AE',
        'pencil-light': '#9AA6FF',
        'pencil-wash': '#DFE2F8',
        press: '#1C6A4D',
        'press-wash': '#DDEBE2',
        redline: '#A83224',
        'redline-wash': '#F3DDD8',
      },
      fontFamily: {
        sans: ['"Bricolage Grotesque"', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      letterSpacing: {
        tightest: '-0.045em',
      },
      boxShadow: {
        slip: '0 24px 60px -24px rgba(23, 24, 17, 0.35)',
      },
    },
  },
  plugins: [],
}
