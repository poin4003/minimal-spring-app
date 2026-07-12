# AI Skill Memory

This directory is used to store working context for AI in this project.

Goals:
- Preserve stable project rules so the AI can follow them while coding.
- Keep bug notes separate from project rules to avoid mixing concerns.
- Add more context gradually during development instead of writing everything at once.

Current conventions:
- Do not use `sample.sql` as the main reference for schema or structure. That file is only temporary.
- `sample.sql` is a legacy reference from the old project, not the target state of this one.
- Stable rules should be written in `project-rules.md`.
- Bugs, mismatches, and technical debt should be written in `bug-notes.md`.

File list:
- `project-rules.md`: core rules and the current source structure.
- `bug-notes.md`: a separate bug note file that can be expanded gradually during development.

Useful local H2 URL:
`jdbc:h2:file:C:/workspace/Java/minimal-spring-app/data/minimal_db;AUTO_SERVER=TRUE`
