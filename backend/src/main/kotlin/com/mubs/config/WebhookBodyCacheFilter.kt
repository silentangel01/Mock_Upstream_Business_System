package com.mubs.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class WebhookBodyCacheFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.requestURI == "/api/v1/hvas/webhook") {
            val wrappedRequest = ContentCachingRequestWrapper(request)
            filterChain.doFilter(wrappedRequest, response)
        } else {
            filterChain.doFilter(request, response)
        }
    }
}
