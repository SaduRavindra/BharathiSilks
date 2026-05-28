package com.bharathisilks.service;

import com.bharathisilks.domain.Sale;
import com.bharathisilks.domain.SaleItem;
import com.bharathisilks.repo.ProductRepository;
import com.bharathisilks.repo.SaleRepository;
import com.bharathisilks.web.dto.ReportResponse;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final long DAY_MS = 86_400_000L;

    private final SaleRepository sales;
    private final ProductRepository products;

    public ReportService(SaleRepository sales, ProductRepository products) {
        this.sales = sales;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public ReportResponse report(int days) {
        long cutoff = System.currentTimeMillis() - (long) days * DAY_MS;
        List<Sale> all = sales.findAllByOrderByDateAsc();
        List<Sale> valid = all.stream()
                .filter(s -> !s.isReturned() && s.getDate() >= cutoff)
                .toList();

        double revenue = valid.stream().mapToDouble(Sale::getTotal).sum();
        double profit = valid.stream().mapToDouble(Sale::getProfit).sum();
        double tax = valid.stream().mapToDouble(Sale::getTax).sum();
        int bills = valid.size();
        double avgBill = bills == 0 ? 0 : revenue / bills;

        List<ReportResponse.BestSeller> best = bestSellers(valid);
        Map<String, Double> paymentMix = paymentMix(valid);
        List<ReportResponse.DeadStock> deadStock = deadStock(valid);

        List<Sale> recent = all.stream()
                .sorted(Comparator.comparingLong(Sale::getDate).reversed())
                .limit(12)
                .toList();

        return new ReportResponse(days, revenue, profit, tax, bills, avgBill,
                best, paymentMix, deadStock, recent);
    }

    private List<ReportResponse.BestSeller> bestSellers(List<Sale> valid) {
        Map<String, Integer> qty = new LinkedHashMap<>();
        Map<String, Double> amt = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        for (Sale s : valid) {
            for (SaleItem i : s.getItems()) {
                qty.merge(i.getSku(), i.getQty(), Integer::sum);
                amt.merge(i.getSku(), i.getPrice() * i.getQty(), Double::sum);
                names.putIfAbsent(i.getSku(), i.getName());
            }
        }
        return qty.entrySet().stream()
                .map(e -> new ReportResponse.BestSeller(
                        e.getKey(), names.get(e.getKey()), e.getValue(), amt.get(e.getKey())))
                .sorted(Comparator.comparingInt(ReportResponse.BestSeller::qty).reversed())
                .limit(6)
                .toList();
    }

    private Map<String, Double> paymentMix(List<Sale> valid) {
        Map<String, Double> mix = new LinkedHashMap<>();
        mix.put("Cash", 0.0);
        mix.put("UPI", 0.0);
        mix.put("Card", 0.0);
        for (Sale s : valid) {
            mix.merge(s.getPay() == null ? "Cash" : s.getPay(), s.getTotal(), Double::sum);
        }
        return mix;
    }

    private List<ReportResponse.DeadStock> deadStock(List<Sale> valid) {
        Set<String> sold = new HashSet<>();
        for (Sale s : valid) {
            for (SaleItem i : s.getItems()) {
                sold.add(i.getSku());
            }
        }
        return products.findAll().stream()
                .filter(p -> p.getStock() > 0 && !sold.contains(p.getSku()))
                .map(p -> new ReportResponse.DeadStock(
                        p.getSku(), p.getName(), p.getStock(),
                        p.getStock() * (p.getCost() > 0 ? p.getCost() : p.getPrice())))
                .toList();
    }
}
