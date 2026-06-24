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
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'react-toastify';
import { userApi } from '../api/services';
import DataTable, { Column } from '../components/common/DataTable';
import { extractApiError, sanitizeFormPayload } from '../utils/apiErrors';
import type { User, Role } from '../types';

type UserForm = {
  fullName: string;
  email: string;
  password?: string;
  role: Role;
  department?: string;
  employeeId?: string;
  phone?: string;
};

const defaultForm: UserForm = {
  fullName: '',
  email: '',
  password: '',
  role: 'USER',
  department: '',
  employeeId: '',
  phone: '',
};

export default function UsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [open, setOpen] = useState(false);
  const [editUser, setEditUser] = useState<User | null>(null);
  const { register, handleSubmit, reset, control } = useForm<UserForm>({ defaultValues: defaultForm });

  const { data, isLoading } = useQuery({
    queryKey: ['users', page, size],
    queryFn: () => userApi.getAll({ page, size }),
  });

  const users = data?.data?.data?.content ?? [];
  const total = data?.data?.data?.totalElements ?? 0;

  const saveMutation = useMutation({
    mutationFn: (form: UserForm) =>
      editUser ? userApi.update(editUser.id, form) : userApi.create(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success(editUser ? 'User updated' : 'User created');
      setOpen(false);
      setEditUser(null);
      reset(defaultForm);
    },
    onError: (error) => toast.error(extractApiError(error, 'Failed to save user')),
  });

  const toggleStatus = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      active ? userApi.activate(id) : userApi.deactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Status updated');
    },
    onError: (error) => toast.error(extractApiError(error, 'Failed to update status')),
  });

  const openCreate = () => {
    setEditUser(null);
    reset(defaultForm);
    setOpen(true);
  };

  const openEdit = (user: User) => {
    setEditUser(user);
    reset({
      fullName: user.fullName,
      email: user.email,
      role: user.role,
      department: user.department || '',
      employeeId: user.employeeId || '',
      phone: user.phone || '',
    });
    setOpen(true);
  };

  const onSubmit = (form: UserForm) => {
    if (!editUser && !form.password?.trim()) {
      toast.error('Password is required for new users');
      return;
    }
    const payload = sanitizeFormPayload({
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      role: form.role,
      department: form.department?.trim(),
      employeeId: form.employeeId?.trim(),
      phone: form.phone?.trim(),
      ...(form.password?.trim() ? { password: form.password.trim() } : {}),
    });
    saveMutation.mutate(payload as UserForm);
  };

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
          <IconButton size="small" onClick={() => openEdit(row)}>
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
        <Button startIcon={<Add />} variant="contained" onClick={openCreate}>
          Add User
        </Button>
      </Box>

      <DataTable columns={columns} rows={users} loading={isLoading} page={page} size={size}
        total={total} onPageChange={setPage} onSizeChange={setSize} getRowId={(r) => r.id} />

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editUser ? 'Edit User' : 'Add User'}</DialogTitle>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12}>
                <TextField fullWidth label="Full Name" required {...register('fullName', { required: true })} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="Email" type="email" required {...register('email', { required: true })} />
              </Grid>
              {!editUser && (
                <Grid item xs={12}>
                  <TextField fullWidth label="Password" type="password" required {...register('password', { required: true })} />
                </Grid>
              )}
              <Grid item xs={12}>
                <Controller
                  name="role"
                  control={control}
                  rules={{ required: true }}
                  render={({ field }) => (
                    <TextField fullWidth select label="Role" required {...field}>
                      {(['ADMIN', 'USER'] as Role[]).map((r) => (
                        <MenuItem key={r} value={r}>{r}</MenuItem>
                      ))}
                    </TextField>
                  )}
                />
              </Grid>
              <Grid item xs={12}><TextField fullWidth label="Department" {...register('department')} /></Grid>
              <Grid item xs={12}><TextField fullWidth label="Employee ID" {...register('employeeId')} /></Grid>
              <Grid item xs={12}><TextField fullWidth label="Phone" {...register('phone')} /></Grid>
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
    </Box>
  );
}
