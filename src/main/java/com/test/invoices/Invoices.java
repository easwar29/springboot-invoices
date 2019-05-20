package com.test.invoices;

import com.test.invoices.domain.CreateOrUpdateInvoiceRequest;
import com.test.invoices.domain.Customer;
import com.test.invoices.domain.Invoice;
import com.test.invoices.repository.InvoiceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
public class Invoices {

    private final InvoiceRepository invoiceRepository;

    private final CustomerService customerService;

    public Invoices(InvoiceRepository invoiceRepository, CustomerService customerService) {
        this.invoiceRepository = invoiceRepository;
        this.customerService = customerService;
    }


    @ResponseStatus(OK)
    @RequestMapping(value = "/invoice/{id}", method = GET)
    public Invoice invoice(@PathVariable("id") Long id) {
        return invoiceRepository.findById(id).orElse(null);
    }

    @ResponseStatus(CREATED)
    @RequestMapping(value = "/invoice", method = POST)
    public Invoice createInvoice(@RequestBody CreateOrUpdateInvoiceRequest createOrUpdateInvoiceRequest) {
        Invoice invoice = new Invoice();
        invoice.setPurchaseDate(createOrUpdateInvoiceRequest.getPurchaseDate());
        invoice.setPurchaseAmount(createOrUpdateInvoiceRequest.getPurchaseAmount());
        invoice.setDescription(createOrUpdateInvoiceRequest.getDescription());
        invoice.setInvoiceId(createOrUpdateInvoiceRequest.getInvoiceId());
        invoice.setCustomerId(createOrUpdateInvoiceRequest.getCustomerId());
        return invoiceRepository.save(invoice);
    }

    @ResponseStatus(OK)
    @RequestMapping(value = "/invoice/{id}", method = DELETE)
    public void deleteInvoice(@PathVariable("id") Long id, @RequestHeader("X-USER") String user) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(id);
        optionalInvoice.map(invoice -> {
            List<Customer> customersForUser = customerService.getCustomersForUser(user);
            return customersForUser.stream()
                    .filter(customer -> customer.getId().equals(invoice.getCustomerId()))
                    .findFirst().orElse(null);
        }).ifPresent(account -> invoiceRepository.deleteById(id));
    }

    @ResponseStatus(OK)
    @RequestMapping(value = "/invoice/{id}", method = PUT)
    public Invoice updateInvoice(@RequestBody CreateOrUpdateInvoiceRequest createOrUpdateInvoiceRequest,
                                 @PathVariable("id") Long id,
                                 @RequestHeader("X-USER") String user) {
        return invoiceRepository.findById(id).map(invoice -> {
                    List<Customer> customersForUser = customerService.getCustomersForUser(user);
                    return customersForUser.stream()
                            .filter(customer -> customer.getId().equals(invoice.getCustomerId()))
                            .findFirst().map(customer -> {
                                invoice.setInvoiceId(createOrUpdateInvoiceRequest.getInvoiceId());
                                invoice.setDescription(createOrUpdateInvoiceRequest.getDescription());
                                invoice.setPurchaseAmount(createOrUpdateInvoiceRequest.getPurchaseAmount());
                                invoice.setPurchaseDate(createOrUpdateInvoiceRequest.getPurchaseDate());
                                return invoiceRepository.save(invoice);
                            }).orElse(null);
                }
        ).orElse(null);
    }
}