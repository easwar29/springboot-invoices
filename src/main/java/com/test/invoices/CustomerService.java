package com.test.invoices;

import com.test.invoices.domain.Customer;
import com.test.invoices.domain.CustomerWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class CustomerService {

    private final RestTemplate customerServiceRestTemplate;

    @Value("${customers.service.baseurl}")
    private String customerServiceUrl;

    public CustomerService(RestTemplate customerServiceRestTemplate) {
        this.customerServiceRestTemplate = customerServiceRestTemplate;
    }

    public List<Customer> getCustomersForUser(String user) {
        return customerServiceRestTemplate.getForEntity(customerServiceUrl, CustomerWrapper.class, user)
                .getBody()
                .getCustomers();
    }
}
