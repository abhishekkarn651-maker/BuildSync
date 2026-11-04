import React from 'react';
import Link from 'next/link';
import { BrandLogo } from '../ui/BrandLogo';
import { GithubIcon, TwitterIcon, LinkedinIcon } from '../ui/Icons';
import { Heart } from 'lucide-react';
import { API_CONFIG } from '@/config/api.config';

export function PublicFooter() {
  return (
    <footer className="border-t border-border bg-card/60 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 lg:py-16">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand info */}
          <div className="space-y-3 md:col-span-1">
            <BrandLogo size="md" href="/" />
            <p className="text-xs text-muted leading-relaxed">
              Bringing Developers and Projects Into Sync. Discover talented teammates, showcase open source work, and build scalable systems together.
            </p>
            <div className="flex items-center gap-3 pt-2 text-muted">
              <a
                href="https://github.com/BuildSync"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-foreground transition-colors p-1.5 rounded-lg hover:bg-surface"
                aria-label="GitHub"
              >
                <GithubIcon className="w-4 h-4" />
              </a>
              <a
                href="#"
                className="hover:text-foreground transition-colors p-1.5 rounded-lg hover:bg-surface"
                aria-label="Twitter"
              >
                <TwitterIcon className="w-4 h-4" />
              </a>
              <a
                href="#"
                className="hover:text-foreground transition-colors p-1.5 rounded-lg hover:bg-surface"
                aria-label="LinkedIn"
              >
                <LinkedinIcon className="w-4 h-4" />
              </a>
            </div>
          </div>

          {/* Platform Links */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-foreground uppercase tracking-wider">
              Platform
            </h4>
            <ul className="space-y-2 text-xs text-muted">
              <li>
                <Link href="/dashboard" className="hover:text-foreground transition-colors">
                  Developer Dashboard
                </Link>
              </li>
              <li>
                <Link href="/developers" className="hover:text-foreground transition-colors">
                  Find Teammates
                </Link>
              </li>
              <li>
                <Link href="/projects" className="hover:text-foreground transition-colors">
                  Project Showcase
                </Link>
              </li>
              <li>
                <Link href="/collaborations" className="hover:text-foreground transition-colors">
                  Collaboration Hub
                </Link>
              </li>
            </ul>
          </div>

          {/* Developers & Architecture */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-foreground uppercase tracking-wider">
              Architecture
            </h4>
            <ul className="space-y-2 text-xs text-muted">
              <li>
                <a
                  href={`${API_CONFIG.baseUrl}${API_CONFIG.endpoints.swagger}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-foreground transition-colors"
                >
                  Swagger UI (OpenAPI 3.1)
                </a>
              </li>
              <li>
                <span className="text-muted/70">Spring Boot 3.4.3</span>
              </li>
              <li>
                <span className="text-muted/70">PostgreSQL 17 (Flyway)</span>
              </li>
              <li>
                <span className="text-muted/70">Next.js 16 (React 19)</span>
              </li>
            </ul>
          </div>

          {/* Design System & Identity */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-foreground uppercase tracking-wider">
              Design System
            </h4>
            <p className="text-xs text-muted leading-relaxed">
              Engineered with <span className="text-[#EF4444] font-semibold">Cyber Ember Red (#05)</span>. Designed specifically for high-efficiency software collaboration.
            </p>
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-surface border border-border text-[11px] text-muted">
              <span className="w-2 h-2 rounded-full bg-[#EF4444]" />
              <span>Theme: Cyber Ember Red</span>
            </div>
          </div>
        </div>

        <div className="border-t border-border mt-10 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-muted">
          <p>© {new Date().getFullYear()} BuildSync Inc. All rights reserved.</p>
          <div className="flex items-center gap-1">
            <span>Built with precision for software craftsmen</span>
            <Heart className="w-3 h-3 text-[#EF4444] fill-current" />
          </div>
        </div>
      </div>
    </footer>
  );
}
