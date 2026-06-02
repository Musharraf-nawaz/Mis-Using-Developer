import { Box, Typography, Grid, Card, CardContent, Button } from '@mui/material';
import { Download, Assessment, Inventory, Event } from '@mui/icons-material';
import { assetApi, interviewApi } from '../api/services';
import { toast } from 'react-toastify';

const reports = [
  { title: 'Asset Inventory Report', desc: 'Complete asset inventory', icon: <Inventory />, type: 'asset-excel' },
  { title: 'Asset Assignment Report', desc: 'Current assignments', icon: <Assessment />, type: 'asset-csv' },
  { title: 'Daily Interviews', desc: 'Today\'s interview schedule', icon: <Event />, type: 'interview-csv' },
  { title: 'Monthly Interviews', desc: 'Monthly interview summary', icon: <Event />, type: 'interview-csv' },
];

export default function ReportsPage() {
  const handleExport = async (type: string) => {
    try {
      let res;
      let filename = 'report';
      if (type === 'asset-csv') {
        res = await assetApi.exportCsv();
        filename = 'asset-inventory.csv';
      } else if (type === 'asset-excel') {
        res = await assetApi.exportExcel();
        filename = 'asset-inventory.xlsx';
      } else {
        res = await interviewApi.getAll({ size: 1000 });
        const interviews = res.data.data?.content ?? [];
        const csv = ['ID,Candidate,Email,Interviewer,Date,Status,Round\n',
          ...interviews.map((i) =>
            `${i.id},${i.candidateName},${i.candidateEmail},${i.interviewerName},${i.interviewDate},${i.interviewStatus},${i.interviewRound}`
          )].join('');
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'interviews-report.csv';
        a.click();
        toast.success('Report downloaded');
        return;
      }
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      a.click();
      toast.success('Report downloaded');
    } catch {
      toast.error('Export failed');
    }
  };

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} gutterBottom>Reports</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Generate and export enterprise reports
      </Typography>
      <Grid container spacing={3}>
        {reports.map((report) => (
          <Grid item xs={12} sm={6} md={3} key={report.title}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Box sx={{ color: 'primary.main', mb: 1 }}>{report.icon}</Box>
                <Typography variant="subtitle1" fontWeight={600}>{report.title}</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{report.desc}</Typography>
                <Button
                  startIcon={<Download />}
                  variant="outlined"
                  size="small"
                  onClick={() => handleExport(report.type)}
                >
                  Export
                </Button>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
