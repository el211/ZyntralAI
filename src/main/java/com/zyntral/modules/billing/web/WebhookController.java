package com.zyntral.modules.billing.web;

import com.zyntral.common.web.ApiConstants;
import com.zyntral.modules.billing.application.BillingService;
import com.zyntral.modules.billing.domain.PaymentProviderKind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public payment-webhook endpoints (no JWT — authenticity is the provider signature). The raw
 * request body is required for signature verification, so we bind it as a String.
 */
@Tag(name = "Webhooks", description = "Payment provider webhooks (Stripe, PayPal)")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/billing/webhooks")
public class WebhookController {

    private final BillingService billing;

    public WebhookController(BillingService billing) {
        this.billing = billing;
    }

    @Operation(summary = "Stripe webhook receiver")
    @PostMapping("/stripe")
    public ResponseEntity<Void> stripe(@RequestBody String payload,
                                       @RequestHeader("Stripe-Signature") String signature) {
        billing.applyWebhook(PaymentProviderKind.STRIPE, payload, signature);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "PayPal webhook receiver")
    @PostMapping("/paypal")
    public ResponseEntity<Void> paypal(@RequestBody String payload,
                                       @RequestHeader(value = "Paypal-Transmission-Sig", required = false) String signature) {
        billing.applyWebhook(PaymentProviderKind.PAYPAL, payload, signature);
        return ResponseEntity.ok().build();
    }
}
