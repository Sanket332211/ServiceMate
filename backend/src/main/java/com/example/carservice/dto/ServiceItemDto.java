package com.example.carservice.dto;

import com.example.carservice.entity.ServiceItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ServiceItemDto
 *
 * DTO for itemized work items, parts replacements, fluids, or labor entries.
 */
public class ServiceItemDto {

    private Long id;

    @NotBlank(message = "Item description cannot be blank.")
    private String description;

    private String category; // e.g. "PARTS", "LABOUR", "FLUIDS"

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    private Integer quantity;

    @NotNull(message = "Unit price is required.")
    @DecimalMin(value = "0.00", message = "Unit price cannot be negative.")
    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    public ServiceItemDto() {}

    public ServiceItemDto(String description, String category, Integer quantity, BigDecimal unitPrice) {
        this.description = description;
        this.category = category;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice != null && quantity != null ? unitPrice.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
    }

    public static ServiceItemDto fromEntity(ServiceItem item) {
        ServiceItemDto dto = new ServiceItemDto();
        dto.setId(item.getId());
        dto.setDescription(item.getDescription());
        dto.setCategory(item.getCategory());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
