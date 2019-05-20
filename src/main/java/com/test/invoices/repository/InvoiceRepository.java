package com.test.invoices.repository;

import com.test.invoices.domain.Invoice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends CrudRepository<Invoice, Long> {

    @Query("SELECT SUM(invoice.purchaseAmount) FROM Invoice invoice where invoice.customerId = :customerId")
    Long sumOfInvoicePurchaseAmountsForCustomer(@Param("customerId") Long customerId);

}
