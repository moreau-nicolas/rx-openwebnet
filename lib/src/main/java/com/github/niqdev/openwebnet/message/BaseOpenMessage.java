package com.github.niqdev.openwebnet.message;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

public abstract class BaseOpenMessage implements OpenMessage {

    protected static final String FORMAT_PREFIX_REQUEST_WHO = "*%d*";
    protected static final String FORMAT_PREFIX_STATUS_WHO = "*#%d*";
    protected static final String FORMAT_REQUEST = "*%d*%d*%s##";
    protected static final String FORMAT_STATUS = "*#%d*%s##";
    protected static final String FORMAT_PREFIX_REQUEST = "*%d*%d*";

    protected static final int WHERE_MIN_VALUE = 0;
    protected static final int WHERE_MAX_VALUE = 9999;

    private final String value;

    protected BaseOpenMessage(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    protected static void checkRange(Integer from, Integer to, Integer value) {
        checkNotNull(value, "invalid null value");
        checkArgument(value >= from && value <= to,
            format("value must be between %d and %d", from, to));
    }

    /*
     * See also org.apache.commons.lang.StringUtils.isNumeric
     */
    protected static int checkIsInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid integer format");
        }
    }
}
