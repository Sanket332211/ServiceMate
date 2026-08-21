package com.example.carservice.service;

import com.example.carservice.dto.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * PdfReportService
 *
 * Generates portable, tamper-evident vehicle service reports and complete service passports using OpenPDF.
 */
@Service
public class PdfReportService {

    private static final Color COLOR_NAVY = new Color(13, 30, 76);
    private static final Color COLOR_PINK = new Color(196, 140, 179);
    private static final Color COLOR_SOFT_BLUE = new Color(131, 166, 206);
    private static final Color COLOR_BG_LIGHT = new Color(245, 247, 250);
    private static final Color COLOR_TEXT_DARK = new Color(30, 41, 59);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_SUCCESS = new Color(22, 101, 52);
    private static final Color COLOR_REJECTED = new Color(153, 27, 27);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_NAVY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_NAVY);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_NAVY);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_TEXT_DARK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXT_DARK);
    private static final Font FONT_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED);
    private static final Font FONT_ACCENT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_PINK);

    private final DecimalFormat currencyFormat = new DecimalFormat("₹#,##0.00");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Generates a PDF report for a single service visit.
     */
    public byte[] generateSingleServicePdf(ServiceRecordResponse record) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Header Branding
            addHeader(document, "OFFICIAL VEHICLE SERVICE PASSPORT", "Service Visit #" + record.getId());

            // Vehicle & Customer Info Card
            addVehicleAndCustomerCard(document, record);
            document.add(new Paragraph(" "));

            // Service Summary & Mileage
            addServiceSummarySection(document, record);
            document.add(new Paragraph(" "));

            // Work Performed / Service Items Table
            addServiceItemsTable(document, record);
            document.add(new Paragraph(" "));

            // Inspection Findings Table
            addInspectionFindingsTable(document, record);
            document.add(new Paragraph(" "));

            // Additional Repairs (Approved vs Rejected)
            if (record.getAdditionalRepairs() != null && !record.getAdditionalRepairs().isEmpty()) {
                addAdditionalRepairsSection(document, record);
                document.add(new Paragraph(" "));
            }

            // Financial Breakdown
            addFinancialSummary(document, record);

            // Footer
            addFooter(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate service record PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a comprehensive multi-visit vehicle service history PDF.
     */
    public byte[] generateVehicleHistoryPdf(VehicleServiceHistoryResponse history) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Header Branding
            addHeader(document, "COMPLETE VEHICLE SERVICE HISTORY", "Vehicle: " + history.getVehicleRegistrationNumber());

            // Vehicle Dossier Overview
            addVehicleDossierOverview(document, history);
            document.add(new Paragraph(" "));

            // Iterate over all visits
            if (history.getRecords() == null || history.getRecords().isEmpty()) {
                Paragraph noVisits = new Paragraph("No completed service history recorded for this vehicle.", FONT_NORMAL);
                noVisits.setAlignment(Element.ALIGN_CENTER);
                document.add(noVisits);
            } else {
                int visitNum = history.getRecords().size();
                for (ServiceRecordResponse record : history.getRecords()) {
                    addVisitSectionHeader(document, record, visitNum--);
                    addServiceSummarySection(document, record);
                    document.add(new Paragraph(" "));
                    addServiceItemsTable(document, record);
                    document.add(new Paragraph(" "));
                    addInspectionFindingsTable(document, record);
                    document.add(new Paragraph(" "));
                    if (record.getAdditionalRepairs() != null && !record.getAdditionalRepairs().isEmpty()) {
                        addAdditionalRepairsSection(document, record);
                        document.add(new Paragraph(" "));
                    }
                    addFinancialSummary(document, record);
                    document.add(new Paragraph(" "));
                    addDivider(document);
                    document.add(new Paragraph(" "));
                }
            }

            addFooter(document);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate vehicle service history PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document, String title, String subtitle) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{65, 35});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        Paragraph brand = new Paragraph("SERVICEMATE", FONT_TITLE);
        Paragraph mainTitle = new Paragraph(title, FONT_SUBTITLE);
        leftCell.addElement(brand);
        leftCell.addElement(mainTitle);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph sub = new Paragraph(subtitle, FONT_ACCENT_BOLD);
        sub.setAlignment(Element.ALIGN_RIGHT);
        Paragraph tag = new Paragraph("Smart Car Service Management", FONT_MUTED);
        tag.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(sub);
        rightCell.addElement(tag);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        document.add(headerTable);

        addDivider(document);
    }

    private void addVehicleAndCustomerCard(Document document, ServiceRecordResponse record) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 25, 25, 25});

        addCardCell(table, "Registration No.", record.getVehicleRegistrationNumber(), true);
        addCardCell(table, "Vehicle", record.getVehicleMake() + " " + record.getVehicleModel(), true);
        addCardCell(table, "Customer", record.getCustomerName(), false);
        addCardCell(table, "Phone", record.getCustomerPhone() != null ? record.getCustomerPhone() : "N/A", false);

        addCardCell(table, "Fuel / Transmission", record.getVehicleFuelType() + " / " + record.getVehicleTransmission(), false);
        addCardCell(table, "Service Date", record.getServiceDate() != null ? record.getServiceDate().format(dateFormatter) : "N/A", false);
        addCardCell(table, "Service Mileage", record.getMileage() != null ? record.getMileage() + " km" : "N/A", true);
        addCardCell(table, "Service Package", record.getServiceTypeDisplayName(), false);

        document.add(table);
    }

    private void addVehicleDossierOverview(Document document, VehicleServiceHistoryResponse history) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 25, 25, 25});

        addCardCell(table, "Registration No.", history.getVehicleRegistrationNumber(), true);
        addCardCell(table, "Vehicle Model", history.getVehicleMake() + " " + history.getVehicleModel(), true);
        addCardCell(table, "Owner", history.getCustomerName(), false);
        addCardCell(table, "Current Odometer", history.getCurrentMileage() + " km", true);

        addCardCell(table, "Fuel / Transmission", history.getVehicleFuelType() + " / " + history.getVehicleTransmission(), false);
        addCardCell(table, "Total Service Visits", String.valueOf(history.getTotalCompletedVisits()), true);
        addCardCell(table, "Cumulative Spend", currencyFormat.format(history.getTotalAmountSpent()), true);
        addCardCell(table, "Passport Status", "VERIFIED & AUTHENTIC", false);

        document.add(table);
    }

    private void addVisitSectionHeader(Document document, ServiceRecordResponse record, int visitNumber) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{70, 30});

        PdfPCell left = new PdfPCell(new Phrase("SERVICE VISIT #" + visitNumber + " • " + record.getServiceTypeDisplayName(), FONT_SUBTITLE));
        left.setBackgroundColor(COLOR_BG_LIGHT);
        left.setPadding(6);
        left.setBorder(Rectangle.BOX);
        left.setBorderColor(COLOR_SOFT_BLUE);

        String dateStr = record.getServiceDate() != null ? record.getServiceDate().format(dateFormatter) : "";
        String mileStr = record.getMileage() != null ? record.getMileage() + " km" : "";
        PdfPCell right = new PdfPCell(new Phrase(dateStr + " (" + mileStr + ")", FONT_BOLD));
        right.setBackgroundColor(COLOR_BG_LIGHT);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setPadding(6);
        right.setBorder(Rectangle.BOX);
        right.setBorderColor(COLOR_SOFT_BLUE);

        table.addCell(left);
        table.addCell(right);
        document.add(table);
    }

    private void addServiceSummarySection(Document document, ServiceRecordResponse record) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk("Technician Summary: ", FONT_BOLD));
        p.add(new Chunk(record.getServiceSummary() != null ? record.getServiceSummary() : "Standard service check-up completed.", FONT_NORMAL));
        document.add(p);
    }

    private void addServiceItemsTable(Document document, ServiceRecordResponse record) throws DocumentException {
        Paragraph title = new Paragraph("Work Performed & Parts Replaced", FONT_HEADER);
        title.setSpacingAfter(4);
        document.add(title);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{45, 15, 12, 14, 14});

        addTableHeaderCell(table, "Description");
        addTableHeaderCell(table, "Category");
        addTableHeaderCell(table, "Qty");
        addTableHeaderCell(table, "Unit Price");
        addTableHeaderCell(table, "Total");

        if (record.getItems() != null && !record.getItems().isEmpty()) {
            for (ServiceItemDto item : record.getItems()) {
                addTableCell(table, item.getDescription(), Element.ALIGN_LEFT);
                addTableCell(table, item.getCategory() != null ? item.getCategory() : "PARTS", Element.ALIGN_LEFT);
                addTableCell(table, String.valueOf(item.getQuantity()), Element.ALIGN_CENTER);
                addTableCell(table, currencyFormat.format(item.getUnitPrice()), Element.ALIGN_RIGHT);
                addTableCell(table, currencyFormat.format(item.getTotalPrice()), Element.ALIGN_RIGHT);
            }
        } else {
            PdfPCell cell = new PdfPCell(new Phrase("No work items recorded.", FONT_MUTED));
            cell.setColspan(5);
            cell.setPadding(6);
            table.addCell(cell);
        }

        document.add(table);
    }

    private void addInspectionFindingsTable(Document document, ServiceRecordResponse record) throws DocumentException {
        Paragraph title = new Paragraph("40-Point Diagnostic Inspection Observations", FONT_HEADER);
        title.setSpacingAfter(4);
        document.add(title);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30, 25, 45});

        addTableHeaderCell(table, "Component");
        addTableHeaderCell(table, "Condition Status");
        addTableHeaderCell(table, "Observations & Notes");

        if (record.getInspectionFindings() != null && !record.getInspectionFindings().isEmpty()) {
            for (InspectionFindingDto finding : record.getInspectionFindings()) {
                addTableCell(table, finding.getComponent(), Element.ALIGN_LEFT);
                addTableCell(table, finding.getConditionStatus(), Element.ALIGN_LEFT);
                addTableCell(table, finding.getNotes() != null ? finding.getNotes() : "-", Element.ALIGN_LEFT);
            }
        } else {
            PdfPCell cell = new PdfPCell(new Phrase("No inspection findings recorded.", FONT_MUTED));
            cell.setColspan(3);
            cell.setPadding(6);
            table.addCell(cell);
        }

        document.add(table);
    }

    private void addAdditionalRepairsSection(Document document, ServiceRecordResponse record) throws DocumentException {
        Paragraph title = new Paragraph("Additional Repair Discoveries & Customer Decisions", FONT_HEADER);
        title.setSpacingAfter(4);
        document.add(title);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35, 35, 15, 15});

        addTableHeaderCell(table, "Repair Finding");
        addTableHeaderCell(table, "Diagnostic Reason");
        addTableHeaderCell(table, "Estimate");
        addTableHeaderCell(table, "Decision");

        for (AdditionalRepairResponse repair : record.getAdditionalRepairs()) {
            addTableCell(table, repair.getDescription(), Element.ALIGN_LEFT);
            addTableCell(table, repair.getReason(), Element.ALIGN_LEFT);
            addTableCell(table, currencyFormat.format(repair.getEstimatedAmount()), Element.ALIGN_RIGHT);

            boolean isApproved = "APPROVED".equalsIgnoreCase(String.valueOf(repair.getStatus()));
            PdfPCell statusCell = new PdfPCell(new Phrase(
                    isApproved ? "APPROVED (Performed)" : "DECLINED (Not Performed)",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, isApproved ? COLOR_SUCCESS : COLOR_REJECTED)
            ));
            statusCell.setPadding(5);
            table.addCell(statusCell);
        }

        document.add(table);
    }

    private void addFinancialSummary(Document document, ServiceRecordResponse record) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{60, 40});

        addSummaryRow(table, "Base Scheduled Service:", currencyFormat.format(record.getActualBaseServiceAmount()), false);
        addSummaryRow(table, "Approved Additional Work:", currencyFormat.format(record.getActualAdditionalRepairsAmount()), false);
        if (record.isPickupDropUsed()) {
            addSummaryRow(table, "Doorstep Valet Pickup/Drop:", currencyFormat.format(record.getPickupDropCharge()), false);
        }
        addSummaryRow(table, "Final Total Paid:", currencyFormat.format(record.getActualTotalAmount()), true);

        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(
                "This document is an authentic digital service record certified by ServiceMate.\n" +
                        "For ownership verification or service inquiries, present this document at any authorized workshop.",
                FONT_MUTED
        ));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(COLOR_SOFT_BLUE);
        cell.setPadding(8);
        footer.addCell(cell);
        document.add(footer);
    }

    private void addCardCell(PdfPTable table, String label, String value, boolean highlight) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_BG_LIGHT);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(COLOR_SOFT_BLUE);
        cell.setPadding(5);

        Paragraph p1 = new Paragraph(label, FONT_MUTED);
        Paragraph p2 = new Paragraph(value != null ? value : "-", highlight ? FONT_BOLD : FONT_NORMAL);
        cell.addElement(p1);
        cell.addElement(p2);
        table.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String headerText) {
        PdfPCell cell = new PdfPCell(new Phrase(headerText, FONT_HEADER));
        cell.setBackgroundColor(COLOR_BG_LIGHT);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(COLOR_NAVY);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", FONT_NORMAL));
        cell.setHorizontalAlignment(align);
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, boolean isTotal) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, isTotal ? FONT_BOLD : FONT_NORMAL));
        labelCell.setBorder(isTotal ? Rectangle.TOP : Rectangle.NO_BORDER);
        labelCell.setPadding(4);

        PdfPCell valCell = new PdfPCell(new Phrase(value, isTotal ? FONT_ACCENT_BOLD : FONT_BOLD));
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valCell.setBorder(isTotal ? Rectangle.TOP : Rectangle.NO_BORDER);
        valCell.setPadding(4);

        table.addCell(labelCell);
        table.addCell(valCell);
    }

    private void addDivider(Document document) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setSpacingAfter(8);
        document.add(p);
    }
}
