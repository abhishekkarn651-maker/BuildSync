'use client';

import React, { useState, useRef } from 'react';
import Image from 'next/image';
import { Camera, Trash2, Upload, Loader2, AlertCircle, CheckCircle2, X } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/ui/Avatar';
import { Modal } from '@/components/ui/Modal';
import { profileService } from '@/services/profile.service';
import { DeveloperProfile } from '@/types/profile.types';
import { getAvatarUrl } from '@/config/api.config';
import { ApiError } from '@/services/api-client';

interface AvatarUploadProps {
  currentAvatarUrl?: string | null;
  fullName?: string;
  username: string;
  onAvatarUpdated: (profile: DeveloperProfile) => void;
  className?: string;
}

export function AvatarUpload({
  currentAvatarUrl,
  fullName,
  username,
  onAvatarUpdated,
  className = '',
}: AvatarUploadProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const displayName = fullName || username;
  const resolvedAvatarUrl = previewUrl || getAvatarUrl(currentAvatarUrl);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setErrorMsg('');
    setSuccessMsg('');

    const files = e.target.files;
    if (!files || files.length === 0) return;

    const file = files[0];

    // Client-side MIME type check
    if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
      setErrorMsg('Only JPEG and PNG image files are supported.');
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    // Client-side size check (5 MB)
    const MAX_SIZE = 5 * 1024 * 1024;
    if (file.size > MAX_SIZE) {
      setErrorMsg('Image size exceeds the maximum limit of 5 MB.');
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    setSelectedFile(file);
    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
  };

  const handleCancelPreview = () => {
    if (previewUrl && previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrl);
    }
    setSelectedFile(null);
    setPreviewUrl(null);
    setErrorMsg('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    setIsUploading(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const updatedProfile = await profileService.uploadAvatar(selectedFile);
      onAvatarUpdated(updatedProfile);
      setSuccessMsg('Avatar updated successfully!');
      handleCancelPreview();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message || 'Failed to upload avatar.');
      } else if (err instanceof Error) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg('An unexpected error occurred while uploading.');
      }
    } finally {
      setIsUploading(false);
    }
  };

  const handleDelete = async () => {
    setIsDeleting(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const updatedProfile = await profileService.deleteAvatar();
      onAvatarUpdated(updatedProfile);
      setSuccessMsg('Avatar removed successfully.');
      setIsDeleteModalOpen(false);
      handleCancelPreview();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message || 'Failed to remove avatar.');
      } else if (err instanceof Error) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg('An unexpected error occurred while removing avatar.');
      }
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Alert Messages */}
      {errorMsg && (
        <div className="flex items-center gap-2 p-3 rounded-lg text-xs font-medium bg-red-500/10 border border-red-500/30 text-[#EF4444] animate-in fade-in">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span className="flex-1">{errorMsg}</span>
          <button
            type="button"
            onClick={() => setErrorMsg('')}
            className="p-1 hover:text-red-400 transition-colors"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {successMsg && (
        <div className="flex items-center gap-2 p-3 rounded-lg text-xs font-medium bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span className="flex-1">{successMsg}</span>
          <button
            type="button"
            onClick={() => setSuccessMsg('')}
            className="p-1 hover:text-emerald-300 transition-colors"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Main Avatar Presentation & Controls */}
      <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5">
        {/* Avatar Display */}
        <div className="relative group shrink-0">
          <div className="relative w-24 h-24 sm:w-28 sm:h-28 rounded-full overflow-hidden border-2 border-border group-hover:border-[#EF4444] transition-colors shadow-lg shadow-black/40 bg-surface flex items-center justify-center">
            {resolvedAvatarUrl ? (
              <Image
                src={resolvedAvatarUrl}
                alt={displayName}
                width={112}
                height={112}
                className="w-full h-full object-cover"
                unoptimized
              />
            ) : (
              <Avatar name={displayName} size="xl" className="w-full h-full text-2xl font-bold" />
            )}

            {/* Upload Overlay on Hover */}
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploading}
              className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 flex flex-col items-center justify-center gap-1 text-white transition-opacity cursor-pointer disabled:pointer-events-none"
              title="Upload new photo"
              aria-label="Upload new photo"
            >
              {isUploading ? (
                <Loader2 className="w-6 h-6 animate-spin text-[#EF4444]" />
              ) : (
                <>
                  <Camera className="w-6 h-6 text-white" />
                  <span className="text-[10px] font-semibold uppercase tracking-wider">Change</span>
                </>
              )}
            </button>
          </div>

          {/* Quick upload badge icon */}
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
            className="absolute bottom-0 right-0 p-2 rounded-full bg-[#EF4444] text-white hover:bg-red-600 border-2 border-card shadow-md transition-transform hover:scale-110 active:scale-95 cursor-pointer disabled:pointer-events-none"
            title="Upload photo"
            aria-label="Upload photo"
          >
            <Camera className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Action Controls & Guidelines */}
        <div className="space-y-3 text-center sm:text-left flex-1">
          <div>
            <h4 className="text-sm font-bold text-foreground">Profile Avatar</h4>
            <p className="text-xs text-muted mt-0.5">
              JPG or PNG, up to 5 MB. Minimum 32x32 px.
            </p>
          </div>

          {/* Hidden File Input */}
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png"
            onChange={handleFileChange}
            className="hidden"
          />

          {/* Controls */}
          {selectedFile ? (
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-1">
              <Button
                type="button"
                variant="primary"
                size="sm"
                onClick={handleUpload}
                disabled={isUploading}
                leftIcon={isUploading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Upload className="w-3.5 h-3.5" />}
              >
                {isUploading ? 'Uploading...' : 'Save New Avatar'}
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleCancelPreview}
                disabled={isUploading}
              >
                Cancel
              </Button>
            </div>
          ) : (
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-1">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
                leftIcon={<Upload className="w-3.5 h-3.5" />}
              >
                Upload Photo
              </Button>

              {currentAvatarUrl && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => setIsDeleteModalOpen(true)}
                  className="text-red-400 hover:text-red-300 hover:bg-red-500/10"
                  leftIcon={<Trash2 className="w-3.5 h-3.5" />}
                >
                  Remove
                </Button>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={isDeleteModalOpen}
        onClose={() => !isDeleting && setIsDeleteModalOpen(false)}
        title="Remove Profile Avatar"
      >
        <div className="space-y-4">
          <p className="text-sm text-muted">
            Are you sure you want to remove your profile photo? Your profile will revert to displaying your default initials avatar.
          </p>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsDeleteModalOpen(false)}
              disabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="danger"
              size="sm"
              onClick={handleDelete}
              disabled={isDeleting}
              leftIcon={isDeleting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Trash2 className="w-3.5 h-3.5" />}
            >
              {isDeleting ? 'Removing...' : 'Remove Photo'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
