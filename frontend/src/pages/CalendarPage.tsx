import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Card,
  CardContent,
  ToggleButtonGroup,
  ToggleButton,
  Grid,
  Chip,
  List,
  ListItem,
  ListItemText,
  Skeleton,
} from '@mui/material';
import { format, startOfWeek, endOfWeek, startOfMonth, endOfMonth, eachDayOfInterval } from 'date-fns';
import { interviewApi } from '../api/services';
import type { InterviewCalendar } from '../types';

type ViewMode = 'day' | 'week' | 'month';

export default function CalendarPage() {
  const [view, setView] = useState<ViewMode>('week');
  const [currentDate] = useState(new Date());

  const { start, end } = useMemo(() => {
    if (view === 'day') {
      const d = format(currentDate, 'yyyy-MM-dd');
      return { start: d, end: d };
    }
    if (view === 'week') {
      return {
        start: format(startOfWeek(currentDate), 'yyyy-MM-dd'),
        end: format(endOfWeek(currentDate), 'yyyy-MM-dd'),
      };
    }
    return {
      start: format(startOfMonth(currentDate), 'yyyy-MM-dd'),
      end: format(endOfMonth(currentDate), 'yyyy-MM-dd'),
    };
  }, [view, currentDate]);

  const { data, isLoading } = useQuery({
    queryKey: ['calendar', start, end],
    queryFn: () => interviewApi.getCalendar(start, end),
    staleTime: 5 * 60 * 1000,
  });

  const interviews = data?.data?.data ?? [];

  const interviewsByDay = useMemo(() => {
    const map = new Map<string, InterviewCalendar[]>();
    for (const interview of interviews) {
      const key = interview.interviewDate;
      const list = map.get(key);
      if (list) list.push(interview);
      else map.set(key, [interview]);
    }
    return map;
  }, [interviews]);

  const days = useMemo(() => {
    if (view === 'month') {
      return eachDayOfInterval({ start: startOfMonth(currentDate), end: endOfMonth(currentDate) });
    }
    if (view === 'week') {
      return eachDayOfInterval({ start: startOfWeek(currentDate), end: endOfWeek(currentDate) });
    }
    return [currentDate];
  }, [view, currentDate]);

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>Interview Calendar</Typography>
          <Typography variant="body2" color="text.secondary">
            {format(currentDate, 'MMMM yyyy')}
          </Typography>
        </Box>
        <ToggleButtonGroup value={view} exclusive onChange={(_, v) => v && setView(v)} size="small">
          <ToggleButton value="day">Day</ToggleButton>
          <ToggleButton value="week">Week</ToggleButton>
          <ToggleButton value="month">Month</ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {isLoading ? (
        <Grid container spacing={2}>
          {Array.from({ length: view === 'month' ? 12 : 7 }).map((_, i) => (
            <Grid item xs={12} sm={4} key={i}>
              <Skeleton variant="rounded" height={120} />
            </Grid>
          ))}
        </Grid>
      ) : (
        <Grid container spacing={2}>
          {days.map((day) => {
            const dayKey = format(day, 'yyyy-MM-dd');
            const dayInterviews = interviewsByDay.get(dayKey) ?? [];
            return (
              <Grid
                item
                xs={12}
                sm={view === 'month' ? 4 : 12}
                md={view === 'day' ? 12 : view === 'week' ? 12 / 7 : 4}
                key={dayKey}
              >
                <Card variant="outlined" sx={{ minHeight: 120 }}>
                  <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                    <Typography variant="subtitle2" fontWeight={600} gutterBottom>
                      {format(day, 'EEE, MMM d')}
                      {dayInterviews.length > 0 && (
                        <Chip label={dayInterviews.length} size="small" sx={{ ml: 1 }} color="primary" />
                      )}
                    </Typography>
                    <List dense disablePadding>
                      {dayInterviews.map((i) => (
                        <ListItem key={i.id} disablePadding sx={{ py: 0.25 }}>
                          <ListItemText
                            primary={i.candidateName}
                            secondary={`${i.interviewTime} · ${i.interviewRound}`}
                            primaryTypographyProps={{ variant: 'body2', fontWeight: 500 }}
                            secondaryTypographyProps={{ variant: 'caption' }}
                          />
                        </ListItem>
                      ))}
                    </List>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}
    </Box>
  );
}
