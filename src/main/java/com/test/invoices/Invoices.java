package com.test.invoices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.invoices.domain.CreateOrUpdateInvoiceRequest;
import com.test.invoices.domain.Customer;
import com.test.invoices.domain.Invoice;
import com.test.invoices.domain.InvoiceUpdatedEvent;
import com.test.invoices.repository.InvoiceRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Invoices(InvoiceRepository invoiceRepository, CustomerService customerService, RabbitTemplate rabbitTemplate) {
        this.invoiceRepository = invoiceRepository;
        this.customerService = customerService;
        this.rabbitTemplate = rabbitTemplate;
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
        Invoice savedInvoice = invoiceRepository.save(invoice);
        publishEvent(savedInvoice);
        return savedInvoice;
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
        }).ifPresent(account -> {
            invoiceRepository.deleteById(id);
            publishEvent(optionalInvoice.get());
        });
    }

    @ResponseStatus(OK)
    @RequestMapping(value = "/invoice/{id}", method = PUT)
    public Invoice updateInvoice(@RequestBody CreateOrUpdateInvoiceRequest createOrUpdateInvoiceRequest,
                                 @PathVariable("id") Long id,
                                 @RequestHeader("X-USER") String user) {
        Invoice savedInvoice = invoiceRepository.findById(id).map(invoice -> {
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
        publishEvent(savedInvoice);
        return savedInvoice;
    }

    private void publishEvent(Invoice invoice) {
        try {
            //TODO RabbitMQ does not guarantee ordering. So this event must contain date as well for the consumer to handle it
            InvoiceUpdatedEvent invoiceUpdatedEvent = new InvoiceUpdatedEvent();
            invoiceUpdatedEvent.setAggregatedRevenue(invoiceRepository.sumOfInvoicePurchaseAmountsForCustomer(invoice.getCustomerId()));
            invoiceUpdatedEvent.setCustomerId(invoice.getCustomerId());
            String invoiceUpdatedEventString = objectMapper.writeValueAsString(invoiceUpdatedEvent);
            rabbitTemplate.convertAndSend(Application.topicExchangeName, "invoice.id." + invoice.getId(), invoiceUpdatedEventString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}