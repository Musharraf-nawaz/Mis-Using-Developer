import { useState, useMemo, useEffect, useCallback } from 'react';
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
  InputAdornment,
} from '@mui/material';
import { Add, Edit, Delete, Search, Download } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { assetApi, fileUrl } from '../api/services';
import { useAuth } from '../context/AuthContext';
import DataTable, { Column } from '../components/common/DataTable';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import type { Asset, AssetStatus } from '../types';
import type { ApiResponse, PageResponse } from '../types';

const STATUS_COLORS: Record<AssetStatus, 'success' | 'warning' | 'info' | 'error' | 'default'> = {
  AVAILABLE: 'success',
  ASSIGNED: 'warning',
  RETURNED: 'info',
  DAMAGED: 'error',
  LOST: 'default',
};

export default function AssetsPage() {
  const { hasRole } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search);
  const [statusFilter, setStatusFilter] = useState('');

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, statusFilter]);
  const [open, setOpen] = useState(false);
  const [editAsset, setEditAsset] = useState<Asset | null>(null);
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [videoFile, setVideoFile] = useState<File | null>(null);
  const { register, handleSubmit, reset, setValue } = useForm<Partial<Asset>>();

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['assets', page, size, debouncedSearch, statusFilter],
    queryFn: () =>
      assetApi.getAll({ page, size, search: debouncedSearch || undefined, status: statusFilter || undefined }),
    placeholderData: keepPreviousData,
  });

  const assets = data?.data?.data?.content ?? [];
  const total = data?.data?.data?.totalElements ?? 0;

  const saveMutation = useMutation({
    mutationFn: async (form: Partial<Asset>) => {
      const res = editAsset ? await assetApi.update(editAsset.id, form) : await assetApi.create(form);
      const assetId = res.data.data.id;
      if (photoFile) await assetApi.uploadMedia(assetId, photoFile, 'PHOTO');
      if (videoFile) await assetApi.uploadMedia(assetId, videoFile, 'VIDEO');
      return res;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      toast.success(editAsset ? 'Asset updated' : 'Asset created');
      setOpen(false);
      setEditAsset(null);
      setPhotoFile(null);
      setVideoFile(null);
      reset();
    },
    onError: () => toast.error('Operation failed'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => assetApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      toast.success('Asset deleted');
    },
  });

  const offboardMutation = useMutation({
    mutationFn: ({ id, offboarded }: { id: number; offboarded: boolean }) =>
      assetApi.updateOffboarded(id, offboarded),
    onMutate: async ({ id, offboarded }) => {
      await queryClient.cancelQueries({ queryKey: ['assets'] });
      type AssetsQueryData = { data: ApiResponse<PageResponse<Asset>> };
      const snapshots = queryClient.getQueriesData<AssetsQueryData>({ queryKey: ['assets'] });
      snapshots.forEach(([key, cached]) => {
        if (!cached?.data?.data?.content) return;
        queryClient.setQueryData(key, {
          ...cached,
          data: {
            ...cached.data,
            data: {
              ...cached.data.data,
              content: cached.data.data.content.map((asset) =>
                asset.id === id ? { ...asset, projectOffboarded: offboarded } : asset
              ),
            },
          },
        });
      });
      return { snapshots };
    },
    onSuccess: () => toast.success('Offboard status updated'),
    onError: (_err, _vars, context) => {
      context?.snapshots?.forEach(([key, cached]) => queryClient.setQueryData(key, cached));
      toast.error('Failed to update offboard status');
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['assets'] }),
  });

  const handleExport = async (format: 'csv' | 'excel') => {
    const res = format === 'csv' ? await assetApi.exportCsv() : await assetApi.exportExcel();
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const a = document.createElement('a');
    a.href = url;
    a.download = `assets.${format === 'csv' ? 'csv' : 'xlsx'}`;
    a.click();
  };

  const openEdit = useCallback((asset: Asset) => {
    setEditAsset(asset);
    Object.entries(asset).forEach(([k, v]) => setValue(k as keyof Asset, v));
    setOpen(true);
  }, [setValue]);

  const columns: Column<Asset>[] = useMemo(() => [
    {
      id: 'photoUrl',
      label: 'Photo',
      render: (row) =>
        row.photoUrl ? (
          <Box component="img" src={fileUrl(row.photoUrl)} alt={row.assetName} loading="lazy" decoding="async" sx={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 1 }} />
        ) : '—',
    },
    { id: 'companyName', label: 'Company', minWidth: 120 },
    { id: 'assetName', label: 'Asset Name', minWidth: 150 },
    { id: 'serialNumber', label: 'Serial No.' },
    { id: 'vendorName', label: 'Vendor' },
    { id: 'associatedDeveloper', label: 'Associated Developer', minWidth: 160 },
    { id: 'projectName', label: 'Project Name', minWidth: 140 },
    { id: 'assignedDate', label: 'Date Assigned' },
    {
      id: 'projectOffboarded',
      label: 'Offboarded',
      render: (row) =>
        hasRole('ADMIN') ? (
          <Switch
            size="small"
            checked={!!row.projectOffboarded}
            disabled={offboardMutation.isPending}
            onChange={(e) =>
              offboardMutation.mutate({ id: row.id, offboarded: e.target.checked })
            }
          />
        ) : (
          <Chip
            size="small"
            label={row.projectOffboarded ? 'Yes' : 'No'}
            color={row.projectOffboarded ? 'warning' : 'default'}
          />
        ),
    },
    {
      id: 'status',
      label: 'Status',
      render: (row) => <Chip label={row.status} size="small" color={STATUS_COLORS[row.status]} />,
    },
    { id: 'assignedToName', label: 'Assigned To', render: (row) => row.assignedToName || '-' },
    {
      id: 'actions',
      label: 'Actions',
      render: (row) =>
        hasRole('ADMIN') ? (
          <Box>
            <IconButton size="small" onClick={() => openEdit(row)}><Edit fontSize="small" /></IconButton>
            <IconButton size="small" color="error" onClick={() => deleteMutation.mutate(row.id)}>
              <Delete fontSize="small" />
            </IconButton>
          </Box>
        ) : null,
    },
  ], [hasRole, offboardMutation.isPending, offboardMutation.mutate, deleteMutation.mutate, openEdit]);

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>Asset Management</Typography>
          <Typography variant="body2" color="text.secondary">Track and manage company assets</Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          {hasRole('ADMIN') && (
            <>
              <Button startIcon={<Download />} variant="outlined" onClick={() => handleExport('csv')}>CSV</Button>
              <Button startIcon={<Download />} variant="outlined" onClick={() => handleExport('excel')}>Excel</Button>
            </>
          )}
          {hasRole('ADMIN') && (
            <Button startIcon={<Add />} variant="contained" onClick={() => { setEditAsset(null); reset(); setOpen(true); }}>
              Add Asset
            </Button>
          )}
        </Box>
      </Box>

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            size="small"
            placeholder="Search assets..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            InputProps={{ startAdornment: <InputAdornment position="start"><Search /></InputAdornment> }}
          />
        </Grid>
        <Grid item xs={12} md={3}>
          <TextField
            fullWidth
            size="small"
            select
            label="Status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <MenuItem value="">All</MenuItem>
            {(['AVAILABLE', 'ASSIGNED', 'RETURNED', 'DAMAGED', 'LOST'] as AssetStatus[]).map((s) => (
              <MenuItem key={s} value={s}>{s}</MenuItem>
            ))}
          </TextField>
        </Grid>
      </Grid>

      <DataTable
        columns={columns}
        rows={assets}
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
        <DialogTitle>{editAsset ? 'Edit Asset' : 'Add Asset'}</DialogTitle>
        <form onSubmit={handleSubmit((d) => saveMutation.mutate({
          ...d,
          assetCategory: d.assetCategory || 'GENERAL',
          assetType: d.assetType || 'IT_ASSET',
        }))}>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Company Name" {...register('companyName')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Asset Name" {...register('assetName')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Associated Developer" {...register('associatedDeveloper')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Project Name" {...register('projectName')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Date Assigned"
                  type="date"
                  InputLabelProps={{ shrink: true }}
                  {...register('assignedDate')}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Serial Number" {...register('serialNumber')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Vendor Name" {...register('vendorName')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Warranty Expiry" type="date" InputLabelProps={{ shrink: true }} {...register('warrantyExpiryDate')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Remarks" {...register('remarks')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Button variant="outlined" component="label" fullWidth>
                  Upload Photo
                  <input type="file" hidden accept="image/*" onChange={(e) => setPhotoFile(e.target.files?.[0] || null)} />
                </Button>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Button variant="outlined" component="label" fullWidth>
                  Upload Video
                  <input type="file" hidden accept="video/*" onChange={(e) => setVideoFile(e.target.files?.[0] || null)} />
                </Button>
              </Grid>
              {editAsset?.photoUrl && (
                <Grid item xs={12}>
                  <Box component="img" src={fileUrl(editAsset.photoUrl)} alt="preview" sx={{ maxHeight: 120, borderRadius: 1 }} />
                </Grid>
              )}
              {editAsset?.videoUrl && (
                <Grid item xs={12}>
                  <Box component="video" src={fileUrl(editAsset.videoUrl)} controls sx={{ maxHeight: 160, borderRadius: 1 }} />
                </Grid>
              )}
              <Grid item xs={12}>
                <FormControlLabel
                  control={<Switch {...register('projectOffboarded')} />}
                  label="Project Offboarded"
                />
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
    </Box>
  );
}
