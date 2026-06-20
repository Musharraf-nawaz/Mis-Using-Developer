import { useState, useEffect } from 'react';
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
  Autocomplete,
} from '@mui/material';
import { Add, Edit, Delete, Search } from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'react-toastify';
import { projectApi, userApi } from '../api/services';
import { useAuth } from '../context/AuthContext';
import DataTable, { Column } from '../components/common/DataTable';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import type { Project, ProjectStatus, User } from '../types';

const STATUS_COLORS: Record<ProjectStatus, 'success' | 'primary' | 'warning' | 'error'> = {
  ACTIVE: 'success',
  COMPLETED: 'primary',
  ON_HOLD: 'warning',
  CANCELLED: 'error',
};

export default function ProjectsPage() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole('ADMIN');
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch]);
  const [open, setOpen] = useState(false);
  const [editProject, setEditProject] = useState<Project | null>(null);
  const { register, handleSubmit, reset, control } = useForm<Partial<Project>>();

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['projects', page, size, debouncedSearch],
    queryFn: () => projectApi.getAll({ page, size, search: debouncedSearch || undefined }),
    placeholderData: keepPreviousData,
  });

  const { data: usersData } = useQuery({
    queryKey: ['users-for-projects'],
    queryFn: () => userApi.getAll({ role: 'USER', status: 'ACTIVE', size: 100 }),
    enabled: isAdmin,
  });

  const users = usersData?.data?.data?.content ?? [];
  const projects = data?.data?.data?.content ?? [];
  const total = data?.data?.data?.totalElements ?? 0;

  const saveMutation = useMutation({
    mutationFn: (form: Partial<Project>) =>
      editProject ? projectApi.update(editProject.id, form) : projectApi.create(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast.success(editProject ? 'Project updated' : 'Project created');
      setOpen(false);
      setEditProject(null);
      reset();
    },
    onError: () => toast.error('Operation failed'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => projectApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      toast.success('Project deleted');
    },
  });

  const openCreate = () => {
    setEditProject(null);
    reset({ status: 'ACTIVE', candidateWorkingCount: 0, interviewCandidateCount: 0, onboardedCandidateCount: 0 });
    setOpen(true);
  };

  const openEdit = (project: Project) => {
    setEditProject(project);
    reset(project);
    setOpen(true);
  };

  const columns: Column<Project>[] = [
    { id: 'projectName', label: 'Project Name', minWidth: 160 },
    { id: 'clientName', label: 'Client', minWidth: 120 },
    { id: 'midClientName', label: 'Mid Client', minWidth: 120 },
    { id: 'candidateWorkingCount', label: 'Working', minWidth: 80 },
    { id: 'onboardedCandidateCount', label: 'Onboarded', minWidth: 90 },
    {
      id: 'status',
      label: 'Status',
      minWidth: 100,
      render: (row) => <Chip label={row.status} size="small" color={STATUS_COLORS[row.status]} />,
    },
    ...(isAdmin
      ? [{
          id: 'budget',
          label: 'Budget',
          minWidth: 100,
          render: (row: Project) => (row.budget != null ? `$${Number(row.budget).toLocaleString()}` : '—'),
        }]
      : []),
    { id: 'remarks', label: 'Remarks', minWidth: 160 },
    ...(isAdmin
      ? [{
          id: 'actions',
          label: 'Actions',
          minWidth: 100,
          render: (row: Project) => (
            <Box>
              <IconButton size="small" onClick={() => openEdit(row)}>
                <Edit fontSize="small" />
              </IconButton>
              <IconButton size="small" color="error" onClick={() => deleteMutation.mutate(row.id)}>
                <Delete fontSize="small" />
              </IconButton>
            </Box>
          ),
        }]
      : []),
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>
            Project Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {isAdmin ? 'Create and manage projects' : 'View your assigned projects (read-only)'}
          </Typography>
        </Box>
        {isAdmin && (
          <Button variant="contained" startIcon={<Add />} onClick={openCreate}>
            New Project
          </Button>
        )}
      </Box>

      <TextField
        size="small"
        placeholder="Search projects..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        InputProps={{ startAdornment: <Search sx={{ mr: 1, color: 'text.secondary' }} /> }}
        sx={{ mb: 2, width: 300 }}
      />

      <DataTable
        columns={columns}
        rows={projects}
        loading={isLoading}
        fetching={isFetching}
        page={page}
        size={size}
        total={total}
        onPageChange={setPage}
        onSizeChange={setSize}
        getRowId={(r) => r.id}
      />

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editProject ? 'Edit Project' : 'New Project'}</DialogTitle>
        <form onSubmit={handleSubmit((form) => saveMutation.mutate(form))}>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Project Name" required {...register('projectName', { required: true })} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Client Name" required {...register('clientName', { required: true })} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Mid Client Name" {...register('midClientName')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth select label="Status" defaultValue="ACTIVE" {...register('status')}>
                  {(['ACTIVE', 'COMPLETED', 'ON_HOLD', 'CANCELLED'] as ProjectStatus[]).map((s) => (
                    <MenuItem key={s} value={s}>{s}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth type="number" label="Working Candidates" {...register('candidateWorkingCount')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth type="number" label="Interview Candidates" {...register('interviewCandidateCount')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth type="number" label="Onboarded Candidates" {...register('onboardedCandidateCount')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth type="date" label="Start Date" InputLabelProps={{ shrink: true }} {...register('startDate')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth type="date" label="End Date" InputLabelProps={{ shrink: true }} {...register('endDate')} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth type="number" label="Budget" {...register('budget')} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth multiline rows={2} label="Remarks" {...register('remarks')} />
              </Grid>
              <Grid item xs={12}>
                <Controller
                  name="assignedUserIds"
                  control={control}
                  render={({ field }) => (
                    <Autocomplete
                      multiple
                      options={users}
                      getOptionLabel={(u: User) => u.fullName}
                      value={users.filter((u) => (field.value as number[] | undefined)?.includes(u.id))}
                      onChange={(_, val) => field.onChange(val.map((u) => u.id))}
                      renderInput={(params) => <TextField {...params} label="Assign Users" />}
                    />
                  )}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={saveMutation.isPending}>
              Save
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
