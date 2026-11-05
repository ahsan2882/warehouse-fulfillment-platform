# Development Guidelines

## General

- Follow SOLID principles.
- Use object-oriented programming.
- Follow clean architecture principles.
- Prefer maintainable, production-quality code.
- Avoid unnecessary abstractions.
- Never implement features outside the current milestone.

## Java

- Use Java 21.
- Use Spring Boot.
- Use Maven.
- Follow Java naming conventions.
- Use constructor injection.
- Avoid field injection.
- Use DTOs for API requests and responses.
- Implement centralized exception handling.
- Validate incoming requests.
- Avoid business logic in controllers.

## Architecture

- Follow microservices architecture.
- Each service owns its data.
- Services communicate through REST initially.
- Introduce RabbitMQ in a later milestone.
- Avoid circular dependencies.

## Testing

- Write unit tests for business logic.
- Use JUnit 5 and Mockito.
- Write integration tests where appropriate.
- Never remove tests just to make a build pass.
- All tests must pass before considering a task complete.

## Git

- Use meaningful commit messages.
- Never commit secrets.
- Never force push.
- Never delete branches without approval.
- Do not push changes without explicit permission.
- When creating a new feature branch, branch it off from the `develop` branch.
- Branch naming should follow the pattern: `feature/<feature-name>` eg: `feature/service-catalog`, strictly just the feature name, no reference to phase or milestone.
- Commit messages should be short yet meaningful, about what changes are in the commit.
- After committing the changes to the feature branch, also provide a detailed description about the changes.

## Documentation

- Maintain README.md.
- Document API endpoints.
- Document architectural decisions.
- Update documentation when functionality changes.

## Session Management

### Session Initialization

At the beginning of every new development session:

1. Read `project-state/CURRENT_STATE.md` to understand the current development status.
2. Read `project-state/NEXT_STEPS.md` to identify the next unfinished task.
3. Read `project-state/KNOWN_ISSUES.md` to understand outstanding problems.
4. Inspect `git status` and recent commit history to identify changes made since the last checkpoint.
5. Verify that the current implementation matches the recorded development state.
6. Read assitional project documentation only when relevant to the current task.
7. Consult Graphify for codebase exploration according to the Graphify rules below.

Do not unnecessarily reread all project documentation at the beginning of every session.

If project-state files do not exist, initialize them after inspecting the existing project.

If recorded progress conflicts with the actual implementation, trust the verified code and Git history, investigate the discrepancy, and correct the checkpoint.

### Session Checkpoint

Maintain the following files:

- `project-state/CURRENT_STATE.md`: Current milestone, module, implementation status, and important context.
- `project-state/COMPLETED_TASKS.md`: Completed development tasks.
- `project-state/NEXT_STEPS.md`: Ordered list of remaining tasks.
- `project-state/TECHNICAL_DECISIONS.md`: Important technical decisions and their rationale.
- `project-state/KNOWN_ISSUES.md`: Outstanding bugs, blockers, and technical limitations.

### Session Shutdown / Interruption Preparation

Before ending a development session, or whenever the user requests a pause:

1. Inspect all modified and untracked files.
2. Update CURRENT_STATE.md with the latest verified implementation status.
3. Update COMPLETED_TASKS.md with genuinely completed tasks.
4. Update NEXT_STEPS.md with the exact next actions.
5. Record significant technical decisions in TECHNICAL_DECISIONS.md.
6. Record unresolved issues in KNOWN_ISSUES.md.
7. Record relevant test results and build status.
8. Identify uncommitted changes and unfinished implementations.

Never mark partially implemented functionality as complete.
Never discard uncommitted work or create Git commits without following the Git permissions above.

### Context Preservation

- Persist important development decisions in project files rather than relying on conversation history.
- Keep checkpoint files concise and actionable.
- Do not duplicate entire architecture or specification documents in checkpoint files.
- Record file paths, class names, module names, and relevant implementation details.
- The next session must be able to resume without access to previous conversation history.

## Workflow

- Inspect existing code before modifying it.
- Explain the implementation plan before major changes.
- Implement one milestone at a time.
- Run relevant tests after changes.
- Report files changed, commands executed, and test results.
- Do not silently ignore build failures.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:

- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.

### Graph Initialization

- If graphify-out/graph.json does not exist, initialize Graphify
  using the installed CLI and its documented commands.
- Do not create graphify-out files or marker files manually
  to simulate successful initialization.
- Verify that graph.json and other expected outputs exist
  before treating initialization as complete.
- Do not use --code-only when documentation indexing is required.

## Persistent Development Workflows

### On Startup

At the beginning of every new session:

1. Load and follow AGENTS.md.
2. Read project-state/CURRENT_STATE.md.
3. Read project-state/NEXT_STEPS.md.
4. Read project-state/KNOWN_ISSUES.md.
5. Inspect Git changes using git status and recent commit history.
6. Verify the actual implementation against the development checkpoint.
7. Identify the next unfinished task.
8. Continue development without unnecessarily rereading unrelated documentation.

If checkpoint files are missing, initialize them after inspecting the project.

Never assume previous conversation history is available.

### Before Shutdown

Before ending a session or pausing development:

1. Inspect all changes made during the session.
2. Update project-state/CURRENT_STATE.md.
3. Record completed tasks in project-state/COMPLETED_TASKS.md.
4. Record unfinished tasks and exact next actions in project-state/NEXT_STEPS.md.
5. Record important technical decisions in project-state/TECHNICAL_DECISIONS.md.
6. Update project-state/KNOWN_ISSUES.md.
7. Record relevant build and test results.
8. Ensure the next session can resume without relying on conversation history.

Never mark incomplete work as completed.

Never discard uncommitted changes.

Do not create Git commits or push changes without following the Git permissions above.
