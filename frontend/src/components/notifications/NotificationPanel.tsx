import {
  Box,
  Popover,
  Typography,
  List,
  ListItem,
  ListItemText,
  IconButton,
  Button,
  Divider,
  Chip,
  CircularProgress,
} from '@mui/material';
import { Close, DoneAll } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { notificationApi } from '../../api/services';
import type { Notification } from '../../types';

interface Props {
  anchorEl: HTMLElement | null;
  onClose: () => void;
}

const TYPE_COLORS: Record<string, 'primary' | 'success' | 'error' | 'warning' | 'info' | 'default'> = {
  INTERVIEW_SCHEDULED: 'primary',
  INTERVIEW_UPDATED: 'info',
  INTERVIEW_CANCELLED: 'error',
  ROUND_AVAILABLE: 'warning',
  CANDIDATE_SELECTED: 'success',
  CANDIDATE_REJECTED: 'error',
  PROJECT_ASSIGNED: 'primary',
  PROJECT_UPDATED: 'info',
  ASSET_ASSIGNED: 'success',
  ASSET_RETURNED: 'info',
};

export default function NotificationPanel({ anchorEl, onClose }: Props) {
  const queryClient = useQueryClient();
  const open = Boolean(anchorEl);

  const { data, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationApi.getAll(0, 30),
    enabled: open,
    staleTime: 2 * 60 * 1000,
  });

  useEffect(() => {
    if (open) {
      queryClient.invalidateQueries({ queryKey: ['notifications-unread'] });
    }
  }, [open, queryClient]);

  const notifications = data?.data?.data?.content ?? [];

  const markReadMutation = useMutation({
    mutationFn: (id: number) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread'] });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread'] });
    },
  });

  return (
    <Popover
      open={open}
      anchorEl={anchorEl}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      PaperProps={{ sx: { width: 380, maxHeight: 480 } }}
    >
      <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="subtitle1" fontWeight={700}>
          Notifications
        </Typography>
        <Box>
          <Button
            size="small"
            startIcon={<DoneAll />}
            onClick={() => markAllMutation.mutate()}
            disabled={markAllMutation.isPending}
          >
            Mark all read
          </Button>
          <IconButton size="small" onClick={onClose}>
            <Close fontSize="small" />
          </IconButton>
        </Box>
      </Box>
      <Divider />
      {isLoading ? (
        <Box sx={{ p: 4, textAlign: 'center' }}>
          <CircularProgress size={28} />
        </Box>
      ) : notifications.length === 0 ? (
        <Typography variant="body2" color="text.secondary" sx={{ p: 3, textAlign: 'center' }}>
          No notifications yet
        </Typography>
      ) : (
        <List dense sx={{ overflow: 'auto', maxHeight: 360 }}>
          {notifications.map((n: Notification) => (
            <ListItem
              key={n.id}
              sx={{
                bgcolor: n.read ? 'transparent' : 'action.hover',
                borderBottom: 1,
                borderColor: 'divider',
                flexDirection: 'column',
                alignItems: 'flex-start',
              }}
              secondaryAction={
                !n.read && (
                  <IconButton edge="end" size="small" onClick={() => markReadMutation.mutate(n.id)}>
                    <DoneAll fontSize="small" />
                  </IconButton>
                )
              }
            >
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mb: 0.5 }}>
                <Typography variant="body2" fontWeight={n.read ? 400 : 700}>
                  {n.title}
                </Typography>
                <Chip
                  label={n.type.replace(/_/g, ' ')}
                  size="small"
                  color={TYPE_COLORS[n.type] || 'default'}
                  sx={{ height: 20, fontSize: 10 }}
                />
              </Box>
              <ListItemText
                primary={n.message}
                secondary={new Date(n.createdAt).toLocaleString()}
                primaryTypographyProps={{ variant: 'caption', color: 'text.secondary' }}
                secondaryTypographyProps={{ variant: 'caption' }}
              />
            </ListItem>
          ))}
        </List>
      )}
    </Popover>
  );
}
