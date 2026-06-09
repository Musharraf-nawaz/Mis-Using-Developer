import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  TextField,
  Button,
  Autocomplete,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'react-toastify';
import { assetApi, userApi } from '../api/services';
import type { Asset, User } from '../types';

interface AssignmentForm {
  assetId: number;
  employeeId: number;
  employeeName: string;
  employeeDepartment: string;
  assignedDate: string;
  expectedReturnDate: string;
  remarks: string;
}

export default function AssetAssignmentPage() {
  const queryClient = useQueryClient();
  const { control, handleSubmit, register } = useForm<AssignmentForm>({
    defaultValues: { assignedDate: new Date().toISOString().split('T')[0] },
  });

  const { data: assetsData } = useQuery({
    queryKey: ['assets-available'],
    queryFn: () => assetApi.getAll({ status: 'AVAILABLE', size: 100 }),
  });

  const { data: usersData } = useQuery({
    queryKey: ['users-employees'],
    queryFn: () => userApi.getAll({ role: 'USER', status: 'ACTIVE', size: 100 }),
  });

  const availableAssets = assetsData?.data?.data?.content ?? [];
  const employees = usersData?.data?.data?.content ?? [];

  const assignMutation = useMutation({
    mutationFn: (data: AssignmentForm) =>
      assetApi.assign(data as unknown as Record<string, unknown>),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      toast.success('Asset assigned successfully');
    },
    onError: () => toast.error('Assignment failed'),
  });

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} gutterBottom>
        Asset Assignment
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Assign company assets to employees
      </Typography>

      <Card>
        <CardContent>
          <form onSubmit={handleSubmit((d) => assignMutation.mutate(d))}>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Controller
                  name="assetId"
                  control={control}
                  rules={{ required: true }}
                  render={({ field }) => (
                    <Autocomplete
                      options={availableAssets}
                      getOptionLabel={(a: Asset) => `${a.assetTag} - ${a.assetName}`}
                      onChange={(_, v) => field.onChange(v?.id)}
                      renderInput={(params) => <TextField {...params} label="Select Asset" required />}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Controller
                  name="employeeId"
                  control={control}
                  rules={{ required: true }}
                  render={({ field }) => (
                    <Autocomplete
                      options={employees}
                      getOptionLabel={(u: User) => `${u.employeeId} - ${u.fullName}`}
                      onChange={(_, v) => field.onChange(v?.id)}
                      renderInput={(params) => <TextField {...params} label="Select Employee" required />}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Employee Name" {...register('employeeName')} />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Department" {...register('employeeDepartment')} />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Assigned Date" type="date" InputLabelProps={{ shrink: true }}
                  {...register('assignedDate', { required: true })} />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth label="Expected Return Date" type="date" InputLabelProps={{ shrink: true }}
                  {...register('expectedReturnDate')} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="Remarks" multiline rows={2} {...register('remarks')} />
              </Grid>
              <Grid item xs={12}>
                <Button type="submit" variant="contained" disabled={assignMutation.isPending}>
                  Assign Asset
                </Button>
              </Grid>
            </Grid>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}
