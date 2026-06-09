import { useState } from 'react';
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Button,
  TextField,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
  IconButton,
  Chip,
  Switch,
  FormControlLabel,
} from '@mui/material';
import { Add, Edit, Cancel, CheckCircle, Timeline } from '@mui/icons-material';
import InterviewStepper from '../components/interviews/InterviewStepper';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { interviewApi, fileUrl } from '../api/services';
import { useAuth } from '../context/AuthContext';
import DataTable, { Column } from '../components/common/DataTable';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import type { ApiResponse, Interview, InterviewStatus, PageResponse } from '../types';

const STATUS_COLORS: Record<InterviewStatus, 'primary' | 'success' | 'error' | 'warning'> = {
  SCHEDULED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'error',
  RESCHEDULED: 'warning',
};

export default function InterviewsPage() {
  const { hasRole } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search);
  const [open, setOpen] = useState(false);
  const [rescheduleOpen, setRescheduleOpen] = useState(false);
  const [stepperOpen, setStepperOpen] = useState(false);
  const [stepperInterview, setStepperInterview] = useState<Interview | null>(null);
  const [selectedInterviewId, setSelectedInterviewId] = useState<number | null>(null);
  const [editInterview, setEditInterview] = useState<Interview | null>(null);
  const [cvFile, setCvFile] = useState<File | null>(null);
  const { register, handleSubmit, reset, setValue } = useForm<Partial<Interview>>();
  const {
    register: registerReschedule,
    handleSubmit: handleRescheduleSubmit,
    reset: resetReschedule,
  } = useForm<{ interviewDate: string; interviewTime: string }>();

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['interviews', page, size, debouncedSearch],
    queryFn: () => interviewApi.getAll({ page, size, search: debouncedSearch || undefined }),
    placeholderData: keepPreviousData,
  });

  const interviews = data?.data?.data?.content ?? [];
  const total = data?.data?.data?.totalElements ?? 0;

  const saveMutation = useMutation({
    mutationFn: async (form: Partial<Interview>) => {
      const res = editInterview
        ? await interviewApi.update(editInterview.id, form)
        : await interviewApi.create(form);
      const interviewId = res.data.data.id;
      if (cvFile) {
        await interviewApi.uploadCv(interviewId, cvFile);
      }
      return res;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interviews'] });
      toast.success(editInterview ? 'Interview updated' : 'Interview scheduled');
      setOpen(false);
      setEditInterview(null);
      setCvFile(null);
      reset();
    },
    onError: () => toast.error('Failed to save interview'),
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => interviewApi.cancel(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['interviews'] });
      type InterviewsQueryData = { data: ApiResponse<PageResponse<Interview>> };
      const snapshots = queryClient.getQueriesData<InterviewsQueryData>({ queryKey: ['interviews'] });
      snapshots.forEach(([key, cached]) => {
        if (!cached?.data?.data?.content) return;
        queryClient.setQueryData(key, {
          ...cached,
          data: {
            ...cached.data,
            data: {
              ...cached.data.data,
              content: cached.data.data.content.map((item) =>
                item.id === id ? { ...item, interviewStatus: 'CANCELLED' as const } : item
              ),
            },
          },
        });
      });
      return { snapshots };
    },
    onSuccess: () => toast.success('Interview cancelled'),
    onError: (_err, _id, context) => {
      context?.snapshots?.forEach(([key, cached]) => queryClient.setQueryData(key, cached));
      toast.error('Cancel failed');
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['interviews'] }),
  });

  const rescheduleMutation = useMutation({
    mutationFn: ({ id, interviewDate, interviewTime }: { id: number; interviewDate: string; interviewTime: string }) =>
      interviewApi.reschedule(id, { interviewDate, interviewTime }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interviews'] });
      toast.success('Interview rescheduled');
      setRescheduleOpen(false);
      setSelectedInterviewId(null);
      resetReschedule();
    },
    onError: () => toast.error('Reschedule failed'),
  });

  const completeMutation = useMutation({
    mutationFn: (id: number) => interviewApi.complete(id, 'Completed successfully'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interviews'] });
      toast.success('Interview completed');
    },
  });

  const columns: Column<Interview>[] = [
    { id: 'candidateName', label: 'Candidate', minWidth: 140 },
    { id: 'clientName', label: 'Client' },
    { id: 'midClientName', label: 'Mid Client' },
    { id: 'candidateProfile', label: 'Profile' },
    { id: 'experience', label: 'Experience' },
    { id: 'interviewerName', label: 'Interviewer' },
    { id: 'interviewDate', label: 'Date' },
    { id: 'interviewTime', label: 'Time' },
    { id: 'interviewRound', label: 'Round' },
    {
      id: 'interviewStatus',
      label: 'Status',
      render: (row) => (
        <Chip label={row.interviewStatus} size="small" color={STATUS_COLORS[row.interviewStatus]} />
      ),
    },
    {
      id: 'actions',
      label: 'Actions',
      render: (row) => (
          <Box>
            <IconButton size="small" onClick={() => { setStepperInterview(row); setStepperOpen(true); }} title="View rounds">
              <Timeline fontSize="small" />
            </IconButton>
            {hasRole('ADMIN') && (
              <IconButton size="small" onClick={() => { setEditInterview(row); setCvFile(null); Object.entries(row).forEach(([k,v]) => setValue(k as keyof Interview, v)); setOpen(true); }}>
                <Edit fontSize="small" />
              </IconButton>
            )}
            {hasRole('ADMIN') && row.interviewStatus === 'SCHEDULED' && (
              <>
                <IconButton size="small" color="success" onClick={() => completeMutation.mutate(row.id)}>
                  <CheckCircle fontSize="small" />
                </IconButton>
                <IconButton size="small" color="error" onClick={() => cancelMutation.mutate(row.id)}>
                  <Cancel fontSize="small" />
                </IconButton>
              </>
            )}
          </Box>
        ),
    },
    {
      id: 'cancelSwitch',
      label: 'Cancelled',
      render: (row) =>
        hasRole('ADMIN') ? (
          <FormControlLabel
            sx={{ m: 0 }}
            control={
              <Switch
                size="small"
                checked={row.interviewStatus === 'CANCELLED'}
                onChange={(e) => {
                  if (e.target.checked && row.interviewStatus !== 'CANCELLED') {
                    cancelMutation.mutate(row.id);
                  }
                }}
              />
            }
            label=""
          />
        ) : null,
    },
    {
      id: 'rescheduleSwitch',
      label: 'Rescheduled',
      render: (row) =>
        hasRole('ADMIN') ? (
          <FormControlLabel
            sx={{ m: 0 }}
            control={
              <Switch
                size="small"
                checked={row.interviewStatus === 'RESCHEDULED'}
                onChange={(e) => {
                  if (e.target.checked) {
                    setSelectedInterviewId(row.id);
                    setRescheduleOpen(true);
                  }
                }}
              />
            }
            label=""
          />
        ) : null,
    },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>Interview Management</Typography>
          <Typography variant="body2" color="text.secondary">Schedule and track candidate interviews</Typography>
        </Box>
        {hasRole('ADMIN') && (
          <Button startIcon={<Add />} variant="contained" onClick={() => { setEditInterview(null); setCvFile(null); reset(); setOpen(true); }}>
            Schedule Interview
          </Button>
        )}
      </Box>

      <TextField size="small" placeholder="Search candidates..." value={search}
        onChange={(e) => setSearch(e.target.value)} sx={{ mb: 2, width: 300 }} />

      <DataTable columns={columns} rows={interviews} loading={isLoading} fetching={isFetching} page={page} size={size}
        total={total} onPageChange={setPage} onSizeChange={setSize} getRowId={(r) => r.id} />

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editInterview ? 'Edit Interview' : 'Schedule Interview'}</DialogTitle>
        <form onSubmit={handleSubmit((d) => {
          const derivedEmail = d.candidateEmail || `${(d.candidateName || 'candidate').replace(/\s+/g, '.').toLowerCase()}@example.com`;
          saveMutation.mutate({
            ...d,
            candidateEmail: derivedEmail,
            interviewerName: d.interviewerName || 'HR Panel',
            interviewMode: d.interviewMode || 'ONLINE',
          });
        })}>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Interview Candidate Name" {...register('candidateName')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Client Name" {...register('clientName')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Mid Client Name" {...register('midClientName')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Company To Represent" {...register('companyToRepresent')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Candidate Profile" {...register('candidateProfile')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Candidate Experience" {...register('experience')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Interview Link" {...register('interviewLink')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Interviewer" {...register('interviewerName')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Date" type="date" InputLabelProps={{ shrink: true }}
                  {...register('interviewDate', { required: true })} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Time" type="time" InputLabelProps={{ shrink: true }}
                  {...register('interviewTime', { required: true })} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Button variant="outlined" component="label" fullWidth sx={{ height: 56 }}>
                  {cvFile ? cvFile.name : 'Upload Candidate CV (PDF/Image)'}
                  <input
                    type="file"
                    hidden
                    accept=".pdf,image/*"
                    onChange={(e) => setCvFile(e.target.files?.[0] || null)}
                  />
                </Button>
              </Grid>
              {editInterview?.candidateCvUrl && (
                <Grid item xs={12} sm={6}>
                  <Button
                    variant="text"
                    fullWidth
                    sx={{ height: 56 }}
                    href={fileUrl(editInterview.candidateCvUrl)}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    View Current CV
                  </Button>
                </Grid>
              )}
              <Grid item xs={12} sm={6}>
                <TextField fullWidth select label="Round" defaultValue="SCREENING" {...register('interviewRound')}>
                  {[
                    { value: 'FIRST_ROUND', label: '1st Round' },
                    { value: 'SECOND_ROUND', label: '2nd Round' },
                    { value: 'THIRD_ROUND', label: '3rd Round' },
                    { value: 'FINAL_ROUND', label: 'Final Round' },
                    { value: 'SCREENING', label: 'Screening' },
                    { value: 'HR', label: 'HR' },
                  ].map((r) => (
                    <MenuItem key={r.value} value={r.value}>{r.label}</MenuItem>
                  ))}
                </TextField>
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving...' : 'Save'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      <Dialog open={rescheduleOpen} onClose={() => setRescheduleOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reschedule Interview</DialogTitle>
        <form
          onSubmit={handleRescheduleSubmit((d) => {
            if (!selectedInterviewId) return;
            rescheduleMutation.mutate({
              id: selectedInterviewId,
              interviewDate: d.interviewDate,
              interviewTime: d.interviewTime,
            });
          })}
        >
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Next Date"
                  type="date"
                  InputLabelProps={{ shrink: true }}
                  {...registerReschedule('interviewDate', { required: true })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Next Time"
                  type="time"
                  InputLabelProps={{ shrink: true }}
                  {...registerReschedule('interviewTime', { required: true })}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setRescheduleOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={rescheduleMutation.isPending}>
              {rescheduleMutation.isPending ? 'Updating...' : 'Update'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      <Dialog open={stepperOpen} onClose={() => setStepperOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Interview Progression</DialogTitle>
        <DialogContent>
          {stepperInterview && <InterviewStepper interview={stepperInterview} onClose={() => setStepperOpen(false)} />}
        </DialogContent>
      </Dialog>
    </Box>
  );
}
