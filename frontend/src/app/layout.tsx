import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import './globals.css';
import { ThemeProvider } from '@/context/ThemeContext';
import { AuthProvider } from '@/context/AuthContext';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

export const metadata: Metadata = {
  title: 'BuildSync — Bringing Developers and Projects Into Sync',
  description:
    'Full-Stack Developer Collaboration Platform. Showcase projects, match with developers by technical skill, and collaborate in sync.',
  keywords: [
    'Developer Platform',
    'Collaboration',
    'Software Projects',
    'Next.js',
    'Spring Boot',
    'PostgreSQL',
    'Cyber Ember Red',
  ],
};

const themeScript = `
(function() {
  try {
    var stored = localStorage.getItem('buildsync-theme');
    var isDark = stored === 'dark' || (!stored && true) || (stored === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  } catch (e) {}
})();
`;

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body className="min-h-full flex flex-col bg-background text-foreground antialiased selection:bg-red-500/30 selection:text-red-400">
        <ThemeProvider>
          <AuthProvider>{children}</AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
