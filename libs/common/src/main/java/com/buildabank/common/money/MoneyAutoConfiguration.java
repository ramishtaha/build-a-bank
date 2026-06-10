// libs/common/src/main/java/com/buildabank/common/money/MoneyAutoConfiguration.java
package com.buildabank.common.money;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Step 28 · the heart of the starter — an {@link AutoConfiguration} Spring Boot discovers automatically (via
 * the {@code AutoConfiguration.imports} file) when this jar is on the classpath. The three conditions are what
 * make a starter polite:
 * <ul>
 *   <li>{@link EnableConfigurationProperties} binds {@code buildabank.money.*} into {@link MoneyProperties};</li>
 *   <li>{@link ConditionalOnProperty}{@code (matchIfMissing=true)} — on by default, but a consumer can switch
 *       the whole feature off with {@code buildabank.money.enabled=false};</li>
 *   <li>{@link ConditionalOnMissingBean} — <strong>back off</strong> if the consumer defined their own
 *       {@code MoneyFormatter}. A starter must never clobber a bean the application already provides.</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(MoneyProperties.class)
@ConditionalOnProperty(prefix = "buildabank.money", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MoneyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MoneyFormatter moneyFormatter(MoneyProperties properties) {
        return new MoneyFormatter(properties.getCurrencyCode());
    }
}
