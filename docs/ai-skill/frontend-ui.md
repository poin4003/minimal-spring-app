# Frontend UI Skill

## Scope
- Apply these rules whenever creating or changing Thymeleaf pages, fragments, UI components, CSS, or browser JavaScript.
- Keep the frontend server-rendered and aligned with the project's minimal-stack direction.

## Reuse Before Build
- Before adding HTML, CSS, or JavaScript, inspect the existing templates, fragments, shared UI components, static helpers, and vendor libraries to determine whether the required behavior is already supported.
- Use this implementation order:
  1. Reuse an existing project fragment, component, view model, or JavaScript helper.
  2. Use Bootstrap components, layout, utilities, states, and JavaScript behavior.
  3. Use an already bundled local vendor library.
  4. Extend an existing shared project component or helper.
  5. Add new page-specific HTML, CSS, or JavaScript only when the earlier options cannot express the requirement cleanly.
- Do not recreate behavior that Bootstrap or an installed library already provides.
- Do not add a second library with overlapping functionality unless the existing option is technically insufficient and the user approves the additional dependency.

## Bootstrap Direction
- Use the locally bundled Bootstrap CSS and JavaScript instead of a CDN.
- Prefer Bootstrap grid, spacing, typography, cards, forms, buttons, badges, alerts, modals, navigation, responsive utilities, color modes, ratios, and loading states over custom equivalents.
- Prefer Bootstrap utility classes in templates before adding custom CSS selectors.
- When custom styling is necessary, build on Bootstrap CSS variables so light and dark themes remain consistent.
- Preserve the project's established Bootstrap visual language; avoid introducing a separate design system.

## Bundled Libraries
- Bootstrap CSS: `/vendor/bootstrap/css/bootstrap.min.css`.
- Bootstrap JavaScript bundle: `/vendor/bootstrap/js/bootstrap.bundle.min.js`.
- Bootstrap Icons SVG assets: `/vendor/bootstrap-icons/icons`.
- HLS.js: `/vendor/hls.js/hls.min.js`.
- Use HLS.js for HLS playback where native browser playback is unavailable; retain native HLS playback when the browser supports it.
- Prefer bundled and minified production assets at runtime. Do not replace them with CDN references.

## HTML And Thymeleaf
- Reuse existing fragments under `templates/fragments`, feature fragments, and shared component models before creating page-specific markup.
- Keep templates focused on rendering structured Java view models instead of assembling business state or loose key-value data.
- Use semantic HTML and preserve keyboard navigation, labels, alt text, and relevant ARIA attributes when composing Bootstrap components.
- Use modals only for the simple actions defined by the project's UI/UX rules; use dedicated pages for complex workflows.

## CSS
- Add custom CSS only for behavior or visual requirements that Bootstrap utilities and variables cannot provide cleanly.
- Prefer extending shared styles in the existing application stylesheets over adding inline styles or a new stylesheet per small feature.
- Avoid duplicating Bootstrap declarations such as spacing, display, flex, grid, borders, colors, sizing, positioning, and responsive visibility.
- Any custom color must work in both Bootstrap light and dark color modes.

## JavaScript
- Add browser JavaScript only when server-rendered HTML and Bootstrap behavior are insufficient.
- Reuse existing scripts such as `app-ui.js`, media helpers, and shared API helpers before creating another script.
- Keep scripts feature-focused and loaded only by pages that use them, unless the behavior is truly application-wide.
- Prefer Bootstrap's JavaScript API and data attributes for modals, collapse, dropdowns, tabs, offcanvas panels, and related interactions.
- When JavaScript calls an API, follow the project's Web API Integration Rule, including authentication, CSRF, structured errors, and loading behavior.
- Do not place substantial behavior in inline event attributes; use reusable event listeners in local JavaScript files.

## Dependency Check
- Before proposing a frontend dependency, inspect `src/main/resources/static/vendor` and the current templates to confirm that the capability is not already available.
- If a new library is still necessary, explain the missing capability, expected scope, local bundling approach, and maintenance cost before changing the source.

