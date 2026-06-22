package com.minimart.server.service;

import com.minimart.common.dto.ProductListWrapper;
import com.minimart.common.model.Product;
import com.minimart.server.dao.ProductDAO;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business service for product management including XML import/export via JAXB.
 */
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductDAO productDAO = new ProductDAO();

    // ── CRUD ────────────────────────────────────────────────

    public List<Product> findAll() throws SQLException {
        return productDAO.findAll();
    }

    public List<Product> findAllActive() throws SQLException {
        return productDAO.findAllActive();
    }

    public Optional<Product> findById(int id) throws SQLException {
        return productDAO.findById(id);
    }

    public List<Product> search(String keyword) throws SQLException {
        return productDAO.search(keyword);
    }

    public Product add(Product product) throws SQLException {
        // Validate unique product code
        if (productDAO.findByCode(product.getProductCode()).isPresent()) {
            throw new IllegalArgumentException("Product code already exists: " + product.getProductCode());
        }
        product.setStatus("ACTIVE");
        int newId = productDAO.insert(product);
        product.setId(newId);
        log.info("Product added: code={}, name={}", product.getProductCode(), product.getName());
        return product;
    }

    public Product update(Product product) throws SQLException {
        // Validate code uniqueness (allow the same product to keep its own code)
        Optional<Product> existing = productDAO.findByCode(product.getProductCode());
        if (existing.isPresent() && existing.get().getId() != product.getId()) {
            throw new IllegalArgumentException("Product code already used by another product: " + product.getProductCode());
        }
        if (!productDAO.update(product)) {
            throw new SQLException("Product not found: id=" + product.getId());
        }
        return product;
    }

    public void delete(int id) throws SQLException {
        if (!productDAO.delete(id)) {
            throw new SQLException("Product not found: id=" + id);
        }
        log.info("Product deleted: id={}", id);
    }

    // ── XML Export ──────────────────────────────────────────

    /**
     * Marshals the entire active product list to an XML string using JAXB.
     *
     * @return a well-formed XML string containing all active products
     */
    public String exportToXml() throws JAXBException, SQLException {
        List<Product> products = productDAO.findAll();
        ProductListWrapper wrapper = new ProductListWrapper(products);

        JAXBContext   context    = JAXBContext.newInstance(ProductListWrapper.class, Product.class);
        Marshaller    marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        StringWriter writer = new StringWriter();
        marshaller.marshal(wrapper, writer);
        log.info("Exported {} products to XML", products.size());
        return writer.toString();
    }

    // ── XML Import ──────────────────────────────────────────

    /**
     * Imports products from an XML string.
     * <ul>
     *   <li>Parses the XML using JAXB</li>
     *   <li>Skips products whose {@code product_code} already exists (duplicate check)</li>
     *   <li>Inserts new products</li>
     * </ul>
     *
     * @param xmlContent the XML string to import
     * @return number of products successfully imported
     */
    public int importFromXml(String xmlContent) throws JAXBException, SQLException {
        JAXBContext    context      = JAXBContext.newInstance(ProductListWrapper.class, Product.class);
        Unmarshaller   unmarshaller = context.createUnmarshaller();
        ProductListWrapper wrapper  = (ProductListWrapper) unmarshaller.unmarshal(new StringReader(xmlContent));

        List<Product> toImport = wrapper.getProducts();
        if (toImport == null || toImport.isEmpty()) {
            log.warn("XML import: no products found in file");
            return 0;
        }

        int imported = 0;
        int skipped  = 0;
        for (Product p : toImport) {
            // Validate required fields
            if (p.getProductCode() == null || p.getName() == null || p.getPrice() == null) {
                log.warn("Skipping invalid product entry: {}", p);
                skipped++;
                continue;
            }
            // Duplicate code check
            if (productDAO.findByCode(p.getProductCode()).isPresent()) {
                log.debug("Skipping duplicate product code: {}", p.getProductCode());
                skipped++;
                continue;
            }
            if (p.getStatus() == null) p.setStatus("ACTIVE");
            if (p.getUnit() == null) p.setUnit("Cái");
            productDAO.insert(p);
            imported++;
        }
        log.info("XML import complete: {} imported, {} skipped", imported, skipped);
        return imported;
    }
}
