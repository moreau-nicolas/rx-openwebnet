package com.github.niqdev.openwebnet.message;

import com.github.niqdev.openwebnet.domain.Who;

public class Lighting extends BaseOpenMessage {

    protected Lighting() {
        super(Who.LIGHTING);
    }

    @Override
    public String getValue() {
        return null;
    }
}