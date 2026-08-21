package com.example.carservice.dto;

/**
 * AiServiceAdvisorResponse
 *
 * Structured AI service advisory recommendation returned to the customer.
 * Contains both high-level diagnostic guidance and a concrete, validated
 * ServiceMate service package mapping for seamless one-click booking.
 */
public class AiServiceAdvisorResponse {

    private String possibleSystem;
    private String recommendedService;
    private String recommendedPackage;      // e.g. "BRAKE_SERVICE"
    private String recommendedPackageName;  // e.g. "Brake Service & Fluid"
    private Integer recommendedPackagePrice;// e.g. 1799
    private String urgency;                 // "LOW", "MEDIUM", "HIGH"
    private String explanation;
    private String disclaimer;

    public AiServiceAdvisorResponse() {}

    public AiServiceAdvisorResponse(String possibleSystem,
                                  String recommendedService,
                                  String recommendedPackage,
                                  String recommendedPackageName,
                                  Integer recommendedPackagePrice,
                                  String urgency,
                                  String explanation,
                                  String disclaimer) {
        this.possibleSystem = possibleSystem;
        this.recommendedService = recommendedService;
        this.recommendedPackage = recommendedPackage;
        this.recommendedPackageName = recommendedPackageName;
        this.recommendedPackagePrice = recommendedPackagePrice;
        this.urgency = urgency;
        this.explanation = explanation;
        this.disclaimer = disclaimer;
    }

    public String getPossibleSystem() {
        return possibleSystem;
    }

    public void setPossibleSystem(String possibleSystem) {
        this.possibleSystem = possibleSystem;
    }

    public String getRecommendedService() {
        return recommendedService;
    }

    public void setRecommendedService(String recommendedService) {
        this.recommendedService = recommendedService;
    }

    public String getRecommendedPackage() {
        return recommendedPackage;
    }

    public void setRecommendedPackage(String recommendedPackage) {
        this.recommendedPackage = recommendedPackage;
    }

    public String getRecommendedPackageName() {
        return recommendedPackageName;
    }

    public void setRecommendedPackageName(String recommendedPackageName) {
        this.recommendedPackageName = recommendedPackageName;
    }

    public Integer getRecommendedPackagePrice() {
        return recommendedPackagePrice;
    }

    public void setRecommendedPackagePrice(Integer recommendedPackagePrice) {
        this.recommendedPackagePrice = recommendedPackagePrice;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
