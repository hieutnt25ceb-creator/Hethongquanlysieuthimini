package com.minimart.common.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a product in the inventory catalog.
 * Annotated with JAXB for XML import/export functionality.
 */
@XmlRootElement(name = "product")
@XmlAccessorType(XmlAccessType.FIELD)
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement private int          id;
    @XmlElement private String       productCode;
    @XmlElement private String       name;
    @XmlElement private String       category;
    @XmlElement private String       description;
    @XmlElement private String       unit;
    @XmlElement private BigDecimal   price;
    @XmlElement private int          stockQuantity;
    @XmlElement private String       status;       // "ACTIVE" | "INACTIVE"
    @XmlElement private String       imagePath;
    private LocalDateTime            createdAt;
    private LocalDateTime            updatedAt;

    // ── Constructors ────────────────────────────────────────

    public Product() {}

    public Product(int id, String productCode, String name, String category, String unit,
                   BigDecimal price, int stockQuantity, String status, String imagePath) {
        this.id            = id;
        this.productCode   = productCode;
        this.name          = name;
        this.category      = category;
        this.unit          = unit;
        this.price         = price;
        this.stockQuantity = stockQuantity;
        this.status        = status;
        this.imagePath     = imagePath;
    }

    public Product(int id, String productCode, String name, String unit,
                   BigDecimal price, int stockQuantity, String status) {
        this(id, productCode, name, "Khác", unit, price, stockQuantity, status, null);
    }

    // ── Getters & Setters ───────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getProductCode()                  { return productCode; }
    public void setProductCode(String productCode)  { this.productCode = productCode; }

    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }

    public String getCategory()                     { return category; }
    public void setCategory(String category)        { this.category = category; }

    public String getDescription()                  { return description; }
    public void setDescription(String description)  { this.description = description; }

    public String getUnit()                         { return unit; }
    public void setUnit(String unit)                { this.unit = unit; }

    public BigDecimal getPrice()                    { return price; }
    public void setPrice(BigDecimal price)          { this.price = price; }

    public int getStockQuantity()                   { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getStatus()                       { return status; }
    public void setStatus(String status)            { this.status = status; }

    public String getImagePath()                    { return imagePath; }
    public void setImagePath(String imagePath)      { this.imagePath = imagePath; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                   { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)     { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", code='" + productCode + "', name='" + name + "', category='" + category + "', stock=" + stockQuantity + "}";
    }
}
