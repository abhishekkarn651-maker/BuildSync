'use client';

import React from 'react';
import Link from 'next/link';
import { Moon, Sun, ArrowRight, ExternalLink } from 'lucide-react';
import { BrandLogo } from '../ui/BrandLogo';
import { Button } from '../ui/Button';
import { useTheme } from '@/context/ThemeContext';
import { API_CONFIG } from '@/config/api.config';

export function PublicNavbar() {
  const { resolvedTheme, toggleTheme } = useTheme();

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border bg-card/85 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center gap-6">
          <BrandLogo size="md" href="/" />

          <nav className="hidden md:flex items-center gap-5 text-xs font-semibold text-muted">
            <a href="#how-it-works" className="hover:text-foreground transition-colors">
              How It Works
            </a>
            <a href="#developers" className="hover:text-foreground transition-colors">
              Developers
            </a>
            <a href="#projects" className="hover:text-foreground transition-colors">
              Projects
            </a>
            <a href="#features" className="hover:text-foreground transition-colors">
              Features
            </a>
            <a
              href={`${API_CONFIG.baseUrl}${API_CONFIG.endpoints.swagger}`}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1 hover:text-[#EF4444] transition-colors"
            >
              <span>Swagger API</span>
              <ExternalLink className="w-3 h-3" />
            </a>
          </nav>
        </div>

        {/* Right Actions */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Theme switch button */}
          <button
            type="button"
            onClick={toggleTheme}
            className="p-2 rounded-lg text-muted hover:text-foreground hover:bg-surface transition-colors"
            title={`Switch to ${resolvedTheme === 'dark' ? 'Light' : 'Dark'} Mode`}
            aria-label="Toggle theme"
          >
            {resolvedTheme === 'dark' ? (
              <Moon className="w-4 h-4" />
            ) : (
              <Sun className="w-4 h-4 text-amber-500" />
            )}
          </button>

          <Link href="/login">
            <Button variant="ghost" size="sm" className="text-xs font-medium">
              Sign In
            </Button>
          </Link>

          <Link href="/register">
            <Button
              variant="primary"
              size="sm"
              className="text-xs font-semibold"
              rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
            >
              Get Started
            </Button>
          </Link>
        </div>
      </div>
    </header>
  );
}

