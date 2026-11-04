'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Plus, Users, FolderKanban, CheckCircle2 } from 'lucide-react';
import { Button } from '../ui/Button';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/Input';

export function QuickActions() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [projectName, setProjectName] = useState('');
  const [projectDescription, setProjectDescription] = useState('');
  const [techStack, setTechStack] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (projectName.trim()) {
      setIsSubmitted(true);
      setTimeout(() => {
        setIsSubmitted(false);
        setIsModalOpen(false);
        setProjectName('');
        setProjectDescription('');
        setTechStack('');
      }, 1500);
    }
  };

  return (
    <>
      <div className="rounded-xl bg-card border border-border p-5 space-y-4">
        <h2 className="text-sm sm:text-base font-bold text-foreground tracking-tight border-b border-border pb-3">
          Quick Actions
        </h2>

        <div className="space-y-2.5">
          {/* Big red button matching mockup: + Create New Project */}
          <Button
            variant="primary"
            size="md"
            className="w-full text-xs sm:text-sm font-semibold h-11"
            leftIcon={<Plus className="w-4 h-4" />}
            onClick={() => setIsModalOpen(true)}
          >
            Create New Project
          </Button>

          <div className="grid grid-cols-2 gap-2 pt-1">
            <Link href="/developers">
              <Button
                variant="secondary"
                size="sm"
                className="w-full text-xs"
                leftIcon={<Users className="w-3.5 h-3.5 text-muted" />}
              >
                Find Teammates
              </Button>
            </Link>
            <Link href="/projects">
              <Button
                variant="secondary"
                size="sm"
                className="w-full text-xs"
                leftIcon={<FolderKanban className="w-3.5 h-3.5 text-muted" />}
              >
                View Projects
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* Create Project Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create New Project"
        description="Launch a new project on BuildSync to find collaborators and synchronize development."
      >
        {isSubmitted ? (
          <div className="flex flex-col items-center justify-center py-6 text-center space-y-2">
            <div className="w-12 h-12 rounded-full bg-emerald-500/10 text-emerald-500 flex items-center justify-center mb-1">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <h4 className="text-base font-bold text-foreground">Project Draft Created!</h4>
            <p className="text-xs text-muted max-w-sm">
              Your project &ldquo;{projectName}&rdquo; draft has been initialized locally.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Project Title"
              placeholder="e.g. Distributed Task Queue"
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              required
            />

            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground tracking-wide">
                Short Description
              </label>
              <textarea
                className="w-full h-24 rounded-lg bg-surface border border-border px-3.5 py-2 text-sm text-foreground placeholder:text-muted/60 transition-all focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/40"
                placeholder="Describe what you are building and what teammates you need..."
                value={projectDescription}
                onChange={(e) => setProjectDescription(e.target.value)}
                required
              />
            </div>

            <Input
              label="Primary Technologies (comma separated)"
              placeholder="e.g. Next.js, Spring Boot, PostgreSQL"
              value={techStack}
              onChange={(e) => setTechStack(e.target.value)}
            />

            <div className="p-3 rounded-lg bg-surface border border-border text-[11px] text-muted flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-[#EF4444]" />
              <span>Project will be published once Phase 3 backend integration is connected.</span>
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-border">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => setIsModalOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" variant="primary" size="sm">
                Create Project
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </>
  );
}

