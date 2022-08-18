package com.mmorrell.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mmorrell.util.PublicKeySerializer;
import lombok.Builder;
import org.p2p.solanaj.core.PublicKey;

import java.util.List;

@Builder
public class OpenOrdersAccountRow {

    private PublicKey publicKey;
    @JsonSerialize(using = PublicKeySerializer.class)
    private PublicKey market;
    @JsonSerialize
    private List<OrderRow> orders;

    public List<OrderRow> getOrders() {
        return orders;
    }
}
