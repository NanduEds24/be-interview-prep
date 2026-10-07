package com.example.app.link;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * An absolute http(s) URL with a host. Checked with java.net.URI, the same parser the redirect's
 * Location header uses, so every accepted URL can actually be redirected to. (Hibernate's @URL uses the
 * more lenient java.net.URL, which accepts spaces and other characters URI rejects.)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HttpUrl.Validator.class)
public @interface HttpUrl {

    String message() default "url must be a valid http or https URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<HttpUrl, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true; // @NotBlank reports this case
            }
            try {
                URI uri = new URI(value);
                String scheme = uri.getScheme();
                // getAuthority, not getHost: URI returns no host for valid-to-redirect names with "_" or
                // non-ASCII letters (e.g. my_service.example.com, bücher.de).
                return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getAuthority() != null;
            } catch (URISyntaxException e) {
                return false;
            }
        }
    }
}
