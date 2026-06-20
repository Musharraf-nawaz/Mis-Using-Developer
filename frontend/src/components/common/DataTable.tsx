import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TablePagination,
  Skeleton,
  Box,
  LinearProgress,
} from '@mui/material';

export interface Column<T> {
  id: string;
  label: string;
  render?: (row: T) => React.ReactNode;
  minWidth?: number;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  loading?: boolean;
  fetching?: boolean;
  page: number;
  size: number;
  total: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
  getRowId: (row: T) => string | number;
}

export default function DataTable<T>({
  columns,
  rows,
  loading,
  fetching,
  page,
  size,
  total,
  onPageChange,
  onSizeChange,
  getRowId,
}: DataTableProps<T>) {
  if (loading && rows.length === 0) {
    return (
      <Box>
        {[1, 2, 3, 4, 5].map((i) => (
          <Skeleton key={i} height={48} sx={{ mb: 1 }} />
        ))}
      </Box>
    );
  }

  return (
    <Paper variant="outlined" sx={{ position: 'relative' }}>
      {fetching && <LinearProgress sx={{ position: 'absolute', top: 0, left: 0, right: 0, zIndex: 1 }} />}
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              {columns.map((col) => (
                <TableCell key={col.id} sx={{ fontWeight: 600, minWidth: col.minWidth }}>
                  {col.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} align="center" sx={{ py: 4 }}>
                  No records found
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow key={getRowId(row)} hover>
                  {columns.map((col) => (
                    <TableCell key={col.id}>
                      {col.render
                        ? col.render(row)
                        : String((row as Record<string, unknown>)[col.id] ?? '')}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        component="div"
        count={total}
        page={page}
        onPageChange={(_, p) => onPageChange(p)}
        rowsPerPage={size}
        onRowsPerPageChange={(e) => onSizeChange(parseInt(e.target.value, 10))}
        rowsPerPageOptions={[5, 10, 25, 50]}
      />
    </Paper>
  );
}
