package com.minimart.common.dto;

import com.minimart.common.model.Product;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * JAXB wrapper for a list of products, used in XML import/export.
 *
 * <pre>
 * Output XML structure:
 * {@code
 * <products>
 *   <product>
 *     <productCode>SP001</productCode>
 *     <name>...</name>
 *     ...
 *   </product>
 * </products>
 * }
 * </pre>
 */
@XmlRootElement(name = "products")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductListWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(name = "product")
    private List<Product> products = new ArrayList<>();

    public ProductListWrapper() {}

    public ProductListWrapper(List<Product> products) {
        this.products = products;
    }

    public List<Product> getProducts()                    { return products; }
    public void setProducts(List<Product> products)       { this.products = products; }
}
