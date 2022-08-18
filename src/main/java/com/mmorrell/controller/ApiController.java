package com.mmorrell.controller;

import com.mmorrell.model.AccountInfoRow;
import com.mmorrell.model.OpenOrdersAccountRow;
import com.mmorrell.model.OrderRow;
import com.mmorrell.serum.model.OpenOrdersAccount;
import com.google.common.io.BaseEncoding;
import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.PublicKey;
import org.springframework.jdbc.core.*;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class ApiController {

    private final JdbcTemplate jdbcTemplate;

    public ApiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping(value = "/serum/markets")
    public List<AccountInfoRow> getMarkets() {
        return jdbcTemplate.query(
                "select pubkey as publicKey, data, slot" +
                        " from account" +
                        " where length(data)=388;",
                BeanPropertyRowMapper.newInstance(AccountInfoRow.class)
        );
    }

    @GetMapping(value = "/serum/account/{accountId}")
    public AccountInfoRow getAccount(@PathVariable String accountId) {
        PublicKey accountPubkey = new PublicKey(accountId);
        byte[] pubkeyArray = accountPubkey.toByteArray();

        String sql = "SELECT slot, data FROM account WHERE pubkey=decode(?, 'hex')";
        Map<String, Object> rowData = jdbcTemplate.queryForMap(
                sql,
                BaseEncoding.base16().lowerCase().encode(
                        pubkeyArray
                )
        );

        byte[] accountData = (byte[]) rowData.get("data");
        Long slot = (Long) rowData.get("slot");

        final AccountInfoRow result = AccountInfoRow.builder()
                .data(accountData)
                .slot(slot)
                .publicKey(pubkeyArray)
                .build();

        return result;
    }

    @PostMapping("/serum/accounts")
    public List<AccountInfoRow> getMultipleAccounts(@RequestBody List<String> accountIds) {
        if (accountIds.size() == 0) {
            return Collections.emptyList();
        }

        String pubkeys;
        try {
            pubkeys = accountIds.stream()
                    .map(pubkey -> {
                                byte[] byteArray = new PublicKey(pubkey).toByteArray();
                                return "decode('" + BaseEncoding.base16().lowerCase().encode(byteArray) + "', 'hex')";
                            }
                    )
                    .collect(Collectors.joining(","));
        } catch (IllegalArgumentException ex) {
            return Collections.emptyList();
        }

        List<AccountInfoRow> result = jdbcTemplate.query(
                String.format(
                        "select pubkey as publicKey, data, slot from account where pubkey in (%s)",
                        pubkeys
                ),
                BeanPropertyRowMapper.newInstance(AccountInfoRow.class)
        );

        return result;
    }


    @Deprecated
    @GetMapping(value = "/serum/slot/{accountId}")
    public Long getSlot(@PathVariable String accountId) {
        PublicKey accountPubkey = new PublicKey(accountId);

        String sql = "SELECT slot FROM account WHERE pubkey=decode(?, 'hex')";
        Long slot = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                BaseEncoding.base16().lowerCase().encode(
                        accountPubkey.toByteArray()
                )
        );

        return slot;
    }

    @GetMapping(value = "/serum/orders/{owner}")
    public List<OpenOrdersAccountRow> getOpenOrderAccounts(@PathVariable String owner) {
        PublicKey ownerPubkey = new PublicKey(owner);
        String sql = "SELECT data " +
                "FROM account " +
                "WHERE length(data)=3228 and position" +
                "('\\x" + BaseEncoding.base16().lowerCase().encode(ownerPubkey.toByteArray()) + "'::bytea in data)>0";

        List<OpenOrdersAccountRow> openOrdersAccountRows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    OpenOrdersAccount openOrdersAccount = OpenOrdersAccount.readOpenOrdersAccount(
                            rs.getBytes(1)
                    );

                    List<OrderRow> orderRows = convertOrdersToOrderRows(openOrdersAccount.getOrders());

                    OpenOrdersAccountRow row = OpenOrdersAccountRow.builder()
                            .market(openOrdersAccount.getMarket())
                            .orders(orderRows)
                            .build();

                    return row;
                }
        );

        return openOrdersAccountRows.stream()
                .filter(openOrdersAccountRow -> openOrdersAccountRow.getOrders().size() > 0)
                .toList();
    }

    private List<OrderRow> convertOrdersToOrderRows(List<OpenOrdersAccount.Order> orders) {
        if (orders.size() == 0) {
            return Collections.emptyList();
        }

        return orders.stream()
                .map(order -> OrderRow.builder().price(order.getPrice()).bid(order.isBid()).build())
                .toList();
    }
}
