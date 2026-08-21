package com.example.carservice.service;

import com.example.carservice.dto.AiServiceAdvisorResponse;
import com.example.carservice.dto.AiServiceSummaryResponse;
import com.example.carservice.entity.*;
import com.example.carservice.exception.InvalidWorkflowTransitionException;
import com.example.carservice.exception.ResourceNotFoundException;
import com.example.carservice.repository.AdditionalRepairRepository;
import com.example.carservice.repository.ServiceRecordRepository;
import com.example.carservice.repository.UserRepository;
import com.example.carservice.repository.VehicleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AiAdvisorService
 *
 * Implements business orchestration for:
 * 1. AI Service Advisor: Evaluates customer symptoms against verified vehicle data.
 * 2. AI Service Summary: Translates finalized technical service dossiers into friendly customer summaries.
 */
@Service
public class AiAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(AiAdvisorService.class);

    private static final String ADVISOR_DISCLAIMER =
            "AI guidance is informational and does not replace a professional vehicle inspection at our service center.";

    private static final String SUMMARY_DISCLAIMER =
            "AI-generated summary based on verified service data.";

    private final GeminiService geminiService;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final AdditionalRepairRepository additionalRepairRepository;
    private final ObjectMapper objectMapper;

    public AiAdvisorService(GeminiService geminiService,
                            VehicleRepository vehicleRepository,
                            UserRepository userRepository,
                            ServiceRecordRepository serviceRecordRepository,
                            AdditionalRepairRepository additionalRepairRepository,
                            ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.additionalRepairRepository = additionalRepairRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * AI Service Advisor: Provides recommendations for a customer's vehicle problem.
     */
    @Transactional(readOnly = true)
    public AiServiceAdvisorResponse getServiceAdvice(Long vehicleId, String problemDescription, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        if (!vehicle.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to request AI advice for this vehicle.");
        }

        String systemInstruction = """
                You are an automotive service advisor assistant at ServiceMate.
                Your task is to provide general, helpful service recommendations to car owners based on their vehicle information and reported symptoms.

                ServiceMate offers exactly five standard service packages in its service catalog:
                1. BRAKE_SERVICE (Brake Service & Fluid, ₹1,799): Front & rear brake pad overhaul, rotor cleaning, line bleeding, and fluid replacement.
                2. AC_SERVICE (AC Service & Inspection, ₹1,299): AC gas refill, cabin filter deep clean, condenser cooling check, and duct sanitization.
                3. BATTERY_SERVICE (Battery Inspection & Care, ₹499): Voltage load test, terminal anti-corrosion treatment, and alternator output test.
                4. OIL_CHANGE (Oil Change, ₹999): Premium synthetic engine oil replacement and oil filter renewal.
                5. GENERAL_SERVICE (General Service, ₹1,499): Comprehensive 40-point full vehicle inspection, fluid top-ups, filter cleaning, transmission/drivetrain inspection, suspension, exhaust, and general diagnostics.

                CRITICAL PACKAGE SELECTION RULES:
                FIRST, verify if the customer query or symptoms match any of the 4 specialized packages:
                - If the issue is related to Brakes (squeaking, grinding, brake pads, spongy pedal, stopping distance, brake fluid) -> MUST choose 'BRAKE_SERVICE'.
                - If the issue is related to AC / Air Conditioning (not cooling, warm air, low gas, foul smell, cabin filter, blower, AC service) -> MUST choose 'AC_SERVICE'.
                - If the issue is related to Battery (car not starting, difficult crank, weak battery, alternator, electrical start) -> MUST choose 'BATTERY_SERVICE'.
                - If the issue is related to Engine Oil / Lubrication (oil change, overdue oil, engine oil level, oil filter renewal) -> MUST choose 'OIL_CHANGE'.
                - ONLY if the issue is NONE of the above 4 specialized categories (such as transmission, suspension, drivetrain, steering, unusual noises, acceleration issues, clutch, warning lights, or general diagnostic checks), THEN choose 'GENERAL_SERVICE'.

                Rules:
                1. The 'recommendedPackage' field MUST contain the exact ServiceMate enum code:
                   - BRAKE_SERVICE
                   - AC_SERVICE
                   - BATTERY_SERVICE
                   - OIL_CHANGE
                   - GENERAL_SERVICE
                   Do NOT invent packages such as 'TRANSMISSION_SERVICE', 'SUSPENSION_SERVICE', 'ENGINE_SERVICE', or 'CLUTCH_SERVICE'.
                2. Do NOT claim a definitive mechanical diagnosis. Use cautious, advisory terms such as 'possible', 'may indicate', or 'recommended inspection'.
                3. Categorize urgency strictly as one of: 'LOW', 'MEDIUM', or 'HIGH'.
                4. Explain the reason in simple, clear language understandable to everyday car owners.
                5. Return your answer ONLY in valid JSON matching this schema:
                {
                  "possibleSystem": "string (e.g. Braking System, Engine Cooling, Transmission / Drivetrain, Electrical System)",
                  "recommendedService": "string (e.g. Brake pad inspection and fluid renewal, Engine diagnostic and vehicle inspection)",
                  "recommendedPackage": "BRAKE_SERVICE" | "AC_SERVICE" | "BATTERY_SERVICE" | "OIL_CHANGE" | "GENERAL_SERVICE",
                  "urgency": "LOW" | "MEDIUM" | "HIGH",
                  "explanation": "string (2-3 clear sentences explaining why this may occur, why the recommended ServiceMate package is appropriate, and what should be checked)"
                }
                """;

        String userPrompt = String.format("""
                Vehicle Details:
                - Make: %s
                - Model: %s
                - Year: %s
                - Fuel Type: %s
                - Transmission: %s
                - Current Distance Covered: %s

                Customer Reported Issue:
                "%s"

                Provide your structured advisory recommendation in JSON.
                """,
                vehicle.getMake() != null ? vehicle.getMake() : "Unknown",
                vehicle.getModel() != null ? vehicle.getModel() : "Unknown",
                vehicle.getManufacturingYear() != null ? String.valueOf(vehicle.getManufacturingYear()) : "N/A",
                vehicle.getFuelType() != null ? vehicle.getFuelType().name() : "N/A",
                vehicle.getTransmission() != null ? vehicle.getTransmission().name() : "N/A",
                vehicle.getCurrentMileage() != null ? vehicle.getCurrentMileage() + " km" : "Unknown",
                problemDescription.trim()
        );

        String jsonResponse = geminiService.generateJson(systemInstruction, userPrompt);

        try {
            String cleanJson = cleanJsonText(jsonResponse);
            JsonNode root = objectMapper.readTree(cleanJson);
            String possibleSystem = root.path("possibleSystem").asText("General Vehicle System");
            String recommendedService = root.path("recommendedService").asText("General Vehicle Inspection");
            String rawUrgency = root.path("urgency").asText("MEDIUM").toUpperCase().trim();
            String urgency = ("LOW".equals(rawUrgency) || "HIGH".equals(rawUrgency)) ? rawUrgency : "MEDIUM";
            String explanation = root.path("explanation").asText("A professional workshop inspection is recommended to evaluate this issue.");

            String rawPackage = root.hasNonNull("recommendedPackage") ? root.path("recommendedPackage").asText() : null;
            ServiceType matchedServiceType = resolveRecommendedServiceType(rawPackage, recommendedService, problemDescription);

            return new AiServiceAdvisorResponse(
                    possibleSystem,
                    recommendedService,
                    matchedServiceType.name(),
                    matchedServiceType.getDisplayName(),
                    matchedServiceType.getBasePrice(),
                    urgency,
                    explanation,
                    ADVISOR_DISCLAIMER
            );
        } catch (Exception ex) {
            log.error("Failed to parse Gemini JSON advisor response: {}", jsonResponse, ex);
            ServiceType defaultType = resolveRecommendedServiceType(null, null, problemDescription);
            return new AiServiceAdvisorResponse(
                    "Vehicle System",
                    "Comprehensive Inspection",
                    defaultType.name(),
                    defaultType.getDisplayName(),
                    defaultType.getBasePrice(),
                    "MEDIUM",
                    "Based on the reported symptoms, a qualified technician should inspect your " + vehicle.getMake() + " " + vehicle.getModel() + " to determine the root cause.",
                    ADVISOR_DISCLAIMER
            );
        }
    }

    private String cleanJsonText(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            trimmed = trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    /**
     * Resiliently resolves the appropriate ServiceMate ServiceType enum.
     * Verifies if any of the specialized service packages (BRAKE_SERVICE, AC_SERVICE, BATTERY_SERVICE, OIL_CHANGE)
     * match the recommendation or customer query. If no specialized package applies, suggests GENERAL_SERVICE.
     */
    private ServiceType resolveRecommendedServiceType(String rawPackage, String recommendedService, String problemDescription) {
        // 1. Direct package match from Gemini structured response
        ServiceType fromPackage = resolveFromText(rawPackage);
        if (fromPackage != null && fromPackage != ServiceType.GENERAL_SERVICE) {
            return fromPackage;
        }

        // 2. If Gemini returned GENERAL_SERVICE or null, verify if recommendedService or problemDescription
        // specifically belongs to one of our specialized packages
        ServiceType fromService = resolveFromText(recommendedService);
        if (fromService != null && fromService != ServiceType.GENERAL_SERVICE) {
            return fromService;
        }

        ServiceType fromProblem = resolveFromText(problemDescription);
        if (fromProblem != null && fromProblem != ServiceType.GENERAL_SERVICE) {
            return fromProblem;
        }

        // 3. If explicit General Service was requested or no specialized package applies, return GENERAL_SERVICE
        if (fromPackage == ServiceType.GENERAL_SERVICE || fromService == ServiceType.GENERAL_SERVICE || fromProblem == ServiceType.GENERAL_SERVICE) {
            return ServiceType.GENERAL_SERVICE;
        }

        log.info("No specialized package matched for '{}'; defaulting to GENERAL_SERVICE.", rawPackage != null ? rawPackage : problemDescription);
        return ServiceType.GENERAL_SERVICE;
    }

    private ServiceType resolveFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String trimmed = text.trim();

        // LEVEL 1 — Exact enum match (case-insensitive)
        for (ServiceType type : ServiceType.values()) {
            if (type.name().equalsIgnoreCase(trimmed)) {
                return type;
            }
        }

        // LEVEL 2 — Normalized enum match (replace spaces and hyphens with underscores)
        String normalizedEnum = trimmed.toUpperCase()
                .replaceAll("[-\\s]+", "_")
                .replaceAll("^_+|_+$", "");
        for (ServiceType type : ServiceType.values()) {
            if (type.name().equalsIgnoreCase(normalizedEnum)) {
                return type;
            }
        }

        // LEVEL 3 — Existing display-name match (case-insensitive, normalized whitespace)
        String normalizedDisplay = trimmed.replaceAll("\\s+", " ").trim();
        for (ServiceType type : ServiceType.values()) {
            if (type.getDisplayName().equalsIgnoreCase(normalizedDisplay)) {
                return type;
            }
        }

        // LEVEL 4 — Safe aliases & keywords
        String lower = trimmed.toLowerCase();

        // Brake keywords
        if (lower.contains("brake") || lower.contains("braking") || lower.contains("brake pad")
                || lower.contains("brake fluid") || lower.contains("rotor") || lower.contains("caliper")) {
            return ServiceType.BRAKE_SERVICE;
        }

        // AC keywords (word-boundary check for standalone "ac" or explicit terms)
        if (lower.contains("air condition") || lower.contains("air-condition") || lower.contains("aircondition")
                || lower.contains("ac service") || lower.contains("ac cooling") || lower.contains("ac inspection")
                || lower.contains("refrigerant") || lower.contains("cabin filter") || lower.contains("climate control")
                || lower.matches(".*\\bac\\b.*")) {
            return ServiceType.AC_SERVICE;
        }

        // Battery keywords
        if (lower.contains("battery") || lower.contains("alternator") || lower.contains("starter motor")
                || lower.contains("battery replacement") || lower.contains("battery inspection")
                || lower.contains("cranking") || lower.contains("jump start") || lower.contains("dead battery")) {
            return ServiceType.BATTERY_SERVICE;
        }

        // Oil keywords
        if (lower.contains("engine oil") || lower.contains("oil change") || lower.contains("oil replacement")
                || lower.contains("lubricant") || lower.contains("motor oil") || lower.contains("synthetic oil")
                || lower.contains("oil filter") || lower.matches(".*\\boil\\b.*")) {
            return ServiceType.OIL_CHANGE;
        }

        // General diagnostic keywords
        if (lower.contains("general service") || lower.contains("general inspection")
                || lower.contains("vehicle inspection") || lower.contains("diagnostic inspection")
                || lower.contains("40-point") || lower.contains("40 point") || lower.contains("full service")) {
            return ServiceType.GENERAL_SERVICE;
        }

        return null;
    }

    /**
     * AI Service Summary: Translates finalized service records into a friendly customer summary.
     */
    @Transactional(readOnly = true)
    public AiServiceSummaryResponse getServiceSummary(Long recordId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        ServiceRecord record = serviceRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Service record not found with id: " + recordId));

        if (user.getRole() == Role.CUSTOMER && !record.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view this service record.");
        }

        if (record.getFinalizedAt() == null) {
            throw new InvalidWorkflowTransitionException("AI service summary is only available for completed and finalized service visits.");
        }

        List<AdditionalRepair> repairs = additionalRepairRepository.findByBookingIdOrderByRequestedAtDesc(record.getBooking().getId());

        // Extract factual items
        String itemsSummary = record.getItems().stream()
                .map(i -> "- " + i.getDescription() + " (" + i.getCategory() + ", Qty: " + i.getQuantity() + ")")
                .collect(Collectors.joining("\n"));

        String findingsSummary = record.getInspectionFindings().stream()
                .map(f -> "- " + f.getComponent() + ": " + f.getConditionStatus() + (f.getNotes() != null ? " (" + f.getNotes() + ")" : ""))
                .collect(Collectors.joining("\n"));

        String repairsSummary = repairs.isEmpty() ? "None" : repairs.stream()
                .map(r -> "- " + r.getDescription() + " (Status: " + r.getStatus() + ", Reason: " + r.getReason() + ")")
                .collect(Collectors.joining("\n"));

        String systemInstruction = """
                You are a professional automotive service summary assistant at ServiceMate.
                Your task is to convert factual technical vehicle service records, replaced parts, and diagnostic inspection findings into a friendly, reassuring, customer-oriented summary for the vehicle owner.
                
                Rules:
                1. Use ONLY the supplied factual data. Do NOT invent facts, repairs, parts, prices, or diagnoses.
                2. Mention the primary maintenance performed, notable inspection findings, and approved additional repairs where applicable.
                3. Keep the summary concise (2-4 clear sentences / one short paragraph).
                4. Write in a polite, professional, and clear tone.
                """;

        String userPrompt = String.format("""
                Vehicle: %s %s (%d), Mileage: %d km
                Service Package: %s
                Service Date: %s
                Technician Notes: %s

                Work Performed & Replaced Parts:
                %s

                Diagnostic Checkpoints:
                %s

                Additional Repair Findings:
                %s

                Summarize this completed service visit for the customer.
                """,
                record.getVehicle().getMake(),
                record.getVehicle().getModel(),
                record.getVehicle().getManufacturingYear(),
                record.getMileage(),
                record.getServiceType() != null ? record.getServiceType().name() : "Standard Service",
                record.getServiceDate() != null ? record.getServiceDate().toString() : "Recent",
                record.getServiceSummary() != null ? record.getServiceSummary() : "Completed standard service checklist.",
                itemsSummary.isEmpty() ? "Standard scheduled service" : itemsSummary,
                findingsSummary.isEmpty() ? "All checked components normal" : findingsSummary,
                repairsSummary
        );

        String summaryText = geminiService.generateText(systemInstruction, userPrompt);
        if (summaryText == null || summaryText.trim().isEmpty()) {
            summaryText = "Your vehicle successfully received scheduled maintenance and diagnostic checks as documented in the itemized record.";
        }

        return new AiServiceSummaryResponse(recordId, summaryText.trim(), SUMMARY_DISCLAIMER);
    }
}
