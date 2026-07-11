package org.redlance.common.requester.openpanel;

/**
 * Indicates that OpenPanel rejected an otherwise successfully delivered request.
 */
public final class OpenPanelRequestException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public OpenPanelRequestException(int statusCode, String responseBody) {
        super("OpenPanel request failed with HTTP " + statusCode);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return this.statusCode;
    }

    public String responseBody() {
        return this.responseBody;
    }
}
