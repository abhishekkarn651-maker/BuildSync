'use client';

import React, { useState } from 'react';
import {
  GraduationCap,
  Plus,
  Trash2,
  Edit2,
  Calendar,
  AlertCircle,
  Building2,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Badge } from '@/components/ui/Badge';
import { educationService } from '@/services/education.service';
import { ApiError } from '@/services/api-client';
import { Education, EducationPayload } from '@/types/profile.types';

interface EducationSectionProps {
  education: Education[];
  editable?: boolean;
  onEducationChange?: (updated: Education[]) => void;
}

export function EducationSection({
  education,
  editable = false,
  onEducationChange,
}: EducationSectionProps) {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<Education | null>(null);

  // Form states
  const [institution, setInstitution] = useState('');
  const [degree, setDegree] = useState('');
  const [fieldOfStudy, setFieldOfStudy] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [currentlyStudying, setCurrentlyStudying] = useState(false);
  const [description, setDescription] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleOpenAddModal = () => {
    setEditingItem(null);
    setInstitution('');
    setDegree('');
    setFieldOfStudy('');
    setStartDate('');
    setEndDate('');
    setCurrentlyStudying(false);
    setDescription('');
    setErrorMsg('');
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (item: Education) => {
    setEditingItem(item);
    setInstitution(item.institution || '');
    setDegree(item.degree || '');
    setFieldOfStudy(item.fieldOfStudy || '');
    setStartDate(item.startDate || '');
    setEndDate(item.endDate || '');
    setCurrentlyStudying(item.currentlyStudying || false);
    setDescription(item.description || '');
    setErrorMsg('');
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!institution.trim()) {
      setErrorMsg('Institution name is required.');
      return;
    }

    if (startDate && endDate && !currentlyStudying && endDate < startDate) {
      setErrorMsg('End date cannot precede start date.');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg('');

    const payload: EducationPayload = {
      institution: institution.trim(),
      degree: degree.trim() || undefined,
      fieldOfStudy: fieldOfStudy.trim() || undefined,
      startDate: startDate || null,
      endDate: currentlyStudying ? null : endDate || null,
      currentlyStudying,
      description: description.trim() || undefined,
    };

    try {
      if (editingItem) {
        const updated = await educationService.updateEducation(editingItem.id, payload);
        const newList = education.map((item) => (item.id === updated.id ? updated : item));
        onEducationChange?.(newList);
      } else {
        const created = await educationService.addEducation(payload);
        const newList = [created, ...education];
        onEducationChange?.(newList);
      }
      setIsModalOpen(false);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message || 'Failed to save education record');
      } else {
        setErrorMsg('An unexpected error occurred. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this education entry?')) return;

    try {
      await educationService.deleteEducation(id);
      const newList = education.filter((item) => item.id !== id);
      onEducationChange?.(newList);
    } catch (err: unknown) {
      alert(err instanceof ApiError ? err.message : 'Failed to delete education entry');
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
          <GraduationCap className="w-4 h-4 text-[#EF4444]" />
          <h3 className="text-sm font-bold uppercase tracking-wider text-muted">
            Education History ({education.length})
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
            Add Education
          </Button>
        )}
      </div>

      {education.length === 0 ? (
        <div className="p-6 rounded-2xl bg-surface border border-border/60 text-center space-y-2">
          <GraduationCap className="w-6 h-6 text-muted mx-auto" />
          <p className="text-xs text-muted">No academic education records added yet.</p>
          {editable && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleOpenAddModal}
              className="text-xs mt-2"
            >
              Add Education Details
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {education.map((item) => (
            <div
              key={item.id}
              className="p-5 rounded-2xl bg-surface border border-border hover:border-border/80 transition-all space-y-3 group"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h4 className="text-sm font-bold text-foreground">
                      {item.institution}
                    </h4>
                    {item.currentlyStudying && (
                      <Badge variant="success" size="sm">
                        Currently Studying
                      </Badge>
                    )}
                  </div>

                  {(item.degree || item.fieldOfStudy) && (
                    <p className="text-xs font-medium text-foreground/85">
                      {[item.degree, item.fieldOfStudy].filter(Boolean).join(' in ')}
                    </p>
                  )}

                  {(item.startDate || item.endDate || item.currentlyStudying) && (
                    <div className="flex items-center gap-1.5 text-xs text-muted pt-0.5">
                      <Calendar className="w-3.5 h-3.5" />
                      <span>{formatDateRange(item.startDate, item.endDate, item.currentlyStudying)}</span>
                    </div>
                  )}
                </div>

                {editable && (
                  <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity shrink-0">
                    <button
                      type="button"
                      onClick={() => handleOpenEditModal(item)}
                      className="p-1.5 rounded-lg text-muted hover:text-foreground hover:bg-card transition-colors cursor-pointer"
                      title="Edit education"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDelete(item.id)}
                      className="p-1.5 rounded-lg text-muted hover:text-red-400 hover:bg-red-500/10 transition-colors cursor-pointer"
                      title="Delete education"
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

      {/* Add / Edit Education Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingItem ? 'Edit Education Entry' : 'Add Education Entry'}
        description="Provide details regarding your academic degrees, universities, or institutions."
        maxWidth="md"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {errorMsg && (
            <div className="flex items-center gap-2 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-xs text-red-400">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-foreground">
              Institution / University <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <Building2 className="w-4 h-4 text-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                maxLength={200}
                placeholder="e.g. Stanford University, MIT..."
                value={institution}
                onChange={(e) => setInstitution(e.target.value)}
                className="w-full pl-10 pr-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444]"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground">
                Degree
              </label>
              <input
                type="text"
                maxLength={150}
                placeholder="e.g. Bachelor of Science, M.S., B.Tech"
                value={degree}
                onChange={(e) => setDegree(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444]"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground">
                Field of Study
              </label>
              <input
                type="text"
                maxLength={150}
                placeholder="e.g. Computer Science, Software Engineering"
                value={fieldOfStudy}
                onChange={(e) => setFieldOfStudy(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444]"
              />
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
                disabled={currentlyStudying}
                value={currentlyStudying ? '' : endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-surface border border-border text-xs text-foreground focus:outline-none focus:border-[#EF4444] disabled:opacity-40 disabled:cursor-not-allowed"
              />
            </div>
          </div>

          <label className="flex items-center gap-2 cursor-pointer select-none pt-1">
            <input
              type="checkbox"
              checked={currentlyStudying}
              onChange={(e) => {
                setCurrentlyStudying(e.target.checked);
                if (e.target.checked) setEndDate('');
              }}
              className="rounded border-border text-[#EF4444] focus:ring-[#EF4444] focus:ring-offset-0 bg-surface"
            />
            <span className="text-xs text-foreground">
              I am currently enrolled / studying here
            </span>
          </label>

          <div className="space-y-1.5">
            <div className="flex justify-between text-xs">
              <label className="font-medium text-foreground">
                Description / Notable Achievements
              </label>
              <span className="text-muted">{description.length}/1000</span>
            </div>
            <textarea
              rows={3}
              maxLength={1000}
              placeholder="e.g. Relevant coursework, honors, research papers or capstone project..."
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
              {editingItem ? 'Save Changes' : 'Add Education'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
