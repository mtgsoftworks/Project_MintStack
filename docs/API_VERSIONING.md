# API Versioning Policy

## Overview

MintStack Finance Portal uses URL-based API versioning to ensure backwards compatibility while allowing the API to evolve over time.

## Versioning Scheme

We follow [Semantic Versioning](https://semver.org/) (SemVer):

```
MAJOR.MINOR.PATCH
```

- **MAJOR**: Breaking changes that require client updates
- **MINOR**: New features that are backwards-compatible
- **PATCH**: Bug fixes and minor improvements

## Current Version

| Version | Status | Base URL |
|---------|--------|----------|
| v1 | **Stable** | `/api/v1/*` |

## URL Structure

All API endpoints follow this pattern:

```
/api/v{major}/{resource}
```

Examples:
- `GET /api/v1/currencies` - List all currencies
- `GET /api/v1/portfolios/{id}` - Get a specific portfolio
- `POST /api/v1/portfolios` - Create a new portfolio

## Response Headers

Every API response includes versioning headers:

| Header | Description | Example |
|--------|-------------|---------|
| `X-API-Version` | Current API version | `1.0.0` |
| `X-API-Min-Version` | Minimum supported version | `1.0.0` |
| `X-API-Deprecated` | Present if version is deprecated | `true` |
| `X-API-Sunset` | Removal date for deprecated versions | `2027-01-01` |

## Deprecation Policy

1. **Announcement**: Deprecation is announced at least 6 months before removal
2. **Headers**: Deprecated endpoints include `X-API-Deprecated: true`
3. **Documentation**: Deprecated features are marked in API docs
4. **Migration Guide**: A migration guide is provided for breaking changes

### Timeline

```
                    Deprecation Announced           Sunset Date
                           |                             |
                           v                             v
v1 ═══════════════════════════════════════════════════════════►
                           |←──────── 6 months ────────→|
                           |                             |
v2 ════════════════════════╪═════════════════════════════════════►
                           |
                    New Version Released
```

## Breaking vs Non-Breaking Changes

### Non-Breaking Changes (Minor/Patch)
- Adding new endpoints
- Adding optional request parameters
- Adding new fields to response objects
- Deprecating endpoints (with notice)
- Bug fixes
- Performance improvements

### Breaking Changes (Major)
- Removing endpoints
- Removing required fields
- Changing field data types
- Changing URL structure
- Changing authentication methods
- Modifying existing endpoint behavior

## Client Recommendations

### For API Consumers

1. **Use Explicit Versioning**: Always include the version in your API calls
2. **Monitor Headers**: Check `X-API-Deprecated` header in responses
3. **Subscribe to Updates**: Watch the changelog for deprecation notices
4. **Plan Migrations**: Begin migration when deprecation is announced

### Example Request

```bash
curl -H "Authorization: Bearer <token>" \
     -H "Accept: application/json" \
     https://api.mintstack.local/api/v1/portfolios
```

### Example Response Headers

```http
HTTP/1.1 200 OK
Content-Type: application/json
X-API-Version: 1.0.0
X-API-Min-Version: 1.0.0
```

## Changelog

### v1.0.0 (Initial Release)

- Initial API release
- Market data endpoints (currencies, stocks, bonds, funds, VIOP)
- Portfolio management
- News aggregation
- User preferences
- Real-time WebSocket updates

## Future Versions

### Planned for v2.0.0

- GraphQL support
- Enhanced real-time streaming
- Advanced analytics endpoints
- Multi-currency portfolio support

## Support

For questions about API versioning or migration assistance:

- **Documentation**: `/swagger-ui.html`
- **Email**: support@mintstack.local
- **Issues**: GitHub Issues
