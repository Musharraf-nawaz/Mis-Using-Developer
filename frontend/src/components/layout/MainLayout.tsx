import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  IconButton,
  Avatar,
  Menu,
  MenuItem,
  Badge,
  Divider,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  Inventory as AssetIcon,
  Assignment as AssignIcon,
  Event as InterviewIcon,
  CalendarMonth as CalendarIcon,
  Assessment as ReportIcon,
  People as UsersIcon,
  Settings as SettingsIcon,
  Menu as MenuIcon,
  Brightness4,
  Brightness7,
  Notifications as NotifIcon,
  Logout,
  Person,
} from '@mui/icons-material';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../context/AuthContext';
import { useThemeMode } from '../../context/ThemeContext';
import { assetApi, dashboardApi, interviewApi, notificationApi } from '../../api/services';
import type { Role } from '../../types';

const DRAWER_WIDTH = 260;

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
  roles?: Role[];
}

const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
  { label: 'Assets', path: '/assets', icon: <AssetIcon /> },
  { label: 'Asset Assignment', path: '/assets/assign', icon: <AssignIcon />, roles: ['ADMIN', 'HR'] },
  { label: 'Interviews', path: '/interviews', icon: <InterviewIcon />, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  { label: 'Calendar', path: '/calendar', icon: <CalendarIcon />, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  { label: 'Reports', path: '/reports', icon: <ReportIcon />, roles: ['ADMIN', 'HR'] },
  { label: 'Users', path: '/users', icon: <UsersIcon />, roles: ['ADMIN'] },
  { label: 'Settings', path: '/settings', icon: <SettingsIcon /> },
];

export default function MainLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const { user, logout, hasRole } = useAuth();
  const { mode, toggleMode } = useThemeMode();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();

  const { data: unreadData } = useQuery({
    queryKey: ['notifications-unread'],
    queryFn: () => notificationApi.getUnreadCount(),
    refetchInterval: document.hidden ? false : 120000,
  });

  const prefetchRoute = (path: string) => {
    if (path === '/dashboard') {
      queryClient.prefetchQuery({ queryKey: ['dashboard'], queryFn: () => dashboardApi.get() });
    }
    if (path === '/assets') {
      queryClient.prefetchQuery({
        queryKey: ['assets', 0, 10, '', ''],
        queryFn: () => assetApi.getAll({ page: 0, size: 10 }),
      });
    }
    if (path === '/interviews') {
      queryClient.prefetchQuery({
        queryKey: ['interviews', 0, 10, ''],
        queryFn: () => interviewApi.getAll({ page: 0, size: 10 }),
      });
    }
  };

  const unreadCount = unreadData?.data?.data?.count ?? 0;

  const filteredNav = navItems.filter(
    (item) => !item.roles || item.roles.some((r) => hasRole(r))
  );

  const drawer = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ p: 2.5, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6" fontWeight={700} color="primary">
          Mis-Using Developer
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Enterprise Management Platform
        </Typography>
      </Box>
      <List sx={{ flex: 1, px: 1, py: 2 }}>
        {filteredNav.map((item) => (
          <ListItemButton
            key={item.path}
            selected={location.pathname === item.path}
            onMouseEnter={() => prefetchRoute(item.path)}
            onClick={() => {
              navigate(item.path);
              if (isMobile) setMobileOpen(false);
            }}
            sx={{ borderRadius: 1, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
      <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
        <Typography variant="caption" color="text.secondary">
          © 2026 Mis-Using Developer
        </Typography>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
          bgcolor: 'background.paper',
          color: 'text.primary',
          borderBottom: 1,
          borderColor: 'divider',
        }}
      >
        <Toolbar>
          {isMobile && (
            <IconButton edge="start" onClick={() => setMobileOpen(!mobileOpen)} sx={{ mr: 1 }}>
              <MenuIcon />
            </IconButton>
          )}
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 600 }}>
            {filteredNav.find((n) => n.path === location.pathname)?.label || 'Mis-Using Developer'}
          </Typography>
          <IconButton onClick={toggleMode} size="small">
            {mode === 'light' ? <Brightness4 /> : <Brightness7 />}
          </IconButton>
          <IconButton size="small" sx={{ ml: 1 }}>
            <Badge badgeContent={unreadCount} color="error">
              <NotifIcon />
            </Badge>
          </IconButton>
          <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} sx={{ ml: 1 }}>
            <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: 14 }}>
              {user?.fullName?.charAt(0) || 'U'}
            </Avatar>
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled>
              <Typography variant="body2">{user?.fullName}</Typography>
            </MenuItem>
            <MenuItem disabled>
              <Typography variant="caption" color="text.secondary">
                {user?.role} · {user?.department}
              </Typography>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => { navigate('/profile'); setAnchorEl(null); }}>
              <ListItemIcon><Person fontSize="small" /></ListItemIcon>
              Profile
            </MenuItem>
            <MenuItem
              onClick={() => {
                logout();
                navigate('/login');
              }}
            >
              <ListItemIcon><Logout fontSize="small" /></ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant={isMobile ? 'temporary' : 'permanent'}
          open={isMobile ? mobileOpen : true}
          onClose={() => setMobileOpen(false)}
          sx={{
            '& .MuiDrawer-paper': {
              width: DRAWER_WIDTH,
              boxSizing: 'border-box',
              borderRight: 1,
              borderColor: 'divider',
            },
          }}
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          mt: 8,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          bgcolor: 'background.default',
          minHeight: '100vh',
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}
