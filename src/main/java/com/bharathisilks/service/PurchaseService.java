package com.bharathisilks.service;

import com.bharathisilks.domain.Product;
import com.bharathisilks.domain.Purchase;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.repo.PurchaseRepository;
import com.bharathisilks.web.dto.PurchaseRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private final ProductRepository products;
    private final PurchaseRepository purchases;

    public PurchaseService(ProductRepository products, PurchaseRepository purchases) {
        this.products = products;
        this.purchases = purchases;
    }

    public List<Purchase> list() {
        return purchases.findAllByOrderByDateAsc();
    }

    /** Records incoming stock: bumps quantity, refreshes cost, and logs the receipt. */
    @Transactional
    public Purchase receive(PurchaseRequest req) {
        Product product = products.findBySku(req.sku())
                .orElseThrow(() -> new IllegalArgumentException("Select a valid product"));

        int qty = Math.max(1, req.qty() == null ? 0 : req.qty());
        double cost = req.cost() == null ? 0 : req.cost();
        String supplier = req.supplier() == null ? "" : req.supplier().trim();

        product.setStock(product.getStock() + qty);
        if (cost > 0) {
            product.setCost(cost);
        }
        products.save(product);

        Purchase purchase = new Purchase();
        purchase.setDate(System.currentTimeMillis());
        purchase.setSku(product.getSku());
        purchase.setName(product.getName());
        purchase.setQty(qty);
        purchase.setCost(cost > 0 ? cost : product.getCost());
        purchase.setSupplier(supplier);
        return purchases.save(purchase);
    }
}
