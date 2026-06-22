package com.minimart.server.service;

import com.minimart.common.model.Customer;
import com.minimart.server.dao.CustomerDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business service for customer management.
 * Phone number AES encryption/decryption is handled transparently by {@link CustomerDAO}.
 */
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerDAO customerDAO = new CustomerDAO();

    public List<Customer> findAll() throws SQLException {
        return customerDAO.findAll();
    }

    public Optional<Customer> findById(int id) throws SQLException {
        return customerDAO.findById(id);
    }

    public List<Customer> search(String keyword) throws SQLException {
        return customerDAO.search(keyword);
    }

    public Customer add(Customer customer) throws SQLException {
        validateCustomer(customer);
        int newId = customerDAO.insert(customer);
        customer.setId(newId);
        log.info("Customer added: id={}, name={}", newId, customer.getCustomerName());
        return customer;
    }

    public Customer update(Customer customer) throws SQLException {
        validateCustomer(customer);
        if (!customerDAO.update(customer)) {
            throw new SQLException("Customer not found: id=" + customer.getId());
        }
        return customer;
    }

    public void delete(int id) throws SQLException {
        if (!customerDAO.delete(id)) {
            throw new SQLException("Customer not found: id=" + id);
        }
        log.info("Customer deleted: id={}", id);
    }

    private void validateCustomer(Customer c) {
        if (c.getCustomerName() == null || c.getCustomerName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (c.getPhoneNumber() == null || c.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
    }
}
