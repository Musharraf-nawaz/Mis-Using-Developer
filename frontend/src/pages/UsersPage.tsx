import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Grid,
  MenuItem,
  IconButton,
  Chip,
} from '@mui/material';
import { Add, Edit, Block, CheckCircle } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { userApi } from '../api/services';
import DataTable, { Column } from '../components/common/DataTable';
import type { User, Role } from '../types';

export default function UsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [open, setOpen] = useState(false);
  const [editUser, setEditUser] = useState<User | null>(null);
  const { register, handleSubmit, reset } = useForm<Partial<User> & { password?: string }>();

  const { data, isLoading } = useQuery({
    queryKey: ['users', page, size],
    queryFn: () => userApi.getAll({ page, size }),
  });

  const users = data?.data?.data?.content ?? [];
  const total = data?.data?.data?.totalElements ?? 0;

  const saveMutation = useMutation({
    mutationFn: (form: Partial<User> & { password?: string }) =>
      editUser ? userApi.update(editUser.id, form) : userApi.create(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success(editUser ? 'User updated' : 'User created');
      setOpen(false);
      reset();
    },
  });

  const toggleStatus = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      active ? userApi.activate(id) : userApi.deactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Status updated');
    },
  });

  const columns: Column<User>[] = [
    { id: 'employeeId', label: 'Employee ID' },
    { id: 'fullName', label: 'Name', minWidth: 150 },
    { id: 'email', label: 'Email', minWidth: 180 },
    { id: 'role', label: 'Role' },
    { id: 'department', label: 'Department' },
    {
      id: 'status',
      label: 'Status',
      render: (row) => (
        <Chip label={row.status} size="small" color={row.status === 'ACTIVE' ? 'success' : 'default'} />
      ),
    },
    {
      id: 'actions',
      label: 'Actions',
      render: (row) => (
        <Box>
          <IconButton size="small" onClick={() => { setEditUser(row); setOpen(true); }}>
            <Edit fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            color={row.status === 'ACTIVE' ? 'error' : 'success'}
            onClick={() => toggleStatus.mutate({ id: row.id, active: row.status !== 'ACTIVE' })}
          >
            {row.status === 'ACTIVE' ? <Block fontSize="small" /> : <CheckCircle fontSize="small" />}
          </IconButton>
        </Box>
      ),
    },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>User Management</Typography>
          <Typography variant="body2" color="text.secondary">Manage system users and roles</Typography>
        </Box>
        <Button startIcon={<Add />} variant="contained" onClick={() => { setEditUser(null); reset(); setOpen(true); }}>
          Add User
        </Button>
      </Box>

      <DataTable columns={columns} rows={users} loading={isLoading} page={page} size={size}
        total={total} onPageChange={setPage} onSizeChange={setSize} getRowId={(r) => r.id} />

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editUser ? 'Edit User' : 'Add User'}</DialogTitle>
        <form onSubmit={handleSubmit((d) => saveMutation.mutate(d))}>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12}><TextField fullWidth label="Full Name" {...register('fullName', { required: true })} /></Grid>
              <Grid item xs={12}><TextField fullWidth label="Email" {...register('email', { required: true })} /></Grid>
              {!editUser && <Grid item xs={12}><TextField fullWidth label="Password" type="password" {...register('password')} /></Grid>}
              <Grid item xs={12}>
                <TextField fullWidth select label="Role" defaultValue="USER" {...register('role')}>
                  {(['ADMIN', 'USER'] as Role[]).map((r) => (
                    <MenuItem key={r} value={r}>{r}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12}><TextField fullWidth label="Department" {...register('department')} /></Grid>
              <Grid item xs={12}><TextField fullWidth label="Employee ID" {...register('employeeId')} /></Grid>
              <Grid item xs={12}><TextField fullWidth label="Phone" {...register('phone')} /></Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained">Save</Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
