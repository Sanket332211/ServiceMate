package com.example.carservice.dto;

/**
 * AiServiceSummaryResponse
 *
 * Customer-friendly AI summary of a finalized historical service record.
 */
public class AiServiceSummaryResponse {

    private Long serviceRecordId;
    private String summary;
    private String disclaimer;

    public AiServiceSummaryResponse() {}

    public AiServiceSummaryResponse(Long serviceRecordId, String summary, String disclaimer) {
        this.serviceRecordId = serviceRecordId;
        this.summary = summary;
        this.disclaimer = disclaimer;
    }

    public Long getServiceRecordId() {
        return serviceRecordId;
    }

    public void setServiceRecordId(Long serviceRecordId) {
        this.serviceRecordId = serviceRecordId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
