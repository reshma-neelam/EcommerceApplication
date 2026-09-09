package com.scaler.orderprocessor.service;

import com.scaler.orderprocessor.dto.order.AddressDTO;
import com.scaler.orderprocessor.dto.order.OrderCreateRequestDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

@Component
public class RequestHasher {

    public String hash(OrderCreateRequestDTO request) {
        StringJoiner canonical = new StringJoiner("|");
        request.getLines().stream()
                .sorted(Comparator.comparing(l -> l.getProductId().toString()))
                .forEach(l -> canonical.add(l.getProductId() + ":" + l.getQuantity()));
        canonical.add("SHIP=" + address(request.getShippingAddress()));
        canonical.add("BILL=" + address(request.getBillingAddress() != null
                ? request.getBillingAddress() : request.getShippingAddress()));
        return sha256(canonical.toString());
    }

    private String address(AddressDTO a) {
        if (a == null) {
            return "";
        }
        return String.join(",",
                nv(a.getRecipientName()), nv(a.getLine1()), nv(a.getLine2()), nv(a.getCity()),
                nv(a.getStateRegion()), nv(a.getPostalCode()), nv(a.getCountryCode()), nv(a.getPhone()));
    }

    private String nv(String s) {
        return s == null ? "" : s.trim();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
