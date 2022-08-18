package com.mmorrell.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mmorrell.util.Base64Serializer;
import com.mmorrell.util.PublicKeyByteSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountInfoRow {

    @JsonSerialize(using = PublicKeyByteSerializer.class)
    private byte[] publicKey;

    @JsonSerialize(using = Base64Serializer.class)
    private byte[] data;

    @JsonSerialize
    private long slot;

}
