# Admin API feature ownership

The former global `com.xinghuo.controller.AdminApiController` has been retired.

Existing `/api/admin/**` contracts remain unchanged, but their HTTP entrypoints are now owned by the matching feature `web` packages: dashboard, site config, game status, catalog/services, taxonomy, tool station, quote, order, slot, feedback, and audit.

Audit reads additionally cross an application-owned query port before reaching the JPA repository adapter.

This is a package-boundary refactor only. It does not introduce route, database, authentication, DTO, or payload changes.
