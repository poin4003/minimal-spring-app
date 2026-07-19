package com.app.config.transaction;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
public class TransactionConfig {
}
