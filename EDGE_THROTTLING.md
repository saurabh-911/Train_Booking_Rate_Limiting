# Layer-0 Edge Throttling (Nginx/Cloudflare conceptual)

Use IP-level throttling at the CDN or edge reverse-proxy before requests reach gateway/app.

## Nginx sample

```nginx
limit_req_zone $binary_remote_addr zone=booking_zone:10m rate=30r/s;
server {
  location /api/book-ticket {
    limit_req zone=booking_zone burst=60 nodelay;
    if ($http_user_agent ~* "(scrapy|curl|bot)") { return 403; }
  }
}
```

## Cloudflare sample policy

- Rule: `/api/book-ticket` > 120 req/min/IP -> block 10 minutes
- Action on WAF bot score < 30: challenge or block
- Return HTTP 429 for burst violations

This protects origin services from L7 abuse and bot floods.
