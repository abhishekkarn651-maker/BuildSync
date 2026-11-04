'use client';

import React from 'react';
import Link from 'next/link';
import {
  ArrowRight,
  Search,
  MessageSquare,
  GitBranch,
} from 'lucide-react';
import { PublicNavbar } from '@/components/layout/PublicNavbar';
import { PublicFooter } from '@/components/layout/PublicFooter';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { MOCK_DEVELOPERS, MOCK_PROJECTS } from '@/lib/mock-data';

export default function LandingPage() {
  const featuredDevs = MOCK_DEVELOPERS.slice(0, 4);
  const featuredProjects = MOCK_PROJECTS.slice(0, 3);

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground relative selection:bg-red-500/20 selection:text-red-400">
      {/* Background ambient ember glow effects */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-red-600/10 blur-[140px] pointer-events-none rounded-full" />

      {/* Top Navbar */}
      <PublicNavbar />

      <main className="flex-1">
        {/* HERO SECTION */}
        <section className="relative px-4 sm:px-6 lg:px-8 pt-16 sm:pt-24 pb-16 max-w-7xl mx-auto text-center space-y-6">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-surface border border-border shadow-sm">
            <span className="flex h-2 w-2 rounded-full bg-[#EF4444] animate-pulse" />
            <span className="text-xs font-semibold text-foreground tracking-wide">
              Cyber Ember Red Edition — Developer Platform
            </span>
          </div>

          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-foreground max-w-4xl mx-auto leading-[1.1]">
            Bringing Developers and Projects Into{' '}
            <span className="text-[#EF4444] relative inline-block">
              Sync.
              <span className="absolute bottom-1 left-0 w-full h-[3px] bg-[#EF4444]/40 rounded-full" />
            </span>
          </h1>

          <p className="text-base sm:text-xl text-muted max-w-2xl mx-auto leading-relaxed font-normal">
            Discover talented developers, showcase your projects, find your next teammates and turn ideas into reality.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-3 pt-4">
            <Link href="/register">
              <Button
                variant="primary"
                size="lg"
                className="text-sm font-semibold h-12 px-6 shadow-lg shadow-red-500/25"
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Get Started
              </Button>
            </Link>
            <Link href="/projects">
              <Button
                variant="secondary"
                size="lg"
                className="text-sm font-medium h-12 px-6"
              >
                Explore Projects
              </Button>
            </Link>
          </div>

          <div className="pt-10 max-w-5xl mx-auto">
            <div className="p-2 sm:p-3 rounded-2xl bg-card border border-border shadow-2xl shadow-red-500/5 transition-all">
              <div className="p-4 sm:p-6 rounded-xl bg-surface border border-border flex flex-col sm:flex-row items-center justify-between gap-4 text-left">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-[#EF4444]">&lt;/&gt;</span>
                    <span className="text-xs font-bold uppercase tracking-wider text-foreground">
                      Explore Developer Dashboard
                    </span>
                  </div>
                  <p className="text-xs text-muted">
                    Experience the Cyber Ember Red workspace mockup featuring real live PostgreSQL & Spring Boot telemetry.
                  </p>
                </div>
                <Link href="/dashboard">
                  <Button variant="primary" size="sm" className="whitespace-nowrap">
                    Launch Dashboard →
                  </Button>
                </Link>
              </div>
            </div>
          </div>
        </section>

        {/* HOW BUILDSYNC WORKS */}
        <section id="how-it-works" className="px-4 sm:px-6 lg:px-8 py-20 max-w-7xl mx-auto border-t border-border">
          <div className="text-center max-w-2xl mx-auto mb-16 space-y-2">
            <h2 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
              How BuildSync Works
            </h2>
            <p className="text-sm text-muted">
              A streamlined three-step workflow designed specifically for software engineers.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-6 rounded-2xl bg-card border border-border space-y-4 hover:border-red-500/30 transition-all">
              <div className="w-12 h-12 rounded-xl bg-red-500/10 border border-red-500/20 text-[#EF4444] flex items-center justify-center font-mono font-bold text-lg">
                01
              </div>
              <h3 className="text-base font-bold text-foreground">
                Create Developer Profile
              </h3>
              <p className="text-xs text-muted leading-relaxed">
                Highlight your core programming languages, frameworks, GitHub projects, and availability to match with suitable teams.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-card border border-border space-y-4 hover:border-red-500/30 transition-all">
              <div className="w-12 h-12 rounded-xl bg-red-500/10 border border-red-500/20 text-[#EF4444] flex items-center justify-center font-mono font-bold text-lg">
                02
              </div>
              <h3 className="text-base font-bold text-foreground">
                Showcase Your Projects
              </h3>
              <p className="text-xs text-muted leading-relaxed">
                Publish full-stack applications with architecture overviews, GitHub repository URLs, live demo links, and open collaborator roles.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-card border border-border space-y-4 hover:border-red-500/30 transition-all">
              <div className="w-12 h-12 rounded-xl bg-red-500/10 border border-red-500/20 text-[#EF4444] flex items-center justify-center font-mono font-bold text-lg">
                03
              </div>
              <h3 className="text-base font-bold text-foreground">
                Sync & Collaborate
              </h3>
              <p className="text-xs text-muted leading-relaxed">
                Send structured collaboration requests, communicate through dedicated message channels, and track active team milestones.
              </p>
            </div>
          </div>
        </section>

        {/* FEATURED DEVELOPERS */}
        <section id="developers" className="px-4 sm:px-6 lg:px-8 py-20 max-w-7xl mx-auto border-t border-border">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-12">
            <div>
              <h2 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
                Featured Developers
              </h2>
              <p className="text-sm text-muted mt-1">
                Connect with engineers across frontend, backend, AI/ML, and infrastructure domains.
              </p>
            </div>
            <Link
              href="/developers"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#EF4444] hover:text-red-400"
            >
              <span>Explore All Developers</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {featuredDevs.map((dev) => (
              <div
                key={dev.id}
                className="p-5 rounded-xl bg-card border border-border flex flex-col justify-between space-y-4 hover:border-red-500/30 transition-all group"
              >
                <div className="space-y-3">
                  <div className="flex items-center gap-3">
                    <Avatar src={dev.avatar} name={dev.name} size="md" isOnline />
                    <div className="min-w-0">
                      <h3 className="text-sm font-bold text-foreground truncate group-hover:text-[#EF4444] transition-colors">
                        {dev.name}
                      </h3>
                      <p className="text-xs text-muted truncate">@{dev.username}</p>
                    </div>
                  </div>

                  <p className="text-xs text-muted line-clamp-2 leading-relaxed">
                    {dev.bio}
                  </p>

                  <div className="flex flex-wrap gap-1">
                    {dev.skills.slice(0, 3).map((skill) => (
                      <Badge key={skill} variant="neutral" size="sm">
                        {skill}
                      </Badge>
                    ))}
                  </div>
                </div>

                <Link href={`/developers/${dev.username}`}>
                  <Button variant="secondary" size="sm" className="w-full text-xs">
                    View Profile
                  </Button>
                </Link>
              </div>
            ))}
          </div>
        </section>

        {/* FEATURED PROJECTS */}
        <section id="projects" className="px-4 sm:px-6 lg:px-8 py-20 max-w-7xl mx-auto border-t border-border">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-12">
            <div>
              <h2 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
                Featured Projects
              </h2>
              <p className="text-sm text-muted mt-1">
                Real software projects seeking active collaborators and contributors.
              </p>
            </div>
            <Link
              href="/projects"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#EF4444] hover:text-red-400"
            >
              <span>Explore All Projects</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {featuredProjects.map((project) => (
              <div
                key={project.id}
                className="p-6 rounded-2xl bg-card border border-border flex flex-col justify-between space-y-4 hover:border-red-500/30 transition-all"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-red-500/10 text-[#EF4444] border border-red-500/30">
                      {project.category}
                    </span>
                    <span className="text-xs text-muted">{project.status}</span>
                  </div>

                  <h3 className="text-base font-bold text-foreground">
                    {project.title}
                  </h3>

                  <p className="text-xs text-muted leading-relaxed line-clamp-3">
                    {project.detailedDescription}
                  </p>

                  <div className="flex flex-wrap gap-1.5 pt-2">
                    {project.technologies.map((tech) => (
                      <span
                        key={tech}
                        className="text-[11px] font-medium px-2 py-0.5 rounded bg-surface text-muted-foreground border border-border"
                      >
                        {tech}
                      </span>
                    ))}
                  </div>
                </div>

                <Link href={`/projects/${project.id}`}>
                  <Button variant="outline" size="sm" className="w-full text-xs">
                    Inspect Project
                  </Button>
                </Link>
              </div>
            ))}
          </div>
        </section>

        {/* BENEFITS */}
        <section id="features" className="px-4 sm:px-6 lg:px-8 py-20 max-w-7xl mx-auto border-t border-border">
          <div className="text-center max-w-2xl mx-auto mb-16 space-y-2">
            <h2 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
              Why Engineers Choose BuildSync
            </h2>
            <p className="text-sm text-muted">
              Built by developers, for developers — zero generic marketing fluff.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            <div className="p-6 rounded-xl bg-card border border-border space-y-3">
              <div className="w-10 h-10 rounded-lg bg-red-500/10 text-[#EF4444] flex items-center justify-center">
                <Search className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-foreground">
                Skill Matchmaking
              </h3>
              <p className="text-xs text-muted leading-relaxed">
                Filter prospective teammates by granular tech competencies such as Spring Boot, PostgreSQL, PyTorch, or Docker.
              </p>
            </div>

            <div className="p-6 rounded-xl bg-card border border-border space-y-3">
              <div className="w-10 h-10 rounded-lg bg-red-500/10 text-[#EF4444] flex items-center justify-center">
                <GitBranch className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-foreground">
                GitHub Integration
              </h3>
              <p className="text-xs text-muted leading-relaxed">
                Map repositories directly to showcase cards. Keep open issue telemetry synchronized with active team requests.
              </p>
            </div>

            <div className="p-6 rounded-xl bg-card border border-border space-y-3">
              <div className="w-10 h-10 rounded-lg bg-red-500/10 text-[#EF4444] flex items-center justify-center">
                <MessageSquare className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-foreground">
                Synchronized Messaging
              </h3>
              <p className="text-xs text-muted leading-relaxed">
                Direct channels and project chat rooms to discuss technical RFCs and align on sprint deadlines.
              </p>
            </div>
          </div>
        </section>

        {/* CTA */}
        <section className="px-4 sm:px-6 lg:px-8 py-20 max-w-5xl mx-auto">
          <div className="p-8 sm:p-12 rounded-3xl bg-card border border-border text-center space-y-6 relative overflow-hidden">
            <div className="absolute -right-20 -bottom-20 w-72 h-72 rounded-full bg-red-600/10 blur-3xl pointer-events-none" />

            <h2 className="text-3xl sm:text-4xl font-extrabold text-foreground tracking-tight">
              Ready to Sync Your Next Build?
            </h2>
            <p className="text-sm text-muted max-w-xl mx-auto leading-relaxed">
              Join thousands of developers turning ambitious software projects into shipped products.
            </p>

            <div className="pt-2 flex flex-wrap items-center justify-center gap-3">
              <Link href="/register">
                <Button variant="primary" size="lg" className="h-11 px-6 shadow-lg shadow-red-500/25">
                  Create Your Free Account
                </Button>
              </Link>
              <Link href="/login">
                <Button variant="secondary" size="lg" className="h-11 px-6">
                  Sign In to Dashboard
                </Button>
              </Link>
            </div>
          </div>
        </section>
      </main>

      <PublicFooter />
    </div>
  );
}
