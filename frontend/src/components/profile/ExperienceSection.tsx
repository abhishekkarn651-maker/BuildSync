'use client';

import React, { useState } from 'react';
import {
  Briefcase,
  Plus,
  Trash2,
  Edit2,
  Calendar,
  AlertCircle,
  MapPin,
  Building,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Badge } from '@/components/ui/Badge';
import { experienceService } from '@/services/experience.service';
import { ApiError } from '@/services/api-client';
import { EmploymentType, ExperiencePayload, WorkExperience } from '@/types/profile.types';

interface ExperienceSectionProps {
  experience: WorkExperience[];
  editable?: boolean;
  onExperienceChange?: (updated: WorkExperience[]) => void;
}

const EMPLOYMENT_TYPE_LABELS: Record<EmploymentType, string> = {
  FULL_TIME: 'Full-time',
  PART_TIME: 'Part-time',
  INTERNSHIP: 'Internship',
  CONTRACT: 'Contract',
  FREELANCE: 'Freelance',
  SELF_EMPLOYED: 'Self-employed',
};

export function ExperienceSection({
  experience,
  editable = false,
  onExperienceChange,
}: ExperienceSectionProps) {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<WorkExperience | null>(null);

  // Form states
  const [jobTitle, setJobTitle] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [employmentType, setEmploymentType] = useState<EmploymentType>('FULL_TIME');
  const [location, setLocation] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [currentlyWorking, setCurrentlyWorking] = useState(false);
  const [description, setDescription] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleOpenAddModal = () => {
    setEditingItem(null);
    setJobTitle('');
    setCompanyName('');
    setEmploymentType('FULL_TIME');
    setLocation('');
    setStartDate('');
    setEndDate('');
    setCurrentlyWorking(false);
    setDescription('');
    setErrorMsg('');
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (item: WorkExperience) => {
    setEditingItem(item);
    setJobTitle(item.jobTitle || '');
    setCompanyName(item.companyName || '');
    setEmploymentType(item.employmentType || 'FULL_TIME');
    setLocation(item.location || '');
    setStartDate(item.startDate || '');
    setEndDate(item.endDate || '');
    setCurrentlyWorking(item.currentlyWorking || false);
    setDescription(item.description || '');
    setErrorMsg('');
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!jobTitle.trim() || !companyName.trim()) {
      setErrorMsg('Job title and company name are required.');
      return;
    }

    if (startDate && endDate && !currentlyWorking && endDate < startDate) {
      setErrorMsg('End date cannot precede start date.');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg('');

    const payload: ExperiencePayload = {
      jobTitle: jobTitle.trim(),
      companyName: companyName.trim(),
      employmentType,
      location: location.trim() || undefined,
      startDate: startDate || null,
      endDate: currentlyWorking ? null : endDate || null,
      currentlyWorking,
      description: description.trim() || undefined,
    };

    try {
      if (editingItem) {
        const updated = await experienceService.updateExperience(editingItem.id, payload);
        const newList = experience.map((item) => (item.id === updated.id ? updated : item));
        onExperienceChange?.(newList);
      } else {
        const created = await experienceService.addExperience(payload);
        const newList = [created, ...experience];
        onExperienceChange?.(newList);
      }
      setIsModalOpen(false);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message || 'Failed to save experience record');
      } else {
        setErrorMsg('An unexpected error occurred. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this work experience entry?')) return;

    try {
      await experienceService.deleteExperience(id);
      const newList = experience.filter((item) => item.id !== id);
      onExperienceChange?.(newList);
    } catch (err: unknown) {
      alert(err instanceof ApiError ? err.message : 'Failed to delete work experience entry');
    }
  };

  const formatDateRange = (start?: string | null, end?: string | null, isCurrent?: boolean) => {
    const formatMonthYear = (d?: string | null) => {
      if (!d) return '';
      const [year, month] = d.split('-');
      const date = new Date(parseInt(year), parseInt(month) - 1);
      return date.toLocaleDateString(undefined, { month: 'short', year: 'numeric' });
    };

    const s = formatMonthYear(start);
    if (isCurrent) return `${s || 'Started'} — Present`;
    const e = formatMonthYear(end);
    if (!s && !e) return '';
    return `${s} — ${e || 'Present'}`;
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Briefcase className="w-4 h-4 text-[#EF4444]" />
          <h3 className="text-sm font-bold uppercase tracking-wider text-muted">
            Work Experience ({experience.length})
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
            Add Experience
          </Button>
        )}
      </div>

      {experience.length === 0 ? (
        <div className="p-6 rounded-2xl bg-surface border border-border/60 text-center space-y-2">
          <Briefcase className="w-6 h-6 text-muted mx-auto" />
          <p className="text-xs text-muted">No professional experience records added yet.</p>
          {editable && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleOpenAddModal}
              className="text-xs mt-2"
            >
              Add Work Experience
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {experience.map((item) => (
            <div
              key={item.id}
              className="p-5 rounded-2xl bg-surface border border-border hover:border-border/80 transition-all space-y-3 group"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h4 className="text-sm font-bold text-foreground">
                      {item.jobTitle}
                    </h4>
                    {item.currentlyWorking && (
                      <Badge variant="success" size="sm">
                        Current Role
                      </Badge>
                    )}
                    {item.employmentType && (
                      <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-card text-muted border border-border">
                        {EMPLOYMENT_TYPE_LABELS[item.employmentType] || item.employmentType}
                      </span>
                    )}
                  </div>

                  <p className="text-xs font-medium text-foreground/85">
                    {item.companyName}
                  </p>

                  <div className="flex flex-wrap items-center gap-4 text-xs text-muted pt-0.5">
                    {item.location && (
                      <div className="flex items-center gap-1.5">
                        <MapPin className="w-3.5 h-3.5" />
                        <span>{item.location}</span>
                      </div>
                    )}
                    {(item.startDate || item.endDate || item.currentlyWorking) && (
                      <div className="flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5" />
                        <span>{formatDateRange(item.startDate, item.endDate, item.currentlyWorking)}</span>
                      </div>
                    )}
                  </div>
                </div>

                {editable && (
                  <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity shrink-0">
                    <button
                      type="button"
                      onClick={() => handleOpenEditModal(item)}
                      className="p-1.5 rounded-lg text-muted hover:text-foreground hover:bg-card transition-colors cursor-pointer"
                      title="Edit experience"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDelete(item.id)}
                      className="p-1.5 rounded-lg text-muted hover:text-red-400 hover:bg-red-500/10 transition-colors cursor-pointer"
                      title="Delete experience"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
              </div>

              {item.description && (
                <p className="text-xs text-muted leading-relaxed whitespace-pre-wrap pt-1 border-t border-border/40">
                  {item.description}
                </p>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Add / Edit Experience Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingItem ? 'Edit Work Experience' : 'Add Work Experience'}
        description="Share your roles, companies, responsibilities, and engineering impact."
        maxWidth="md"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {errorMsg && (
            <div className="flex items-center gap-2 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-xs text-red-400">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground">
                Job Title <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                maxLength={150}
                placeholder="e.g. Senior Frontend Engineer"
                value={jobTitle}
                onChange={(e) => setJobTitle(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444]"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground">
                Company Name <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <Building className="w-4 h-4 text-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  maxLength={200}
                  placeholder="e.g. Google, Stripe, Acme Corp..."
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  className="w-full pl-10 pr-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444]"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground">
                Employment Type
              </label>
              <select
                value={employmentType}
                onChange={(e) => setEmploymentType(e.target.value as EmploymentType)}
                className="w-full px-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground focus:outline-none focus:border-[#EF4444]"
              >
                {(Object.keys(EMPLOYMENT_TYPE_LABELS) as EmploymentType[]).map((type) => (
                  <option key={type} value={type}>
                    {EMPLOYMENT_TYPE_LABELS[type]}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground">
                Location
              </label>
              <div className="relative">
                <MapPin className="w-4 h-4 text-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  maxLength={150}
                  placeholder="e.g. Remote, San Francisco, CA"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  className="w-full pl-10 pr-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444]"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground">
                Start Date
              </label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground focus:outline-none focus:border-[#EF4444]"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground">
                End Date
              </label>
              <input
                type="date"
                disabled={currentlyWorking}
                value={currentlyWorking ? '' : endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground focus:outline-none focus:border-[#EF4444] disabled:opacity-40 disabled:cursor-not-allowed"
              />
            </div>
          </div>

          <label className="flex items-center gap-2 cursor-pointer select-none pt-1">
            <input
              type="checkbox"
              checked={currentlyWorking}
              onChange={(e) => {
                setCurrentlyWorking(e.target.checked);
                if (e.target.checked) setEndDate('');
              }}
              className="rounded border-border text-[#EF4444] focus:ring-[#EF4444] focus:ring-offset-0 bg-surface"
            />
            <span className="text-xs text-foreground">
              I currently work in this role
            </span>
          </label>

          <div className="space-y-1.5">
            <div className="flex justify-between text-xs">
              <label className="font-medium text-foreground">
                Description / Key Responsibilities
              </label>
              <span className="text-muted">{description.length}/2000</span>
            </div>
            <textarea
              rows={3}
              maxLength={2000}
              placeholder="e.g. Architected microservices with Spring Boot, built user interfaces with Next.js, optimized SQL queries..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full p-3.5 rounded-xl bg-surface border border-border text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444] resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-border">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={isSubmitting}
              disabled={isSubmitting}
            >
              {editingItem ? 'Save Changes' : 'Add Experience'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
