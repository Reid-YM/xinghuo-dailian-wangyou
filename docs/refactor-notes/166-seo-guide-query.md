# #166 SEO guide query repair

- `SeoController` now depends on `GuideQueryApplicationService` instead of the retired `GuideContentService`.
- Sitemap guide enumeration keeps the same public route and slug behavior.
- `SeoGuideBoundaryContractTest` prevents reintroducing the retired guide service dependency.
- No route, database, response field, or SEO URL changes are intended.
