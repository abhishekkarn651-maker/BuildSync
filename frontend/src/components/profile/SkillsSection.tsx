'use client';

import React, { useState, useEffect } from 'react';
import {
  Code2,
  Plus,
  Trash2,
  Edit2,
  Search,
  Check,
  AlertCircle,
  Sparkles,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Badge } from '@/components/ui/Badge';
import { skillsService } from '@/services/skills.service';
import { ApiError } from '@/services/api-client';
import {
  ProfileSkill,
  ProficiencyLevel,
  Skill,
  SkillCategory,
} from '@/types/profile.types';

interface SkillsSectionProps {
  skills: ProfileSkill[];
  editable?: boolean;
  onSkillsChange?: (updatedSkills: ProfileSkill[]) => void;
}

const CATEGORY_LABELS: Record<SkillCategory, string> = {
  PROGRAMMING_LANGUAGE: 'Languages',
  FRAMEWORK: 'Frameworks',
  DATABASE: 'Databases',
  DEVOPS: 'DevOps & Cloud',
  TOOL: 'Tools & Utilities',
  DESIGN: 'Design',
  OTHER: 'Other',
};

const PROFICIENCY_CONFIG: Record<
  ProficiencyLevel,
  { label: string; variant: 'neutral' | 'primary' | 'warning' | 'success'; color: string }
> = {
  BEGINNER: { label: 'Beginner', variant: 'neutral', color: 'text-zinc-400 border-zinc-700/60' },
  INTERMEDIATE: { label: 'Intermediate', variant: 'primary', color: 'text-red-400 border-red-500/30' },
  ADVANCED: { label: 'Advanced', variant: 'success', color: 'text-emerald-400 border-emerald-500/30' },
  EXPERT: { label: 'Expert', variant: 'warning', color: 'text-amber-400 border-amber-500/30' },
};

export function SkillsSection({
  skills,
  editable = false,
  onSkillsChange,
}: SkillsSectionProps) {
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingSkill, setEditingSkill] = useState<ProfileSkill | null>(null);

  // Catalogue search state
  const [catalogue, setCatalogue] = useState<Skill[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<SkillCategory | ''>('');
  const [selectedSkill, setSelectedSkill] = useState<Skill | null>(null);
  const [proficiency, setProficiency] = useState<ProficiencyLevel>('INTERMEDIATE');

  const [isLoadingCatalogue, setIsLoadingCatalogue] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  // Load catalogue when modal opens or filters change
  useEffect(() => {
    if (!isAddModalOpen) return;

    let isMounted = true;

    const debounce = setTimeout(async () => {
      if (isMounted) {
        setIsLoadingCatalogue(true);
        setErrorMsg('');
      }
      try {
        const results = await skillsService.searchCatalogue(
          searchQuery,
          selectedCategory || undefined
        );
        if (isMounted) {
          setCatalogue(results);
        }
      } catch (err) {
        console.error('Failed to search skills:', err);
      } finally {
        if (isMounted) setIsLoadingCatalogue(false);
      }
    }, 200);

    return () => {
      isMounted = false;
      clearTimeout(debounce);
    };
  }, [isAddModalOpen, searchQuery, selectedCategory]);

  const handleOpenAddModal = () => {
    setSelectedSkill(null);
    setSearchQuery('');
    setSelectedCategory('');
    setProficiency('INTERMEDIATE');
    setErrorMsg('');
    setIsAddModalOpen(true);
  };

  const handleAddSkill = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSkill) {
      setErrorMsg('Please select a skill from the list.');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg('');

    try {
      const added = await skillsService.addSkill({
        skillId: selectedSkill.id,
        proficiencyLevel: proficiency,
      });

      const updated = [...skills, added];
      onSkillsChange?.(updated);
      setIsAddModalOpen(false);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message || 'Failed to add skill');
      } else {
        setErrorMsg('An unexpected error occurred.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateProficiency = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingSkill) return;

    setIsSubmitting(true);
    setErrorMsg('');

    try {
      const updatedItem = await skillsService.updateProficiency(editingSkill.skillId, {
        proficiencyLevel: proficiency,
      });

      const updated = skills.map((s) => (s.id === updatedItem.id ? updatedItem : s));
      onSkillsChange?.(updated);
      setEditingSkill(null);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message || 'Failed to update proficiency');
      } else {
        setErrorMsg('An unexpected error occurred.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRemoveSkill = async (skillId: string) => {
    if (!confirm('Are you sure you want to remove this skill from your profile?')) return;

    try {
      await skillsService.removeSkill(skillId);
      const updated = skills.filter((s) => s.skillId !== skillId);
      onSkillsChange?.(updated);
    } catch (err: unknown) {
      alert(err instanceof ApiError ? err.message : 'Failed to remove skill');
    }
  };

  // Set of assigned skill IDs to disable duplicates in search modal
  const assignedSkillIds = new Set(skills.map((s) => s.skillId));

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Code2 className="w-4 h-4 text-[#EF4444]" />
          <h3 className="text-sm font-bold uppercase tracking-wider text-muted">
            Technical Skills ({skills.length})
          </h3>
        </div>
        {editable && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            leftIcon={<Plus className="w-3.5 h-3.5" />}
            onClick={handleOpenAddModal}
            className="text-xs"
          >
            Add Skill
          </Button>
        )}
      </div>

      {skills.length === 0 ? (
        <div className="p-6 rounded-2xl bg-surface border border-border/60 text-center space-y-2">
          <Sparkles className="w-6 h-6 text-muted mx-auto" />
          <p className="text-xs text-muted">No technical skills added yet.</p>
          {editable && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleOpenAddModal}
              className="text-xs mt-2"
            >
              Add Your First Skill
            </Button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
          {skills.map((skill) => {
            const config = PROFICIENCY_CONFIG[skill.proficiencyLevel] || PROFICIENCY_CONFIG.INTERMEDIATE;
            return (
              <div
                key={skill.id}
                className="flex items-center justify-between p-3.5 rounded-xl bg-surface border border-border hover:border-border/80 transition-all group"
              >
                <div className="space-y-1 min-w-0 pr-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-foreground truncate">
                      {skill.name}
                    </span>
                    <Badge variant={config.variant} size="sm">
                      {config.label}
                    </Badge>
                  </div>
                  <p className="text-[10px] text-muted truncate">
                    {CATEGORY_LABELS[skill.category] || skill.category}
                  </p>
                </div>

                {editable && (
                  <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity shrink-0">
                    <button
                      type="button"
                      onClick={() => {
                        setEditingSkill(skill);
                        setProficiency(skill.proficiencyLevel);
                        setErrorMsg('');
                      }}
                      className="p-1.5 rounded-lg text-muted hover:text-foreground hover:bg-card transition-colors cursor-pointer"
                      title="Edit proficiency"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleRemoveSkill(skill.skillId)}
                      className="p-1.5 rounded-lg text-muted hover:text-red-400 hover:bg-red-500/10 transition-colors cursor-pointer"
                      title="Remove skill"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Add Skill Modal */}
      <Modal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        title="Add Technical Skill"
        description="Search from the BuildSync skills catalogue and select your proficiency level."
        maxWidth="md"
      >
        <form onSubmit={handleAddSkill} className="space-y-4">
          {errorMsg && (
            <div className="flex items-center gap-2 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-xs text-red-400">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Search Query Input */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-foreground">
              Search Skill
            </label>
            <div className="relative">
              <Search className="w-4 h-4 text-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="e.g. Java, React, Docker, PostgreSQL..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444]"
              />
            </div>
          </div>

          {/* Category Filter Pills */}
          <div className="flex flex-wrap gap-1.5">
            <button
              type="button"
              onClick={() => setSelectedCategory('')}
              className={`px-2.5 py-1 text-[11px] rounded-lg border transition-colors cursor-pointer ${
                selectedCategory === ''
                  ? 'bg-red-500/10 text-[#EF4444] border-red-500/30 font-semibold'
                  : 'bg-surface text-muted border-border hover:text-foreground'
              }`}
            >
              All Categories
            </button>
            {(Object.keys(CATEGORY_LABELS) as SkillCategory[]).map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() => setSelectedCategory(cat)}
                className={`px-2.5 py-1 text-[11px] rounded-lg border transition-colors cursor-pointer ${
                  selectedCategory === cat
                    ? 'bg-red-500/10 text-[#EF4444] border-red-500/30 font-semibold'
                    : 'bg-surface text-muted border-border hover:text-foreground'
                }`}
              >
                {CATEGORY_LABELS[cat]}
              </button>
            ))}
          </div>

          {/* Skill List Selection */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-foreground">
              Select Skill {catalogue.length > 0 && `(${catalogue.length} available)`}
            </label>
            <div className="max-h-48 overflow-y-auto space-y-1.5 p-2 rounded-xl bg-surface border border-border">
              {isLoadingCatalogue ? (
                <p className="text-center py-6 text-xs text-muted">Searching catalogue...</p>
              ) : catalogue.length === 0 ? (
                <p className="text-center py-6 text-xs text-muted">
                  No skills found matching your search.
                </p>
              ) : (
                catalogue.map((s) => {
                  const isAssigned = assignedSkillIds.has(s.id);
                  const isSelected = selectedSkill?.id === s.id;

                  return (
                    <button
                      key={s.id}
                      type="button"
                      disabled={isAssigned}
                      onClick={() => setSelectedSkill(s)}
                      className={`w-full flex items-center justify-between p-2.5 rounded-lg text-left text-xs transition-colors cursor-pointer ${
                        isAssigned
                          ? 'opacity-40 cursor-not-allowed bg-transparent text-muted'
                          : isSelected
                          ? 'bg-red-500/15 border border-[#EF4444] text-[#EF4444] font-semibold'
                          : 'bg-card hover:bg-surface-hover text-foreground border border-transparent'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <span>{s.name}</span>
                        <span className="text-[10px] text-muted">
                          ({CATEGORY_LABELS[s.category] || s.category})
                        </span>
                      </div>
                      {isAssigned ? (
                        <span className="text-[10px] text-muted">Already Added</span>
                      ) : isSelected ? (
                        <Check className="w-3.5 h-3.5 text-[#EF4444]" />
                      ) : null}
                    </button>
                  );
                })
              )}
            </div>
          </div>

          {/* Proficiency Level Selector */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-foreground">
              Proficiency Level
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {(['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'] as ProficiencyLevel[]).map(
                (lvl) => {
                  const isSelected = proficiency === lvl;
                  const cfg = PROFICIENCY_CONFIG[lvl];
                  return (
                    <button
                      key={lvl}
                      type="button"
                      onClick={() => setProficiency(lvl)}
                      className={`p-2.5 rounded-xl border text-center transition-all cursor-pointer ${
                        isSelected
                          ? 'bg-red-500/15 border-[#EF4444] text-[#EF4444] font-bold shadow-sm'
                          : 'bg-surface border-border text-muted hover:text-foreground hover:bg-surface-hover'
                      }`}
                    >
                      <span className="block text-xs">{cfg.label}</span>
                    </button>
                  );
                }
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-border">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsAddModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={isSubmitting}
              disabled={!selectedSkill || isSubmitting}
            >
              Add Skill
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Proficiency Modal */}
      <Modal
        isOpen={!!editingSkill}
        onClose={() => setEditingSkill(null)}
        title={`Update Proficiency: ${editingSkill?.name || ''}`}
        maxWidth="sm"
      >
        <form onSubmit={handleUpdateProficiency} className="space-y-4">
          {errorMsg && (
            <div className="flex items-center gap-2 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-xs text-red-400">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-foreground">
              Proficiency Level
            </label>
            <div className="grid grid-cols-2 gap-2">
              {(['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'] as ProficiencyLevel[]).map(
                (lvl) => {
                  const isSelected = proficiency === lvl;
                  const cfg = PROFICIENCY_CONFIG[lvl];
                  return (
                    <button
                      key={lvl}
                      type="button"
                      onClick={() => setProficiency(lvl)}
                      className={`p-2.5 rounded-xl border text-center transition-all cursor-pointer ${
                        isSelected
                          ? 'bg-red-500/15 border-[#EF4444] text-[#EF4444] font-bold shadow-sm'
                          : 'bg-surface border-border text-muted hover:text-foreground'
                      }`}
                    >
                      <span className="block text-xs">{cfg.label}</span>
                    </button>
                  );
                }
              )}
            </div>
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-border">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setEditingSkill(null)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" isLoading={isSubmitting}>
              Save Proficiency
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
