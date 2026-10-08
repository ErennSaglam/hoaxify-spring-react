package com.hoaxify.common.web.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller parametresine gateway'in gönderdiği X-User-Id başlığını enjekte eder.
 * Monolith'teki @AuthenticationPrincipal CurrentUser'ın mikroservis karşılığı.
 *
 * <pre>
 * public ResponseEntity&lt;Void&gt; deleteHoax(@PathVariable Long id, @CurrentUserId Long currentUserId)
 * </pre>
 *
 * required = true (varsayılan): başlık yoksa 401. required = false: başlık yoksa null (anonim kullanıcı).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {

	boolean required() default true;
}
