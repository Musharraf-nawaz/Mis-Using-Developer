import { Box, Typography, Card, CardContent, Switch, FormControlLabel, Divider } from '@mui/material';
import { useThemeMode } from '../context/ThemeContext';

export default function SettingsPage() {
  const { mode, toggleMode } = useThemeMode();

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} gutterBottom>Settings</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Configure application preferences
      </Typography>
      <Card>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Appearance</Typography>
          <FormControlLabel
            control={<Switch checked={mode === 'dark'} onChange={toggleMode} />}
            label={`Dark Mode (${mode})`}
          />
          <Divider sx={{ my: 2 }} />
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Notifications</Typography>
          <FormControlLabel control={<Switch defaultChecked />} label="In-app notifications" />
          <FormControlLabel control={<Switch defaultChecked />} label="Email notifications" />
          <Divider sx={{ my: 2 }} />
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>System</Typography>
          <Typography variant="body2" color="text.secondary">
            Version 1.0.0 · Mis-Using Developer
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
}
