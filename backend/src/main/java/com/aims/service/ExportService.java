package com.aims.service;

import com.aims.entity.Asset;
import com.aims.entity.Interview;
import com.aims.repository.AssetRepository;
import com.aims.repository.InterviewRepository;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final AssetRepository assetRepository;
    private final InterviewRepository interviewRepository;

    public byte[] exportAssetsToCsv() throws Exception {
        List<Asset> assets = assetRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"ID", "Company", "Asset Name", "Category", "Type",
                    "Serial", "Tag", "Status", "Assigned To"});
            for (Asset a : assets) {
                writer.writeNext(new String[]{
                        String.valueOf(a.getId()), a.getCompanyName(), a.getAssetName(),
                        a.getAssetCategory(), a.getAssetType(), a.getSerialNumber(),
                        a.getAssetTag(), a.getStatus().name(),
                        a.getAssignedTo() != null ? a.getAssignedTo().getFullName() : ""
                });
            }
        }
        return out.toByteArray();
    }

    public byte[] exportAssetsToExcel() throws Exception {
        List<Asset> assets = assetRepository.findAll();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Assets");
            Row header = sheet.createRow(0);
            String[] headers = {"ID", "Company", "Asset Name", "Category", "Type", "Serial", "Tag", "Status"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (Asset a : assets) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getId());
                row.createCell(1).setCellValue(a.getCompanyName());
                row.createCell(2).setCellValue(a.getAssetName());
                row.createCell(3).setCellValue(a.getAssetCategory());
                row.createCell(4).setCellValue(a.getAssetType());
                row.createCell(5).setCellValue(a.getSerialNumber() != null ? a.getSerialNumber() : "");
                row.createCell(6).setCellValue(a.getAssetTag() != null ? a.getAssetTag() : "");
                row.createCell(7).setCellValue(a.getStatus().name());
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportInterviewsToCsv() throws Exception {
        List<Interview> interviews = interviewRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"ID", "Candidate", "Email", "Interviewer", "Date", "Time", "Status", "Round"});
            for (Interview i : interviews) {
                writer.writeNext(new String[]{
                        String.valueOf(i.getId()), i.getCandidateName(), i.getCandidateEmail(),
                        i.getInterviewerName(), i.getInterviewDate().toString(),
                        i.getInterviewTime().toString(), i.getInterviewStatus().name(),
                        i.getInterviewRound().name()
                });
            }
        }
        return out.toByteArray();
    }
}
